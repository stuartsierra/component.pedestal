(ns hello
  "Basic 'hello world' example using component.pedestal."
  (:require
   [com.stuartsierra.component.pedestal :as component.pedestal]
   [com.stuartsierra.component :as component]
   [io.pedestal.http :as http]))

(def get-hello
  (component.pedestal/component-handler
   ::get-hello
   ::hello
   (fn [hello request]
     {:status 200
      :headers {"Content-type" "text/plain"}
      :body (pr-str hello)})))

(def routes
  #{["/hello" :get get-hello]})

(defn pedestal-config [this]
  (-> {::http/host "0.0.0.0"
       ::http/port 8080
       ::http/type :jetty
       ::http/join? false
       ::http/resource-path "/public"
       ::http/routes routes}
      http/default-interceptors
      http/dev-interceptors))

(defrecord HelloComponent []
  component/Lifecycle
  (start [this]
    (assoc this :started? true))
  (stop [this]
    (assoc this :started? false)))

(defn pedestal []
  (component/using
   (component.pedestal/pedestal-server pedestal-config)
   [::hello]))

(defn system []
  (component/system-map
   ::hello (->HelloComponent)
   ::pedestal (pedestal)))

(comment
  ;; Run in the REPL:
  (require 'hello)
  (in-ns 'hello)
  (def sys (component/start (system)))

  ;; Visit http://localhost:8080/hello

  (def svc-fn (component.pedestal/get-service-fn (::pedestal sys)))
  (require '[io.pedestal.test :refer [response-for]])
  (response-for svc-fn :get "/hello")
  ;;=> {:status 200, :body "#hello.HelloComponent{:started? true}", ...}

  (component/stop s)

  )
