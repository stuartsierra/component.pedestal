# component.pedestal

A wrapper around a [Pedestal] web server to integrate it into a
[Component] system, merging dependencies into request context.

[Pedestal]: http://pedestal.io/
[Component]: https://github.com/stuartsierra/component



## Releases and Dependency Information

No published releases yet.

You will need [Leiningen].

Install locally with `lein install` and use the following dependency:

    [com.stuartsierra/component.repl "0.1.0-SNAPSHOT"]

[Leiningen]: http://leiningen.org/



## Usage

This section describes how to create a Pedestal HTTP server using the
Pedestal component provided by this library.

Assuming the following namespace aliases:

```clojure
(ns example.server
  (:require
   [com.stuartsierra.component.pedestal :as component.pedestal]
   [com.stuartsierra.component :as component]
   [io.pedestal.http :as http]))
```

You need to construct the Pedestal [service map].

[service map]: http://pedestal.io/reference/service-map

You **must** set up an interceptor chain in the service map. (This
library adds an interceptor, so it needs the chain defined before the
service is created.) You can use [http/default-interceptors] to get
Pedestal's default interceptor chain, and optionally add
[http/dev-interceptors] for development utilities.
See [Default Interceptors] in the Pedestal documentation for details.

[http/default-interceptors]: http://pedestal.io/api/pedestal.service/io.pedestal.http.html#var-default-interceptors
[http/dev-interceptors]: http://pedestal.io/api/pedestal.service/io.pedestal.http.html#var-dev-interceptors
[Default Interceptors]: http://pedestal.io/reference/default-interceptors

Your service map should also contain `::http/join? false`.

Your service map should be created in a function that takes the
Pedestal **component** (provided by this library) as an argument and
returns the service map.

Example:

```clojure
(defn my-service [pedestal-component]
  (-> {::http/host "0.0.0.0"
       ::http/port 8080
       ::http/type :jetty
       ::http/join? false
       ::http/resource-path "/public"
       ::http/routes routes}
      http/default-interceptors
      http/dev-interceptors))
```

You don't have to do anything with the Pedestal component: It's just
there in case you need access to its dependencies or configuration to
construct your service map.

Once you have a service map function, you need to construct an
instance of the Pedestal component provided by this library.

The Pedestal component is constructed with
`component.pedestal/pedestal-server`, passing in your service map
function.

For example, using the service map function defined above:

```clojure
(defn pedestal []
  (component.pedestal/pedestal-server my-service))
```

Once you have a constructor, you can add an instance of the component
to your Component system map; see the [Component] documentation for
details.


### Dependencies and Context

The purpose of this library is to connect Component's
dependency-injection mechanism with Pedestal's [context map]. This is
done using Component dependencies.

[context map]: http://pedestal.io/reference/context-map

First, find all the components that your web application will need
access to. Then declare them as **dependencies of** the Pedestal
component with `component/using` and/or `component/system-using`.
Again, see the [Component] documentation for details.

At the start of every request, the dependencies of the Pedestal
component will be **merged** into the **Pedestal context map**.

For example, if you construct the component like this:

```clojure
(defn pedestal []
  (component/using (component.pedestal/pedestal-server my-service)
    [:database :mail]))
```

Then the context map will contain the `:database` and `:mail` keys,
whose values will be the corresponding components from the Component
system map. You can use these components in Pedestal [interceptors].

[interceptors]: http://pedestal.io/reference/interceptors

Make sure your dependency keys do not clash with any keys in the
Pedestal [context map]! For example, don't name a dependency
`:request` or `:response`.


### Renaming Dependencies

As in the Component library, you are not limited to one set of names
for your components. A component can have its own **local** names for
its dependencies.

If you use the verbose map form of dependencies, then the
**component-local** keys will be used in the Pedestal context map.

As an example, if you declare your dependencies like this:

```clojure
(defn pedestal []
  (component/using (component.pedestal/pedestal-server my-service)
    {:db :database
     :m  :mail))
;;    ^   ^
;;    |   |
;;    |   \- keys in the system map
;;    |
;;    \- component-local keys: merged into Pedestal context
```

Then the Pedestal context map would have the database component at
`:db` and the mail component at `:m`.



## Copyright and license

The MIT License (MIT)

Copyright © 2016 Stuart Sierra

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
