(ns rohabini.macchiato-sente.frontend.main
  (:require [taoensso.timbre :as logger]
            [reagent.dom     :as rdom]))

(enable-console-print!)

(defn hello-world
  []
  [:h1#heading "Hello, world."])

(defn mount-app
  []
  (rdom/render [:f> hello-world] (.getElementById js/document "app")))

(defn ^:export ^:dev/after-load init []
  (mount-app)
  (logger/info "App loaded."))