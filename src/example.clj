(ns example
  (:require
   [com.stuartsierra.component.pedestal :as cp]
   [com.stuartsierra.component :as component]
   [io.pedestal.http :as http]
   [io.pedestal.http.route.definition :refer [defroutes]]))

(defn hello [request]
  {:status 200
   :body (pr-str "hi" (cp/use-component request))})

(defroutes routes
  [[["/hello" {:get hello}
     ^:interceptors [(cp/using-component :hello-component)]]]])

(def pedestal-config
  {::http/host "0.0.0.0"
   ::http/port 8080
   ::http/type :jetty
   ::http/join? false
   ::http/resource-path "/public"
   ::http/routes (fn [] routes)})

(defrecord HelloComponent [])

(defn system []
  (component/system-map
   :hello-component (->HelloComponent)
   :pedestal (component/using (cp/pedestal pedestal-config)
                              [])))
