--------------------------------------------------------------------------------
:page/uri /tutorials/state-datascript/
:page/title State management with Datascript
:page/kind :page.kind/tutorial
:page/category :tutorial.category/basics
:page/order 30

--------------------------------------------------------------------------------
:block/markdown

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

--------------------------------------------------------------------------------
:block/title Basic setup
:block/level 2
:block/id setup
:block/markdown

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
(ns state-datascript.core
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

--------------------------------------------------------------------------------
:block/title Updating the database
:block/level 2
:block/id updating-the-database
:block/markdown

Our UI now responds to changes in the database, great. The next step is to put
in place a small system for updating the database based on user interaction. To do
this we will use the [action dispatch pattern](/event-handlers/#action-dispatch)
detailed in the event handlers guide. Instead of building it from scratch, we
will use [Nexus](https://github.com/cjohansen/nexus):

```clj
(ns state-datascript.core
  (:require [datascript.core :as ds]
            [nexus.registry :as nxr]
            [replicant.dom :as r]))

(nxr/register-system->state! ds/db) ;; <==

(defn main [conn el]
  ,,,

  (r/set-dispatch!                  ;; <==
   (fn [dispatch-data actions]
     (nxr/dispatch conn dispatch-data actions)))

  ,,,)
```

We now have a tiny tailor-made framework. Next we will add an action that
updates the database:

```clj
(nxr/register-effect! :db/transact
  (fn [_ conn tx-data]
    (ds/transact! conn tx-data)))
```

Datascript's `transact!` function already takes a transaction as pure data, so
the `:db/transact` effect is a one-liner. It will serve 90%, if not more, of
your state management needs. Amazing.

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

--------------------------------------------------------------------------------
:block/title Pure domain-specific actions
:block/level 2
:block/id pure-actions
:block/markdown

While low-level utilities like `:db/transact` will take you far, it doesn't
always capture the intention in terms of your business domain well. We can solve
this by adding high-level actions that expand to low-level effects.

Here's the above UI expressed with a high-level domain action:

```clj
(defn render-page [db]
  (let [app (ds/entity db :system/app)
        clicks (:clicks app)]
    [:div
     [:h1 "Hello world"]
     [:p "Started at " (:app/started-at app)]
     [:button
      {:on {:click [[:counter/inc app]]}} ;; <==
      "Click me"]
     (when (< 0 clicks)
       [:p
        "Button was clicked "
        clicks
        (if (= 1 clicks) " time" " times")])]))
```

We can implement this action as a pure transformation to the side-effecty
`:store/assoc-in`:

```clj
(nxr/register-action! :counter/inc
  (fn [_ entity]
    [[:db/transact [[:db/add (:db/id entity) :clicks (inc (:clicks entity))]]]]))
```

As your app grows, you will add new actions like this, that are pure and easy to
test. Only add new effects when your app needs new capabilities.

--------------------------------------------------------------------------------
:block/title Batched state updates
:block/level 2
:block/id batched-swap
:block/markdown

Some interactions require adding more than one piece of state. Currently, each
`:db/transact` becomes a separate `transact!`, which causes a render. This means
that dispatching three actions will trigger three consecutive renders -- not
ideal. We can fix this by batching the `transact!`:

```clj
(nxr/register-effect! :db/transact
  ^:nexus/batch
  (fn [_ conn txes]
    (ds/transact! conn (apply concat (map first txes)))))
```

The other actions do not need to change, Nexus will know to do the right thing
based on the `^:nexus/batch` meta on the effect function.

--------------------------------------------------------------------------------
:block/title More specific transactions
:block/level 2
:block/markdown

`datascript.core/transact!` can take both full entity maps and individual
transaction functions. Transaction functions look a lot like Nexus actions:

```clj
[[:db/transact                      ;; Action
  [[:db/add eid :attr "value"]      ;; Transaction functions
   [:db/retract eid :attr "value"]
   [:db/retractEntity eid]]
]]
```

It would be neat if we could use these transaction functions directly as
actions. Luckily, that's very straight-forward to do:

```clj
(nxr/register-action! :db/add
  (fn [_ eid attr value]
    [[:db/transact [[:db/add eid attr value]]]]))

(nxr/register-action! :db/retract
  (fn [_ eid attr & [value]]
    [[:db/transact [(cond-> [:db/retract eid attr]
                      value (conj value))]]]))

(nxr/register-action! :db/retractEntity
  (fn [_ eid]
    [[:db/transact [[:db/retractEntity eid]]]]))
```

These all convert to `:db/transact`. Since we made sure to batch `:db/transact`,
all uses of any of these in one dispatch will end up in the same transaction.
We can simplify our `:counter/inc` action by using the first-class `:db/add`
action:

```clj
(nxr/register-action! :counter/inc
  (fn [_ entity]
    [[:db/add (:db/id entity) :clicks (inc (:clicks entity))]]))
```

The [code from this tutorial is available on
Github](https://github.com/cjohansen/replicant-state-datascript/tree/state-setup):
feel free to use it as a starting template for building an app with Datascript
based state management. Also consider checking out [state management with an
atom tutorial](/tutorials/state-atom/).

--------------------------------------------------------------------------------
:block/title Bonus: Routing
:block/level 2
:block/id routing
:block/markdown

In [the routing tutorial](/tutorials/routing/) we built a small routing system
for a top-down rendered app. In this bonus section, we'll integrate the routing
solution with the Datascript state management we just created.

Routing and state management are orthogonal concerns, but both need to trigger
rendering. The system as a whole will be easier to reason about if rendering
only happens one way. We'll keep the render hook on the Datascript connection,
and have the routing system render indirectly through it by transacting the
current location.

We start by copying over the router namespace:

--------------------------------------------------------------------------------
:block/size :large
:block/lang :clj
:block/code

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

(defn location->url [routes {:location/keys [page-id params query-params hash-params]}]
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

--------------------------------------------------------------------------------
:block/markdown

Next, we'll add the routing alias to the core namespace:

```clj
(ns state-datascript.core
  (:require [datascript.core :as ds]
            [nexus.registry :as nxr]
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
  (->> js/location.href
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

To handle click events, we will extract the core of the old `route-click`
function as an action. We will need to make some changes to achieve this. First
of all, we need the routes object to be a part of the Nexus system:

```clj
(defn main [conn el]
  (let [system {:conn conn, :routes router/routes}]
    ,,,

    (r/set-dispatch!
     (fn [dispatch-data actions]
       (nxr/dispatch system dispatch-data actions)))

    ,,,))
```

We will only need the routing table in side-effecting effects, so the
`system->state` function can still just return the database value:

```clj
(nxr/register-system->state! (comp ds/db :conn))
```

This way we won't need to change any of the action implementations, only the
`:db/transact` effect:

```clj
(nxr/register-effect! :db/transact
  ^:nexus/batch
  (fn [_ {:keys [conn]} txes]
    (ds/transact! conn (apply concat (map first txes)))))
```

We can now define a new effect that updates the browser's current URL:

```clj
(nxr/register-effect! :effects/update-url
  (fn [_ {:keys [routes]} new-location old-location]
    (if (router/essentially-same? new-location old-location)
      (.replaceState js/history nil "" (router/location->url routes new-location))
      (.pushState js/history nil "" (router/location->url routes new-location)))))
```

And then an action that uses this effect along with the effect that updates the
store:

```clj
(nxr/register-action! :actions/navigate
  (fn [db location]
    [[:effects/update-url location (ds/entity db :ui/location)]
     [:db/transact [(get-location-entity location)]]]))
```

We will now dispatch the navigation action from the `route-click` function:

```clj
(ns state-atom.core
  ,,,)

,,,

(defn route-click [e system]
  (let [href (find-target-href e)]
    (when-let [location (router/url->location (:routes system) href)]
      (.preventDefault e)
      (nxr/dispatch system nil [[:actions/navigate location]]))))
```

Next we'll add the event listeners for body clicks and the back button. The back
button handler will also update the database using an action:

```clj
(defn main [conn el]
  (let [system {:conn conn, :routes router/routes}]
    ,,,

    (js/document.body.addEventListener
     "click"
     #(route-click % system))

    (js/window.addEventListener
     "popstate"
     (fn [_]
       (nxr/dispatch system nil
        [[:db/transact [(get-location-entity (get-current-location))]]])))

    ,,,))
```

The final piece of the puzzle is to make sure routes are available as alias
data. The final `main` function looks like this:

```clj
(defn main [conn el]
  (let [system {:conn conn, :routes router/routes}]
    (add-watch
     conn ::render
     (fn [_ _ _ _]
       (r/render el (ui/render-page (ds/db conn))
                 {:alias-data {:routes router/routes}})))

    (r/set-dispatch!
     (fn [dispatch-data actions]
       (nxr/dispatch system dispatch-data actions)))

    (js/document.body.addEventListener
     "click"
     #(route-click % system))

    (js/window.addEventListener
     "popstate"
     (fn [_]
       (nxr/dispatch system nil
        [[:db/transact [(get-location-entity (get-current-location))]]])))

    ;; Trigger the initial render
    (ds/transact! conn
     [{:db/ident :system/app
       :app/started-at (js/Date.)}
      (get-location-entity (get-current-location))])))
```

Now the app can generate links with the `:ui/a` alias just like in the routing
tutorial. You can also trigger navigation with an action, which can be useful
after posting a form (to simulate a redirect), after completing login, etc.

As usual, the [full source is available on
Github](https://github.com/cjohansen/replicant-state-datascript).
