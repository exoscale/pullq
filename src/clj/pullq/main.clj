(ns pullq.main
  (:require [bidi.ring :refer [->Redirect ->ResourcesMaybe make-handler]]
            [clojure.java.io   :as io]
            [clojure.tools.cli :refer [cli]]
            [clojure.pprint    :refer [pprint]]
            [ring.util.response :as res]
            [ring.middleware.params :refer [wrap-params]]
            [aleph.http :as http]
            [pullq.queue :refer [pull-queue]]))

(def valid-conf
  #"^([A-Za-z0-9-]+)[ \t]+([A-Za-z0-9-]+)[ \t]+([0-9]+).*")

(def five-minutes
  (* 5 60 1000))

;; This is the main application state. A simple atom that the periodic-refresher
;; will reset!.
(def data
  (atom []))

(defn log
  "A cheap and dirty logging function that will prepend the date to text."
  [text]
  (let [now (.toString (java.util.Date.))]
    (println now "-" text)))

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
         ["-f" "--path" "Configuration file path" :default "pullq.conf"]
         ["-i" "--interval" "Interval at which data will be re-queried from github" :default (str five-minutes)]
         ["-p" "--port" "The port on which the HTTP server will be listening" :default (str 8000)])
    (catch Exception _
      (binding [*out* *err*]
        (println "could not parse arguments")
        (System/exit 1)))))

(defn data-handler
  "The handler for the data.edn file. Simply make the data and EDN string and
  add the appropriate header."
  [auth config req]
  (-> (res/response (pr-str @data))
      (res/header "content-type" "application/octet-stream")))

(defn scheduled-pull
  "A single pull of the data from github, as done by the periodic scheduler."
  [auth config]
  (log "Refreshing data from github")
  (try
    (reset! data (pull-queue auth config))
    (catch Exception e (log e)))
  (log "refresh done"))

(defn handler
  "The routes handler for the web server.

  All the routes are just forwarded to ressources, except the data.edn path
  for which we serve the edn data from our application state directly."
  [auth config]
  (make-handler ["/" [["data.edn" (partial data-handler auth config)]
                      ["" (->ResourcesMaybe {:prefix "public/"})]
                      ["" (->Redirect 301 "index.html")]]]))

(defn periodic-refresh
  "Periodically refresh the data from github."
  [auth config interval]
  (future (while true (do (Thread/sleep interval) (scheduled-pull auth config)))))

(defn -main
  [& args]
  (let [[opts _ banner] (get-cli args)
        config          (read-config (:path opts))
        env-token       (System/getenv "GITHUB_TOKEN")
        auth            {:oauth-token (or (:token opts) env-token)
                         :per-page    100}
        interval        (Integer/parseInt (:interval opts))
        port            (Integer/parseInt (:port opts))]

    (log "Starting initial data dump - this may take a while")
    (reset! data (pull-queue auth config))
    (log "Initial data dump acquired, starting server")
    (http/start-server (handler auth config) {:port port})
    (log "starting periodic refresh")
    (periodic-refresh auth config interval)
    (log (format "!!! server ready and listening on port %d !!!" port))))
