(ns pullq.main
  (:require [clojure.string    :as str]
            [clojure.java.io   :as io]
            [clojure.pprint    :refer [pprint]]
            [clojure.tools.cli :refer [cli]]
            [clj-time.format   :refer [parse]]
            [clj-time.coerce   :refer [to-epoch]]
            [tentacles.pulls   :refer [pulls]]
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

(defn pull-reviews
  [auth user repo number]
  (some->> (reviews user repo number auth)
           (map sanitize-review)
           (group-by :login)
           (reduce-kv #(conj %1 (last (sort-by :age %3))) [])))

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
                 (< oks min-oks) "orange"
                 :else           "green")
     :counter  (cond
                 (pos? changes)  changes
                 (< oks min-oks) (- min-oks oks)
                 :else           oks)
     :type     (cond
                 (= 1 changes)         "change request"
                 (pos? changes)        "change requests"
                 (= 1 (- min-oks oks)) "ok missing"
                 (< oks min-oks)       "oks missing"
                 :else                 "ready")}))

(defn pull-stats
  [auth min-oks {:keys [labels number title] :as pull}]
  (let [updated (:updated_at pull)
        created (:created_at pull)
        login   (get-in pull [:user :login])
        repo    (get-in pull [:head :repo :name])
        user    (get-in pull [:head :repo :owner :login])
        reviews (pull-reviews auth user repo number)]
    {:repo    {:name (get-in pull [:head :repo :name])
               :url  (get-in pull [:head :repo :html_url])}
     :url     (:html_url pull)
     :labels  (mapv :name labels)
     :title   title
     :updated (to-epoch (parse updated))
     :created (to-epoch (parse created))
     :login   login
     :avatar  (get-in pull [:user :avatar_url])
     :reviews (vec (sort-by :age reviews))
     :status  (review-stats reviews min-oks)}))

(defn pull-fn
  [auth]
  (fn [[user repo min-oks]]
    (map (partial pull-stats auth min-oks) (pulls user repo auth))))

(defn pull-queue
  [auth config]
  (vec
   (mapcat (pull-fn auth) config)))

(def valid-conf
  #"^([A-Za-z0-9-]+)[ \t]+([A-Za-z0-9-]+)[ \t]+([0-9]+).*")

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
         ["-f" "--path" "Configuration file path" :default "pullq.conf"])
    (catch Exception _
      (binding [*out* *err*]
        (println "could not parse arguments")
        (System/exit 1)))))

(defn -main
  [& args]
  (let [[opts _ banner]           (get-cli args)
        {:keys [path help token]} opts
        config                    (read-config path)
        env-token                 (System/getenv "GITHUB_TOKEN")
        auth                      {:oauth-token (or token env-token)}]
    (when help
      (println "Usage: pullq [-t token] [f config]\n")
      (print banner)
      (flush)
      (System/exit 0))
    (try
      (spit "resources/public/data.edn"
            (with-out-str (pprint (pull-queue {:oauth-token token} config))))
      (catch Exception e
        (binding [*out* *err*]
          (println "could not generate stats:" (.getMessage e))
          (System/exit 1))))))
