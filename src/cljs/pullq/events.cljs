(ns pullq.events
  (:require
   [re-frame.core :as re-frame]
   [ajax.edn :as edn]
   [pullq.db :as db]
   [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]))

(re-frame/reg-event-db
 ::initialize-db
 (fn-traced [_ _] db/default-db))


(re-frame/reg-event-db
 ::set-order
 (fn-traced [db [_ value]] (assoc db :order value)))

(re-frame/reg-event-db
 ::set-filter
 (fn-traced [db [_ value]] (assoc db :filter value)))

(re-frame/reg-event-db
 ::set-search
 (fn-traced [db [_ value]] (assoc db :search value)))

(re-frame/reg-event-db
 ::toggle
 (fn-traced
  [db [_ what value]]
  (let [current (get-in db [:only what])]
    (assoc-in db [:only what] (if (= current value) nil value)))))

(re-frame/reg-event-db
 ::refresh-success
 (fn-traced
  [db [_ value]]
  (assoc db :pulls value)))

(re-frame/reg-event-db
 ::refresh-failure
 (fn-traced
  [db [_ {:keys [debug-message]}]]
  (assoc db :error debug-message)))

(re-frame/reg-event-fx
 ::refresh-db
 (fn-traced
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
 (fn-traced
  [db [_ label]]
  (update db :hidden-labels conj label)))

(re-frame/reg-event-db
 ::show-label
 (fn-traced
  [db [_ label]]
  (enable-console-print!)
  (println "showing label: " (pr-str label))
  (update db :hidden-labels #(remove (partial = label) %))))
