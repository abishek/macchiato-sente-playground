(ns rohabini.macchiato-sente.backend.main
  (:require [macchiato.server                         :as http]
            [taoensso.timbre                          :as logger]
            [taoensso.sente                           :as sente]
            [macchiato.util.response                  :as mres]
            [macchiato.middleware.resource            :as mmr]
            [macchiato.middleware.params              :as mmp]
            [macchiato.middleware.keyword-params      :as mmkp]
            [macchiato.middleware.defaults            :as mdef]
            [macchiato.middleware.anti-forgery        :as maf]
            [reitit.ring                              :as ring]
            [reitit.ring.coercion                     :as rrc] 
            [taoensso.sente.server-adapters.macchiato :as sente-macchiato]
            [clojure.core.async                       :as async :refer [<! go-loop]]
            [hiccups.runtime])
  (:require-macros [hiccups.core :refer [html]]))

;; websockets
(defonce ws-server (atom nil))
(let [packer :edn
      {:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn connected-uids]}
      (sente-macchiato/make-macchiato-channel-socket-server! {:packer packer})]
  (def ajax-post                ajax-post-fn)
  (def ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                  ch-recv)
  (def chsk-send!               send-fn)
  (def connected-uids           connected-uids))

(defn fetch-random-user [cbfn]
  (-> (js/fetch "https://random-data-api.com/api/v2/users?size=2")
      (.then #(.json %))
      ;(.then #(js->clj %))
      (.then #(cbfn {:data %}))))

(def string-list ["hello, world"
                  "how can I help you today?"
                  "sorry, I don't think I understand that."
                  "Would you like me to call instead?"])

(defmulti -event-msg-handler :id)

(defmethod -event-msg-handler :default
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid (:uid session)]
    (logger/debugf "Unhandled event: %s" event)
    (when ?reply-fn
      (?reply-fn {:unmatched-event-as-echoed-from-server event}))))

(defmethod -event-msg-handler :demo/btn-get-msg
  [{:as ev-msg :keys [event id uid ?data ring-req ?reply-fn send-fn]}]
  (fetch-random-user ?reply-fn))

(defn event-msg-handler [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg))

(defn stop-ws! []
  (when-let [stop-ws-fn @ws-server]
    (stop-ws-fn)))

(defn start-ws! []
  (stop-ws!)
  (reset! ws-server
          (sente/start-server-chsk-router! ch-chsk event-msg-handler)))


;; router and simple html delivery
(defn home-page-html []
  (let [csrf-token maf/*anti-forgery-token*]
    [:html
     [:head
      [:title "Macchiato Sente Example"]
      [:meta {:name "viewport"
              :content "minimum-scale=1,initial-scale=1,width=device-width"}]
      [:meta {:name "description"
              :content "Macchiato Application Example to use Sente websockets."}]
      [:link {:href "/css/styles.css"
              :rel "stylesheet"
              :type "text/css"}]]
     [:body
      [:input#csrf {:type "hidden" :value csrf-token}]
      [:div#app
       [:h4 "App Loads Here."]]
      [:script {:src "/js/client.js"
                :type "text/javascript"}]
      [:script "rohabini.macchiato_sente.frontend.main.init()"]]]))

(defn home-page [req res raise]
  (-> (home-page-html)
      (html)
      (mres/ok)
      (mres/content-type "text/html")
      (res)))

(def html-routes ["/" {:get home-page}])
(def ws-routes   ["/chsk" {:get  ajax-get-or-ws-handshake
                           :post ajax-post
                           :ws   ajax-get-or-ws-handshake}])

(def router (ring/router
             [html-routes
              ws-routes]
             {:data {:middleware [mmp/wrap-params
                                  mmkp/wrap-keyword-params
                                  maf/wrap-anti-forgery
                                  rrc/coerce-request-middleware
                                  rrc/coerce-response-middleware]}}))

(def route-handler (ring/ring-handler router))
(def app (-> route-handler
             (mmr/wrap-resource "resources/public")
             (mdef/wrap-defaults mdef/site-defaults)))

;; web and websocket servers

(defonce http-server (atom {}))

(defn reload! []
  (logger/info "Page reload."))

(defn main! []
  (logger/info "Starting HTTP and Socket servers.")
  (let [options {:handler     app
                 :host        "127.0.0.1"
                 :port        "3000"
                 :websockets? true
                 :on-sockets  #(logger/info "Started.")}
        server  (http/start options)]
    (start-ws!)
    (http/start-ws server app)
    (reset! http-server {:stop-fn #(.end server)})))