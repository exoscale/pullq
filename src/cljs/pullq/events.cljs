(ns pullq.events
  (:require
   [re-frame.core :as re-frame]
   [ajax.edn :as edn]
   [pullq.db :as db]))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _] db/default-db))


(re-frame/reg-event-db
 ::set-order
 (fn [db [_ value]] (assoc db :order value)))

(re-frame/reg-event-db
 ::set-sort-dir
 (fn [db [_ value]] (assoc db :sort-dir value)))

(re-frame/reg-event-db
 ::set-filter
 (fn [db [_ value]] (assoc db :filter value)))

(re-frame/reg-event-db
 ::set-search
 (fn [db [_ value]] (assoc db :search value)))

(re-frame/reg-event-db
 ::toggle
 (fn
  [db [_ what value]]
  (let [current (get-in db [:only what])]
    (assoc-in db [:only what] (if (= current value) nil value)))))

(re-frame/reg-event-db
 ::refresh-success
 (fn
  [db [_ value]]
  (assoc db :pulls value)))

(re-frame/reg-event-db
 ::refresh-failure
 (fn
  [db [_ {:keys [debug-message]}]]
  (assoc db :error debug-message)))

(re-frame/reg-event-fx
 ::refresh-db
 (fn
  [{:keys [db]} _]
  {:db         (assoc db :pulling? true)
   :http-xhrio {:method          :get
                :uri             "data.edn"
                :timeout         8000
                :response-format (edn/edn-response-format)
                :on-success      [::refresh-success]
                :on-failure      [::refresh-failure]}}))


(re-frame/reg-event-db
 ::hide-label
 (fn
  [db [_ label]]
  (update db :hidden-labels conj label)))

(re-frame/reg-event-db
 ::show-label
 (fn
  [db [_ label]]
  (enable-console-print!)
  (println "showing label: " (pr-str label))
  (update db :hidden-labels #(remove (partial = label) %))))
