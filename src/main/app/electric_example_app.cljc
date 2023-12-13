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
