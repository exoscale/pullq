(defproject exoscale/pullq "0.3.0"
  :dependencies [[org.clojure/clojure "1.10.2-alpha1"]
                 [org.clojure/clojurescript "1.10.773"]
                 [org.clojure/tools.cli "1.0.194"]
                 [cljsjs/moment "2.24.0-0"]
                 [day8.re-frame/http-fx "v0.2.0"]
                 [irresponsible/tentacles "0.6.6"]
                 [clj-time "0.15.2"]
                 [reagent "0.9.1"]
                 [re-frame "0.12.0"]
                 [soda-ash "0.83.0"]]
  :plugins [[lein-cljsbuild "1.1.8"]]
  :main pullq.main
  :min-lein-version "2.5.3"
  :source-paths ["src/clj" "src/cljs"]
  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]
  :cljsbuild
  {:builds
   [{:id           "min"
     :source-paths ["src/cljs"]
     :compiler     {:main            pullq.main
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}]})
