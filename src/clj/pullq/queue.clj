(ns pullq.queue
  (:require [clojure.string    :as str]
            [clojure.java.io   :as io]
            [clojure.pprint    :refer [pprint]]
            [clojure.tools.cli :refer [cli]]
            [clj-time.format   :refer [parse]]
            [clj-time.coerce   :refer [to-epoch]]
            [tentacles.pulls   :refer [specific-pull pulls]]
            [tentacles.core    :refer [api-call]]))

(defn reviews
  [user repo pull & [options]]
  (let [resp (api-call :get "repos/%s/%s/pulls/%s/reviews"
                       [user repo pull] options)]
    (when (< (:status resp 200) 300)
      resp)))

(def states
  {"COMMENTED"         :comment
   "APPROVED"          :approved
   "CHANGES_REQUESTED" :needs-changes})

(defn sanitize-review
  [{:keys [user state html_url submitted_at] :or {state "unknown"} :as input}]
  {:login  (:login user)
   :url    html_url
   :avatar (:avatar_url user)
   :state  (or (get states state) (keyword (str/lower-case state)))
   :age    (to-epoch (parse submitted_at))})

(defn aggregate-reviews
  "Create an aggregate review for a user given all of a user's reviews.

  We take the latest review, but override its :state with the latest non-comment
  review's :state if there is one. That prevents having somebody approve a review
  then cancel the approval by commenting further."
  [reviews]
  (let [sorted             (sort-by :age reviews)
        latest             (last sorted)
        non-comment        (filter #(#{:approved :needs-changes} (:state %1)) sorted)
        latest-non-comment (last non-comment)]
    (assoc latest :state (:state latest-non-comment :comment))))

(defn pull-reviews
  [raw-reviews]
  (some->> raw-reviews
           (remove #(= (:state %) "PENDING"))
           (map sanitize-review)
           (group-by :login)
           (reduce-kv #(conj %1 (aggregate-reviews %3)) [])))

(defn review-stats
  [reviews min-oks]
  (let [states   (mapv :state reviews)
        oks      (count (filter #{:approved} states))
        changes  (count (filter #{:needs-changes} states))
        comments (count (filter #{:comment} states))
        color    (cond (pos? changes) "red")]
    {:min-oks  min-oks
     :oks      oks
     :comments comments
     :changes  changes
     :open?    (or (pos? changes) (< oks min-oks))
     :color    (cond
                 (pos? changes)  "red"
                 (< oks min-oks) "yellow"
                 :else           "blue")
     :counter  (cond
                 (pos? changes)  changes
                 (< oks min-oks) (- min-oks oks)
                 :else           oks)
     :type     (cond
                 (= 1 changes)         "blocker"
                 (pos? changes)        "blockers"
                 (= 1 (- min-oks oks)) "ok missing"
                 (< oks min-oks)       "oks missing"
                 :else                 "ready")}))

(defn pull-stats
  [auth min-oks {:keys [labels number title mergeable_state] :as pull}]
  (let [updated     (:updated_at pull)
        created     (:created_at pull)
        login       (get-in pull [:user :login])
        repo        (get-in pull [:head :repo :name])
        user        (get-in pull [:head :repo :owner :login])
        raw-reviews (reviews user repo number auth)
        reviews     (pull-reviews raw-reviews)]
    {:repo            {:name (get-in pull [:head :repo :name])
                       :url  (get-in pull [:head :repo :html_url])}
     :url             (:html_url pull)
     :labels          (mapv :name labels)
     :title           title
     :mergeable-state mergeable_state
     :updated         (to-epoch (parse updated))
     :created         (to-epoch (parse created))
     :login           login
     :avatar          (get-in pull [:user :avatar_url])
     :reviews         (vec (sort-by :age reviews))
     :status          (review-stats reviews min-oks)}))

(defn pulls-with-details
  [user repo auth]
  (map (fn [{:keys [number] :as pull}]
         (let [details (specific-pull user repo number auth)]
           (merge details pull)))
       (pulls user repo auth)))

(defn pull-fn
  [auth]
  (fn [[user repo min-oks]]
    (->> (pulls-with-details user repo auth)
         (remove #(= "draft" (:mergeable_state %)))
         (map (partial pull-stats auth min-oks)))))

(defn pull-queue
  [auth config]
  (vec
   (mapcat (pull-fn auth) config)))
