:page/uri /tutorials/state-atom/
:page/title State management: Atom
:page/kind :page.kind/tutorial
:page/order 20
:page/body

In this tutorial we will implement state management for top-down rendering with
an atom as the global store. See [State management with
Datascript](/tutorials/state-datascript/) for an alternative take on this
tutorial.

This tutorial isn't really Replicant-specific: you can use the suggest approach
with any rendering library.

As explained in [top-down rendering](/top-down/), Replicant is built around the
idea that you render your entire app whenever the app state changes. In this
tutorial we will use a single atom to hold all the application state, and add a
listener that makes sure the app re-renders every time it changes.

It's useful to differentiate the atom that holds the current state, and the
snapshot/current state. I like to use the name `store` for the atom and `state`
for the value inside.

## Basic setup

The `store` can be created in the main function that starts the app, but it's
rather useful to be able to access it in a REPL, so I usually `defonce` it in a
suitable place:

```clj
(defonce store (atom {}))
```

Next we will need a function that can render the app:

```clj
(defn render [state]
  [:div
   [:h1 "Hello world"]
   [:p "Started at " (:app/started-at state)]])
```

Finally, we'll add a `main` function that sets up the watcher and renders the
app when the store changes:

```clj
(ns state-atom.core
  (:require [replicant.dom :as r]))

,,,

(defonce el (js/document.getElementById "app"))

(defn ^:dev/after-load main []
  (add-watch
   store ::render
   (fn [_ _ _ state]
     (r/render el (render state))))

  ;; Trigger the initial render
  (swap! store assoc :app/started-at (js/Date.)))
```

We now have a basic setup. You can verify that things work by updating the
started at attribute. Evaluate the following expression in the REPL:

```clj
(swap! store assoc :app/started-at (js/Date.))
```

When this is evaluated, the UI should automatically update.

## Updating the store

Our UI now responds to changes in the store, great. The next step is to put in
place a small system for updating the store based on user interaction. To do
this we will use the [action dispatch pattern](/event-handlers/#action-dispatch)
described in the event handlers guide:

```clj
(ns state-atom.core
  (:require [clojure.walk :as walk]
            [replicant.dom :as r]))

,,,

(defn interpolate-actions [event actions]
  (walk/postwalk
   (fn [x]
     (case x
       :event/target.value (.. event -target -value)
       ;; Add more cases as needed
       x))
   actions))

(defn execute-actions [store actions]
  (doseq [[action & args] actions]
    (case action
      ;; Add actions here
      (println "Unknown action" action "with arguments" args))))

(defn ^:dev/after-load main []
  ,,,

  (r/set-dispatch!
   (fn [event-data actions]
     (->> actions
          (interpolate-actions
           (:replicant/dom-event event-data))
          (execute-actions store))))

  ;; Trigger the initial render
  (swap! store assoc :app/started-at (js/Date.)))
```

We now have a tiny tailor-made framework. Next we will add an action that
updates the store:

```clj
(defn execute-actions [store actions]
  (doseq [[action & args] actions]
    (case action
      :store/assoc-in (apply swap! store assoc-in args)
      (println "Unknown action" action "with arguments" args))))
```

`:store/assoc-in` is just what the name implies: an `assoc-in` for the
application state in `store`. It's a one-liner, and will serve 90%, if not more,
of your state management needs. Amazing.

Let's see it in use:

```clj
(defn render [state]
  (let [clicks (:clicks state 0)]
    [:div
     [:h1 "Hello world"]
     [:p "Started at " (:app/started-at state)]
     [:button
      {:on {:click [[:store/assoc-in [:clicks] (inc clicks)]]}}
      "Click me"]
     (when (< 0 clicks)
       [:p
        "Button was clicked "
        clicks
        (if (= 1 clicks) " time" " times")])]))
```

This is an essential Replicant UI: it's a pure function, it returns pure data
(including the event handler), and even though the mechanics of updating the
store is not in this function, it is quite obvious how `(:clicks state)` and
`[:store/assoc-in [:clicks] (inc clicks)]` are related.

The [code from this tutorial is available on
Github](https://github.com/cjohansen/replicant-state-atom/tree/state-setup):
feel free to use it as a starting template for building an app with atom based
state management. Also consider checking out the [state management with
Datascript tutorial](/tutorials/state-datascript/).

<a id="routing"></a>
## Bonus: Routing

In [the routing tutorial](/tutorials/routing/) we built a small routing system
for a top-down rendered app. In this bonus section, we'll integrate the routing
solution with the atom based state management we just created.

Routing and state management are orthogonal concerns, but both need to trigger
rendering. The system as a whole will be easier to reason with if rendering only
happens one way. We'll keep the render hook on the state atom, and have the
routing system render indirectly through the atom by storing the current
location in it.

We start by copying over the router namespace:

```clj
(ns state-atom.router
  (:require [domkm.silk :as silk]
            [lambdaisland.uri :as uri]))

(def routes
  (silk/routes
   [[:pages/episode [["episodes" :episode/id]]]
    [:pages/frontpage []]]))

(defn url->location [routes url]
  (let [uri (cond-> url (string? url) uri/uri)]
    (when-let [arrived (silk/arrive routes (:path uri))]
      (let [query-params (uri/query-map uri)
            hash-params (some-> uri :fragment uri/query-string->map)]
        (cond-> {:location/page-id (:domkm.silk/name arrived)
                 :location/params (dissoc arrived
                                          :domkm.silk/name
                                          :domkm.silk/pattern
                                          :domkm.silk/routes
                                          :domkm.silk/url)}
          (seq query-params) (assoc :location/query-params query-params)
          (seq hash-params) (assoc :location/hash-params hash-params))))))

(defn ^{:indent 1} location->url [routes {:location/keys [page-id params query-params hash-params]}]
  (cond-> (silk/depart routes page-id params)
    (seq query-params)
    (str "?" (uri/map->query-string query-params))

    (seq hash-params)
    (str "#" (uri/map->query-string hash-params))))

(defn essentially-same? [l1 l2]
  (and (= (:location/page-id l1) (:location/page-id l2))
       (= (not-empty (:location/params l1))
          (not-empty (:location/params l2)))
       (= (not-empty (:location/query-params l1))
          (not-empty (:location/query-params l2)))))
```

Next, we'll add the routing alias to the core namespace:

```clj
(ns state-atom.core
  (:require [clojure.walk :as walk]
            [replicant.alias :as alias]
            [replicant.dom :as r]
            [state-atom.router :as router]
            [state-atom.ui :as ui]))

(defn routing-anchor [attrs children]
  (let [routes (-> attrs :replicant/alias-data :routes)]
    (into [:a (cond-> attrs
                (:ui/location attrs)
                (assoc :href (router/location->url routes
                                                   (:ui/location attrs))))]
          children)))

(alias/register! :ui/a routing-anchor)
```

Then we'll copy over the helper functions:

```clj
(ns state-atom.core
  ,,,)

,,,

(defn find-target-href [e]
  (some-> e .-target
          (.closest "a")
          (.getAttribute "href")))

(defn get-current-location []
  (->> js/location.pathname
       (router/url->location router/routes)))
```

To handle the initial routing when the app boots we can find the location in the
`main` function and store it when we trigger the initial render:

```clj
;; Trigger the initial render
(swap! store assoc
       :app/started-at (js/Date.)
       :location (get-current-location))
```

To handle click events, we will copy over and adjust the `route-click` function.
Instead of triggering rendering directly, it will now store the location in the
store -- which will cause rendering to happen:

```clj
(ns state-atom.core
  ,,,)

,,,

(defn route-click [e store routes]
  (let [href (find-target-href e)]
    (when-let [location (router/url->location routes href)]
      (.preventDefault e)
      (if (router/essentially-same? location (:location @store))
        (.replaceState js/history nil "" href)
        (.pushState js/history nil "" href))
      (swap! store assoc :location location))))
```

Now we'll add the event listeners for body clicks and the back button. The back
button handler will also update the store:

```clj
(defn main [store el]
  ,,,
  
  (js/document.body.addEventListener
   "click"
   #(route-click % store router/routes))

  (js/window.addEventListener
   "popstate"
   (fn [_] (swap! store assoc :location (get-current-location))))
   
  ,,,)
```

The final piece of the puzzle is to make sure routes are available as alias
data. The final `main` function looks like this:

```clj
(defn main [store el]
  (add-watch
   store ::render
   (fn [_ _ _ state]
     (r/render el (ui/render-page state) {:alias-data {:routes router/routes}})))

  (r/set-dispatch!
   (fn [event-data actions]
     (->> actions
          (interpolate-actions
           (:replicant/dom-event event-data))
          (execute-actions store))))

  (js/document.body.addEventListener
   "click"
   #(route-click % store router/routes))

  (js/window.addEventListener
   "popstate"
   (fn [_] (swap! store assoc :location (get-current-location))))

  ;; Trigger the initial render
  (swap! store assoc
         :app/started-at (js/Date.)
         :location (get-current-location)))
```

Now the app can generate links with the `:ui/a` alias just like in the routing
tutorial. As usual, the [full source is available on Github]().
