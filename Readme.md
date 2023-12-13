# Fulcro Electric integration example

Shows how to integrate [Electric Clojure](https://github.com/hyperfiddle/electric) into a [Fulcro Starter App](https://github.com/fulcrologic/fulcro-template).

## Integrations steps ##

### Define your Electric app ###

```clojure
(ns app.electric-example-app
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [hyperfiddle.electric-fulcro-dom-adapter :as fulcro-adapter]
            #?(:cljs [com.fulcrologic.fulcro.routing.dynamic-routing :as dr])))

(e/defn App
  "Mount a button inside a provided fulcro react-ref (dom ref). Clicking the
  button navigates to another, injected page using the fulcro router."
  [ring-request] ; initially called on the server
  (binding [e/http-request ring-request] ; optional, only if your app needs to refer to cookies, headers, etc...
    (e/client
      (let [{:keys [this OtherPage ::fulcro-adapter/react-ref] :as props} (fulcro-adapter/GetProps. `App)]
        (binding [dom/node react-ref]
          (dom/button
            (dom/on! "click" (fn [_event] (dr/change-route this (dr/path-to OtherPage))))
            (dom/text "Go to OtherPage")))))))
```

### Add the electric middleware ###

In [app.server-components.middleware](https://github.com/hyperfiddle/fulcro-electric-template/blob/99c139588e63b629cde03782f3857a78ddd45a69/src/main/app/server_components/middleware.clj#L16C7-L17):

```clojure
(ns app.server-components.middleware
  (:require
    ...
    [hyperfiddle.electric :as e]
    [hyperfiddle.electric-ring-middleware-httpkit] ; httpkit adapter for electric, to plug into your middleware chain
    app.electric-example-app                       ; load your electric apps namespaces here for httpkit to serve them.
    ))
```

Register the electric middleware at the [end of the same file](https://github.com/hyperfiddle/fulcro-electric-template/blob/99c139588e63b629cde03782f3857a78ddd45a69/src/main/app/server_components/middleware.clj#L101):

```clojure
(defstate middleware
  :start
  (let [defaults-config (:ring.middleware/defaults-config config)
        legal-origins   (get config :legal-origins #{"localhost"})]
    (-> not-found-handler
        ,,,
      (hyperfiddle.electric-ring-middleware-httpkit/wrap-electric (fn [ring-req] (e/boot-server {} app.electric-example-app/App ring-req)))
      ,,,
      (wrap-defaults defaults-config))))
```


### Run Electric from a Fulcro component: ###

In [app.ui.root](https://github.com/hyperfiddle/fulcro-electric-template/blob/99c139588e63b629cde03782f3857a78ddd45a69/src/main/app/ui/root.cljs#L17-L20):

```clojure
(ns app.ui.root
  (:require
    ...
    [hyperfiddle.electric :as e]
    [hyperfiddle.electric-fulcro-dom-adapter :refer [run-electric!]]
    app.electric-example-app
    ))

...

(declare Settings)

(defsc Main [this props]
  {:query         [:main/welcome-message]
   :initial-state {:main/welcome-message "Hi!"}
   :ident         (fn [] [:component/id :main])
   :route-segment ["main"]}
  (div :.ui.container.segment
    (h3 "Main")
    ...
    (run-electric! {:className "electric-container"} ; extra props passed to fulcro dom wrapper div
        app.electric-example-app/App ; Electric program to run, fully qualified
        {:this this, :OtherPage Settings} ; arguments to pass
        )))

(defsc Settings [this {:keys [:account/time-zone :account/real-name] :as props}]
  {:query         [:account/time-zone :account/real-name :account/crap]
   :ident         (fn [] [:component/id :settings])
   :route-segment ["settings"]
   :initial-state {}}
  (div :.ui.container.segment
    (h3 "Settings")))


```

## Difference with Fulcro ##

In Fulcro, one usually build the client side app with `npx shadow-cljs compile/watch/release :build-id` and runs the server as a separate process.
At dev time only, Electric Clojure requires you to build client and server in the same JVM. Otherwise your client and server programs might not match after you edit/eval them at the repl.

To run shadow from the same JVM as the server (dev-time only):
```clojure
(require '[shadow.cljs.devtools.api :as shadow])

(shadow/compile :build-id)
(shadow/release :build-id)

;; hot code reload:

(require '[shadow.cljs.devtools.sever :as shadow-server])
(shadow-server/start!)

(shadow/watch :build-id)
```
