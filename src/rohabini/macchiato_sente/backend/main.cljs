(ns rohabini.macchiato-sente.backend.main
  (:require [macchiato.server              :as http]
            [taoensso.timbre               :as logger]
            [macchiato.util.response       :as mres]
            [macchiato.middleware.resource :as mmr]
            [macchiato.middleware.params   :as mmp]
            [reitit.ring                   :as ring]
            [reitit.ring.coercion          :as rrc]
            [hiccups.runtime])
  (:require-macros [hiccups.core :refer [html]]))

;; websockets

;; router and simple html delivery
(def home-page-html
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
    [:div#app
     [:h4 "App Loads Here."]]
    [:script {:src "/js/client.js"
              :type "text/javascript"}]
    [:script "rohabini.macchiato_sente.frontend.main.init()"]]])

(defn home-page [req res raise]
  (-> home-page-html
      (html)
      (mres/ok)
      (mres/content-type "text/html")
      (res)))

(def html-routes ["/" {:get home-page}])
(def ws-routes [])

(def router (ring/router 
             [html-routes
              ws-routes]
             {:data {:middleware [mmp/wrap-params
                                  rrc/coerce-request-middleware
                                  rrc/coerce-response-middleware]}}))

(def route-handler (ring/ring-handler router))
(def app (-> route-handler
             (mmr/wrap-resource "resources/public")))

;; web and websocket servers

(defonce http-server (atom {}))
(defonce socket-server (atom nil))

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
    (reset! http-server {:stop-fn #(.end server)})))