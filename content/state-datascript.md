:page/uri /tutorials/state-datascript/
:page/title State management: Datascript
:page/kind :page.kind/tutorial
:page/order 30
:page/body

In this tutorial we will implement state management for top-down rendering with
a [Datascript](https://github.com/tonsky/datascript) database as the global
store. See [State management with an atom](/tutorials/state-atom/) for an
alternative take on this tutorial.

This tutorial isn't really Replicant-specific: you can use the suggest approach
with any rendering library.

As explained in [top-down rendering](/top-down/), Replicant is built around the
idea that you render your entire app whenever the app state changes. In this
tutorial we will use a Datascript database to hold all the application state,
and add a listener that makes sure the app re-renders every time it changes.

## Basic setup

The database connection can be created in the main function that starts the app,
but it's rather useful to be able to access it in a REPL, so I usually `defonce`
it in a suitable place:

```clj
(require '[state-datascript.core :as app])

(def schema {}) ;; Empty for now

(defonce conn (ds/create-conn schema))
```

Next we will need a function that can render the app:

```clj
(defn render-page [db]
  (let [app (ds/entity db :system/app)]
    [:div
     [:h1 "Hello world"]
     [:p "Started at " (:app/started-at app)]]))
```

Finally, we'll add a `main` function that sets up the watcher and renders the
app when the database changes:

```clj
(ns state-atom.core
  (:require [datascript.core :as ds]
            [replicant.dom :as r]))

,,,

(defonce el (js/document.getElementById "app"))

(defn main [conn]
  (add-watch
   conn ::render
   (fn [_ _ _ _]
     (r/render el (render (ds/db conn)))))

  ;; Trigger the initial render
  (ds/transact! conn [{:db/ident :system/app
                       :app/started-at (js/Date.)}]))
```

We now have a basic setup. You can verify that things work by updating the
started at attribute. Evaluate the following expression in the REPL:

```clj
(ds/transact! conn [{:db/ident :system/app
                     :app/started-at (js/Date.)}])
```

When this is evaluated, the UI should automatically update.

## Updating the database

Our UI now responds to changes in the database, great. The next step is to put
in place a small system for updating the database based on user interaction. To
do this we will use the [action dispatch
pattern](/event-handlers/#action-dispatch) described in the event handlers
guide:

```clj
(ns state-atom.core
  (:require [clojure.walk :as walk]
            [datascript.core :as ds]
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

(defn execute-actions [conn actions]
  (doseq [[action & args] actions]
    (case action
      ;; Add actions here
      (println "Unknown action" action "with arguments" args))))

(defn main [conn el]
  ,,,

  (r/set-dispatch!
   (fn [event-data actions]
     (->> actions
          (interpolate-actions
           (:replicant/dom-event event-data))
          (execute-actions conn))))

  ;; Trigger the initial render
  (ds/transact! conn [{:db/ident :system/app
                       :app/started-at (js/Date.)}]))
```

We now have a tiny tailor-made framework. Next we will add an action that
updates the database:

```clj
(defn execute-actions [conn actions]
  (doseq [[action & args] actions]
    (case action
      :db/transact (apply ds/transact! conn args)
      (println "Unknown action" action "with arguments" args))))
```

Datascript's `transact!` function already takes transactions as pure data, so
`:db/transact` can just `apply` it. It's a one-liner, and will serve 90%, if not
more, of your state management needs. Amazing.

Let's see it in use:

```clj
(defn render-page [db]
  (let [app (ds/entity db :system/app)
        clicks (:clicks app)]
    [:div
     [:h1 "Hello world"]
     [:p "Started at " (:app/started-at app)]
     [:button
      {:on {:click [[:db/transact [[:db/add (:db/id app) :clicks (inc clicks)]]]]}}
      "Click me"]
     (when (< 0 clicks)
       [:p
        "Button was clicked "
        clicks
        (if (= 1 clicks) " time" " times")])]))
```

This is an essential Replicant UI: it's a pure function, it returns pure data
(including the event handler), and if you "speak Datascript", the relationship
between `:db/transact` and the `app` entity should be obvious.

The [code from this tutorial is available on
Github](https://github.com/cjohansen/replicant-state-datascript/tree/state-setup):
feel free to use it as a starting template for building an app with Datascript
based state management. Also consider checking out [state management with an
atom tutorial](/tutorials/state-atom/).

<a id="routing"></a>
## Bonus: Routing

In [the routing tutorial](/tutorials/routing/) we built a small routing system
for a top-down rendered app. In this bonus section, we'll integrate the routing
solution with the Datascript state management we just created.

Routing and state management are orthogonal concerns, but both need to trigger
rendering. The system as a whole will be easier to reason with if rendering only
happens one way. We'll keep the render hook on the Datascript connection, and
have the routing system render indirectly through it by transacting the current
location.

We start by copying over the router namespace:

```clj
(ns state-datascript.router
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
(ns state-datascript.core
  (:require [clojure.walk :as walk]
            [datascript.core :as ds]
            [replicant.alias :as alias]
            [replicant.dom :as r]
            [state-datascript.router :as router]
            [state-datascript.ui :as ui]))

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
(ns state-datascript.core
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

To store the location in Datascript, we need some sort of unique identifier. We
can use a `:db/ident`, which can also easily retrieve the location. Because of
how Datascript does upserts, we also have to make sure the various parameters
are explicitly set, or we will have a hard time clearing out parameters:

```clj
(defn get-location-entity [location]
  (into {:db/ident :ui/location
         :location/query-params {}
         :location/hash-params {}
         :location/params {}}
        location))
```

To handle the initial routing when the app boots we can find the location in the
`main` function and transact it when we trigger the initial render:

```clj
;; Trigger the initial render
(ds/transact! conn
 [{:db/ident :system/app
   :app/started-at (js/Date.)}
  (get-location-entity (get-current-location))])
```

To handle click events, we will copy over and adjust the `route-click` function.
Instead of triggering rendering directly, it will now transact the location --
which will cause rendering to happen:

```clj
(ns state-datascript.core
  ,,,)

,,,

(defn route-click [e conn routes]
  (let [href (find-target-href e)]
    (when-let [location (router/url->location routes href)]
      (.preventDefault e)
      (if (router/essentially-same? location (ds/entity (ds/db conn) :ui/location))
        (.replaceState js/history nil "" href)
        (.pushState js/history nil "" href))
      (ds/transact! conn [(-> (router/url->location router/routes href)
                              get-location-entity)]))))
```

Now we'll add the event listeners for body clicks and the back button. The back
button handler will also update the database:

```clj
(defn main [store el]
  ,,,

  (js/document.body.addEventListener
   "click"
   #(route-click % conn router/routes))

  (js/window.addEventListener
   "popstate"
   (fn [_] (ds/transact! conn [(-> (get-current-location)
                                   get-location-entity)])))

  ,,,)
```

The final piece of the puzzle is to make sure routes are available as alias
data. The final `main` function looks like this:

```clj
(defn main [conn el]
  (add-watch
   conn ::render
   (fn [_ _ _ _]
     (r/render el (ui/render-page (ds/db conn)) {:alias-data {:routes router/routes}})))

  (r/set-dispatch!
   (fn [event-data actions]
     (->> actions
          (interpolate-actions
           (:replicant/dom-event event-data))
          (execute-actions conn))))

  (js/document.body.addEventListener
   "click"
   #(route-click % conn router/routes))

  (js/window.addEventListener
   "popstate"
   (fn [_] (ds/transact! conn [(-> (get-current-location)
                                   get-location-entity)])))

  ;; Trigger the initial render
  (ds/transact! conn [{:db/ident :system/app
                       :app/started-at (js/Date.)}
                      (get-location-entity (get-current-location))]))
```

Now the app can generate links with the `:ui/a` alias just like in the routing
tutorial. As usual, the [full source is available on
Github](https://github.com/cjohansen/replicant-state-datascript).
