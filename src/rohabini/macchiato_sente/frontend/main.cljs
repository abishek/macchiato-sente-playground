(ns rohabini.macchiato-sente.frontend.main
  (:require [taoensso.timbre :as logger]
            [reagent.core    :as r     ]
            [reagent.dom     :as rdom  ]
            [taoensso.sente  :as sente ]))

(enable-console-print!)

(defonce ws-router (atom nil))
(defonce messages (r/atom []))

(def csrf-token 
  (when-let [el (.getElementById js/document "csrf")]
    (.-value el)))

(let [chsk-type :auto
      packer    :edn
      {:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket-client! "/chsk" csrf-token {:type chsk-type
                                                             :packer packer})]
  (def chsk       chsk   )
  (def ch-chsk    ch-recv)
  (def chsk-send! send-fn)
  (def chsk-state state  ))

(defmulti -event-msg-handler :id)

(defmethod -event-msg-handler :default
  [{:as ev-msg :keys [event]}]
  (logger/info "Unhandled event: %s" event))

(defmethod -event-msg-handler :chsk/state
  [{:as ev-msg :keys [?data]}]
  (if (= ?data {:first-open? true})
    (logger/info "Socket connection established successfully.")
    (logger/info "Socket state change: %s" ?data)))

(defmethod -event-msg-handler :chsk/recv
  [{:as ev-msg :keys [?data]}]
  (logger/info "Reveived from server: %s" ?data))

(defmethod -event-msg-handler :chsk/handshake
  [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (logger/info "Handshake: %s" ?data)))

(defn event-msg-handler [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg))

(defn stop-router! []
  (when-let [stop-fn @ws-router]
    (stop-fn)))

(defn start-router! []
  (stop-router!)
  (reset! ws-router (sente/start-client-chsk-router! ch-chsk event-msg-handler)))

(defn get-message []
  (logger/info "get a message")
  (chsk-send! [:demo/btn-get-msg {:data? nil}] 5000 (fn [reply] (swap! messages conj (:message reply)))))

(defn demo-app
  []
  [:div#content
   [:button.getmsg {:on-click get-message} "Get A Message"]
   [:ul.messages
    (map-indexed (fn [idx message] [:li.message {:key idx} message]) @messages)]])


(defn mount-app
  []
  (rdom/render [:f> demo-app] (.getElementById js/document "app")))

(defn ^:export ^:dev/after-load init [] 
  (start-router!)
  (logger/info "WS started.") 
  (mount-app)
  (logger/info "App loaded."))