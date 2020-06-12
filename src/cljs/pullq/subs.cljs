(ns pullq.subs
  (:require
   [re-frame.core :as re-frame]
   [clojure.string :as str]))

(defn is-open?
  [pull]
  (get-in pull [:status :open?]))

(defn bug?
  [{:keys [labels]}]
  (boolean (seq (filter #(str/includes? % "bug") labels))))

(defn match-fn
  [pattern]
  (fn [{:keys [title repo]}]
    (when (and (some? title) (some? repo))
      (let [p (re-pattern pattern)]
        (or (re-find p title) (re-find p (:name repo)))))))

(defn only-fn
  [{:keys [author repo]}]
  (fn [pull]
    (and (or (nil? author) (= author (:login pull)))
         (or (nil? repo) (= repo (get-in pull [:repo :name]))))))

(defn sort-fn
  [order]
  (if (= :age order) :created :updated))

(defn hidden-label-fn
  [hidden-labels]
  (fn [{:keys [labels]}]
    (first
     (for [hidden hidden-labels
           :let [found? (some #(str/includes? % hidden) (map str/lower-case labels))]
           :when found?]
       true))))

(defn filter-pulls
  [db]
  (->> (:pulls db)
       (filter (match-fn (:search db)))
       (filter (only-fn (:only db)))
       (remove (hidden-label-fn (:hidden-labels db)))
       (sort-by (sort-fn (:order db)) (:sort-dir db))))

(re-frame/reg-sub
 ::pulls
 (fn [db]
   (let [filter-name (:filter db)]
     (filter (cond
               (= :open filter-name) is-open?
               (= :bug  filter-name) bug?
               :else                 (complement is-open?))
             (filter-pulls db)))))

(defn get-authors
  [pulls]
  (distinct
   (for [{:keys [avatar login]} pulls]
     [login avatar])))

(defn get-repos
  [pulls]
  (distinct
   (for [{:keys [repo]} pulls]
     (:name repo))))

(re-frame/reg-sub
 ::menu-stats
 (fn [db]
   (let [pulls (filter-pulls db)
         open  (count (filter is-open? pulls))]
     {:open          open
      :bug           (count (filter bug? pulls))
      :ready         (- (count pulls) open)
      :repos         (get-repos pulls)
      :authors       (get-authors pulls)
      :filter        (:filter db)
      :hidden-labels (:hidden-labels db)
      :order         (:order db)
      :sort-dir      (:sort-dir db)
      :search        (:search db)
      :only          (:only db)})))

(re-frame/reg-sub ::order :order)
(re-frame/reg-sub ::hidden-labels :hidden-labels)
