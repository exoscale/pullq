(ns pullq.main
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [pullq.events :as events]
   [pullq.views :as views]
   [pullq.config :as config]
   [day8.re-frame.http-fx]
   ))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (re-frame/dispatch-sync [::events/refresh-db])
  (dev-setup)
  (mount-root))
