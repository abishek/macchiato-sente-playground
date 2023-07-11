(ns rohabini.macchiato-sente.frontend.main
  (:require [taoensso.timbre :as logger]
            [reagent.dom     :as rdom  ]
            [taoensso.sente  :as sente ]))

(enable-console-print!)

(defonce ws-router (atom nil))

(let [chsk-type :auto
      packer    :edn
      {:keys [chsk ch-recv send-fn state]} (sente/make-channel-socket-client! "/chsk" {:type chsk-type :packer packer})]
  (def chsk       chsk   )
  (def ch-chsk    ch-recv)
  (def chsk-send! send-fn)
  (def chsk-state state  ))

(defn event-msg-handler [msg]
  (logger/info msg))

(defn stop-router! []
  (when-let [stop-fn @ws-router]
    (stop-fn)))

(defn start-router! []
  (stop-router!)
  (reset! ws-router (sente/start-client-chsk-router! ch-chsk event-msg-handler)))

(defn hello-world
  []
  [:h1#heading "Hello, world."])

(defn mount-app
  []
  (rdom/render [:f> hello-world] (.getElementById js/document "app")))

(defn ^:export ^:dev/after-load init []
  (start-router!)
  (logger/info "WS started.")
  (mount-app)
  (logger/info "App loaded."))