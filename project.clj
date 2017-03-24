(defproject com.stuartsierra/component.pedestal "0.1.0-SNAPSHOT"
  :description "Component wrapper for the Pedestal web application server"
  :url "https://github.com/stuartsierra/component.pedestal"
  :license {:name "MIT license"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[com.stuartsierra/component "0.3.2"]
                 [io.pedestal/pedestal.service "0.5.2"]
                 [org.clojure/clojure "1.8.0" :scope "provided"]]
  :repositories [["releases" "https://clojars.org/repo/"]]
  :profiles {:dev {:dependencies [[io.pedestal/pedestal.jetty "0.5.2"]]
                   :source-paths ["examples"]}})
