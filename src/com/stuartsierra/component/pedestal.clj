(ns com.stuartsierra.component.pedestal
  "Component wrapper for the Pedestal web application server. Injects
  component dependencies into Pedestal context."
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [io.pedestal.interceptor :refer [interceptor interceptor-name]]
            [io.pedestal.log :as pedestal.log]))

(defn- merge-context-interceptor
  "Returns an interceptor which merges a map into the Pedestal context
  map."
  [m]
  (interceptor
   {:name ::merge-context
    :enter (fn [context] (merge context m))}))

(defn- prependv
  "Returns a vector of x prepended to the elements of collection."
  [coll x]
  (vec (cons x (seq coll))))

(defn- add-dependency-interceptor
  "Adds an interceptor to the beginning of the interceptor chain
  which associates the dependencies of component into the Pedestal
  context map. Must be called before `io.pedestal.http/create-server`."
  [service-map component]
  (let [deps (reduce (fn [m k]
                       (assoc m k (get component k)))
                     {}
                     (keys (component/dependencies component)))]
    (update service-map
            ::http/interceptors
            prependv
            (merge-context-interceptor deps))))

(defrecord Pedestal []
  component/Lifecycle
  (start [this]
    (if (::service this)
      this
      (let [{:keys [::service-map-fn ::start-service-fn]} this]
        (assoc this ::service
               (-> (service-map-fn this)
                   (add-dependency-interceptor this)
                   start-service-fn)))))
  (stop [this]
    (when-let [server (::service this)]
      (try (http/stop server)
           (catch Throwable t
             (pedestal.log/warn
              :msg "Exception stopping Pedestal server"
              :exception t))))))

(defn get-service-fn
  "Returns the Pedestal interceptor service function to use with
  `io.pedestal.test/response-for`."
  [component]
  (get-in component [::service ::http/service-fn]))

(defn pedestal-server
  "Returns a new instance of the Pedestal server component.

  On Lifecycle start, this component calls `service-map-fn` with
  itself as an argument. `service-map-fn` must return a Pedestal
  service configuration map (see Pedestal documentation).

  The service configuration should include
  `:io.pedestal.http/join? false`.

  If you want the default interceptors, you must call
  `io.pedestal.http/default-interceptors` in `service-map-fn`.

  If you want the development interceptors, you must call
  `io.pedestal.http/dev-interceptors` in `service-map-fn`.

  This component calls `io.pedestal.http/create-server` and
  `io.pedestal.http/start` for you.

  This component's dependencies (as by `component/using` or
  `system-using`) will be merged into the Pedestal context map at the
  beginning of each request. The keys in the context map will be the
  same as the keys in this component.

  For example, if this component has dependencies declared like this:

   (component/using ... {:local-name :global-name})

  Then the component found in the system at :global-name will appear
  in the Pedestal context map as :local-name."
  [service-map-fn]
  (map->Pedestal {::service-map-fn service-map-fn
                  ::start-service-fn (comp http/start http/create-server)}))

(defn pedestal-servlet
  "Like `pedestal-server` but only constructs a Servlet without
  initializing a container or starting an HTTP server. Useful for
  testing: see `get-service-fn`."
  ([service-map-fn]
   (map->Pedestal {::service-map-fn service-map-fn
                   ::start-service-fn http/create-servlet})))

(defn- get-component [context key]
  (or (get context key)
      (throw (ex-info (str "Missing (or nil) component " (pr-str key)
                           " from Pedestal context")
                      {:reason ::missing-dependency
                       :dependency-key key
                       :context context}))))

(defn component-handler
  "Returns a Pedestal interceptor which extracts the component named
  key from the context map. The key must have been declared a
  dependency of the Pedestal server component.

  Invokes f with two arguments, the component and the Ring-style
  request map. f should return a Ring-style response map.

  You can use this to replace Ring-style handler functions with
  functions that take both a component and a request.

  Optional first argument `name` is the Pedestal interceptor `:name`."
  ([key f] (component-handler nil key f))
  ([name key f]
   (interceptor
    {:name (interceptor-name name)
     :enter (fn [context]
              (let [c (get-component context key)]
                (assoc context :response (f c (:request context)))))})))
