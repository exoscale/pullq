(ns pullq.main
  (:gen-class)
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
  [auth min-oks {:keys [labels number title draft] :as pull}]
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
     :draft           draft
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
         (remove #(:draft %))
         (map (partial pull-stats auth min-oks)))))

(defn pull-queue
  [auth config]
  (vec
   (mapcat (pull-fn auth) config)))

(def valid-conf
  #"^([A-Za-z0-9-]+)[ \t]+([A-Za-z0-9-\.]+)[ \t]+([0-9]+).*")

(defn read-config
  [path]
  (try
    (for [raw   (line-seq (io/reader path))
          :let  [[_ user repo oks] (re-matches valid-conf raw)]
          :when (some? user)]
      [user repo (Long/parseLong oks)])
    (catch Exception e
      (binding [*out* *err*]
        (println "could not parse config" path ":" (.getMessage e))
        (System/exit 1)))))

(defn get-cli
  [args]
  (try
    (cli args
         ["-h" "--help" "Show Help" :default false :flag true]
         ["-t" "--token" "Github Token, overrides GITHUB_TOKEN environment"]
         ["-o" "--output" "Where to dump data" :default "build/data.edn"]
         ["-S" "--syncdir" "A directory in which to produce a full static site"]
         ["-f" "--path" "Configuration file path" :default "pullq.conf"])
    (catch Exception _
      (binding [*out* *err*]
        (println "could not parse arguments")
        (System/exit 1)))))

(def files
  ["data.edn"
   "index.html"
   "css/themes/default/assets/fonts/icons.eot"
   "css/themes/default/assets/fonts/icons.otf"
   "css/themes/default/assets/fonts/icons.ttf"
   "css/themes/default/assets/fonts/icons.svg"
   "css/themes/default/assets/fonts/icons.woff"
   "css/themes/default/assets/fonts/icons.woff2"
   "css/themes/default/assets/images/flags.png"
   "css/semantic.min.css"
   "img/favicon.png"
   "js/compiled/app.js"])

(defn copy-files
  [syncdir]
  (doseq [path files
          :let [src (io/file "build" path)
                dst (io/file syncdir path)]]
    (io/make-parents dst)
    (io/copy src dst)))

(defn -main
  [& args]
  (let [[opts _ banner] (get-cli args)
        config          (read-config (:path opts))
        env-token       (System/getenv "GITHUB_TOKEN")
        auth            {:oauth-token (or (:token opts) env-token)
                         :per-page 100}]
    (when (:help opts)
      (println "Usage: pullq [-t token] [-f config] [-o outfile] [-S syncdir]\n")
      (print banner)
      (flush)
      (System/exit 0))
    (try
      (println "starting dump, this might take a while")
      (spit (:output opts) (with-out-str (pprint (pull-queue auth config))))
      (println "created data file in:" (:output opts))
      (when-some [syncdir (:syncdir opts)]
        (println "copying full website output to:" syncdir)
        (copy-files syncdir))
      (catch Exception e
        (binding [*out* *err*]
          (println "could not generate stats:" (.getMessage e))
          (System/exit 1))))))

(comment
  (def auth {})
  (def config [["pyr" "dot.emacs" 2] ["pyr" "watchman" 2]])

;;  (pulls-with-details "pyr" "dot.emacs" auth)
  (pull-queue auth config))
