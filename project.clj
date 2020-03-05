(defproject pull-queue "0.1.1"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.520"]
                 [org.clojure/tools.cli "0.4.1"]
                 [cljsjs/moment "2.24.0-0"]
                 [day8.re-frame/http-fx "0.1.6"]
                 [irresponsible/tentacles "0.6.3"]
                 [clj-time "0.15.1"]
                 [reagent "0.8.1"]
                 [re-frame "0.10.6"]
                 [soda-ash "0.83.0"]
                 [aleph "0.4.6"]
                 [bidi "2.1.6"]]
  :plugins [[lein-cljsbuild "1.1.7"]]
  :main pullq.main
  :min-lein-version "2.5.3"
  :source-paths ["src/clj" "src/cljs"]
  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]
  :figwheel {:css-dirs ["resources/public/css"]}
  :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
  :profiles
  {:dev  {:dependencies [[binaryage/devtools "0.9.10"]
                         [day8.re-frame/tracing "0.5.1"]
                         [figwheel-sidecar "0.5.18"]
                         [cider/piggieback "0.4.0"]]
          :plugins      [[lein-figwheel "0.5.18"]]}
   :prod {:dependencies [[day8.re-frame/tracing-stubs "0.5.1"]]}}
  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "pullq.main/mount-root"}
     :compiler     {:main                 pullq.main
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :external-config
                    {:devtools/config {:features-to-install :all}}}}
    {:id           "min"
     :source-paths ["src/cljs"]
     :compiler     {:main            pullq.main
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}]})
