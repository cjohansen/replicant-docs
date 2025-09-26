--------------------------------------------------------------------------------
:page/uri /tutorials/state-atom/
:page/title State management with atoms
:page/kind :page.kind/tutorial
:page/category :tutorial.category/basics
:page/order 20

--------------------------------------------------------------------------------
:block/markdown

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

--------------------------------------------------------------------------------
:block/title Basic setup
:block/level 2
:block/id basic-setup
:block/markdown

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
`:app/started-at` attribute. Evaluate the following expression in the REPL:

```clj
(swap! store assoc :app/started-at (js/Date.))
```

When this is evaluated, the UI should automatically update.

--------------------------------------------------------------------------------
:block/title Updating the store
:block/level 2
:block/id updating-the-store
:block/markdown

Our UI now responds to changes in the store, great. The next step is to put in
place a small system for updating the store based on user interaction. To do
this we will use the [action dispatch pattern](/event-handlers/#action-dispatch)
detailed in the event handlers guide. Instead of building it from scratch, we
will use [Nexus](https://github.com/cjohansen/nexus):

```clj
(ns state-atom.core
  (:require [nexus.registry :as nxr]
            [replicant.dom :as r]))

,,,

(nxr/register-system->state! deref)

(defn ^:dev/after-load main []
  ,,,

  (r/set-dispatch!
   (fn [dispatch-data actions]
     (nxr/dispatch store dispatch-data actions)))

  ;; Trigger the initial render
  (swap! store assoc :app/started-at (js/Date.)))
```

We now have a tiny tailor-made framework. Next we will add an effect that
updates the store:

```clj
(nxr/register-effect! :store/assoc-in
  (fn [_ store path value]
    (swap! store assoc-in path value)))
```

`:store/assoc-in` is just what the name implies: an `assoc-in` for the
application state in `store`. It's a one-liner, and will serve 90%, if not more,
of your state management needs. Amazing.

NB! This example registers actions and effects globally for convenience. See the
[Nexus docs](https://github.com/cjohansen/nexus) for how to use `nexus.core` to
avoid global state entirely.

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
(including the event handler). Even though the mechanics of updating the store
is not in this function, it is quite obvious how `(:clicks state)` and
`[:store/assoc-in [:clicks] (inc clicks)]` are related.

--------------------------------------------------------------------------------
:block/title Pure domain-specific actions
:block/level 2
:block/id pure-actions
:block/markdown

While low-level utilities like `:store/assoc-in` will take you far, it doesn't
always capture the intention in terms of your business domain well. We can solve
this by adding high-level actions that expand to low-level effects.

Here's the above UI expressed with a high-level domain action:

```clj
(defn render-page [state]
  (let [clicks (:clicks state 0)]
    [:div
     [:h1 "Hello world"]
     [:p "Started at " (:app/started-at state)]
     [:button
      {:on {:click [[:counter/inc [:clicks]]]}} ;; <==
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
  (fn [state path]
    [[:store/assoc-in path (inc (get-in state path))]]))
```

As your app grows, you will add new actions like this, that are pure and easy to
test. Only add new effects when your app needs new capabilities.

--------------------------------------------------------------------------------
:block/title Batched state updates
:block/level 2
:block/id batched-swap
:block/markdown

Some interactions require adding more than one piece of state. Currently, each
`:store/assoc-in` becomes a separate `swap!`, which causes a render. This means
that dispatching three actions will trigger three consecutive renders -- not
ideal. We can fix this by batching the `swap!`:

```clj
(nxr/register-effect! :store/assoc-in
  ^:nexus/batch
  (fn [_ store path-values]
    (swap! store
     (fn [state]
       (reduce (fn [s [path value]]
                 (assoc-in s path value)) state path-values)))))
```

The other actions do not need to change, Nexus will know to do the right thing
based on the `^:nexus/batch` meta on the effect function.

--------------------------------------------------------------------------------
:block/title Beyond assoc-in
:block/id beyond-assoc-in
:block/level 2
:block/markdown

As previously mentioned, `assoc-in` will take care of 90 % of your state
management needs. But what about the remaining 10 %? Let's add the ability to
use `dissoc` and `conj` to see how to go about it.

In order to batch `swap!`s, we need to collect side-effects in a way that can be
reduced to a single state. One way to achieve this is to change
`:store/assoc-in` to `:store/save` and then provide the operation as a keyword
-- a sub-effect of sorts.

Let's first add two helper functions to make `dissoc` and `conj` work on nested
data structures, just like `assoc-in`:

```clj
(defn dissoc-in [m path]
  (if (= 1 (count path))
    (dissoc m (first path))
    (update-in m (butlast path) dissoc (last path))))

(defn conj-in [m path v]
  (update-in m path conj v))
```

Now we can write our state updating function:

```clj
(defn update-state [state [op & args]]
  (case op
    :assoc-in (apply assoc-in state args)
    :dissoc-in (apply dissoc-in state args)
    :conj-in (apply conj-in state args)))
```

And finally we can add our new effect, which reduces over the operations with
`update-state`:

```clj
(nxr/register-effect! :store/save
  ^:nexus/batch
  (fn [_ store ops]
    (swap! store
           (fn [state]
             (reduce update-state state ops)))))
```

To use this effect we have two options: Replace existing occurrences of
`[:store/assoc-in path value]` with `[:store/save :assoc-in path value]` -- or
implement `:store/assoc-in` as an action. This way we won't have to change any
of the UI code:

```clj
(nxr/register-action! :store/assoc-in
  (fn [_ path value]
    [[:store/save :assoc-in path value]]))
```

Now you really have a good foundation for atom/map based state management.

NB! The provided implementation of `update-state` will not work on seqs and
lists. When storing app state in an atom like this I recommend you store
collections as maps keyed by id, as you can add, edit and remove items from
those with `assoc-in` and `dissoc`.

The [code from this tutorial is available on
Github](https://github.com/cjohansen/replicant-state-atom/tree/state-setup):
feel free to use it as a starting template for building an app with atom based
state management. Also consider checking out the [state management with
Datascript tutorial](/tutorials/state-datascript/).

If you'd rather build the action dispatch yourself and not use Nexus, there is
also a [hand-written
version](https://github.com/cjohansen/replicant-state-atom/tree/hand-made)
available.

--------------------------------------------------------------------------------
:block/title Bonus: Routing
:block/level 2
:block/id routing
:block/markdown

In [the routing tutorial](/tutorials/routing/) we built a small routing system
for a top-down rendered app. In this bonus section, we'll integrate the routing
solution with the atom based state management we just created.

Routing and state management are orthogonal concerns, but both need to trigger
rendering. The system as a whole will be easier to reason about if rendering
only happens one way. We'll keep the render hook on the state atom, and have the
routing system render indirectly through the atom by storing the current
location in it.

We start by copying over the router namespace:

--------------------------------------------------------------------------------
:block/size :large
:block/lang :clj
:block/code

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
(ns state-atom.core
  (:require [nexus.registry :as nxr]
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
  (->> js/location.href
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

To handle click events, we will extract the core of the old `route-click`
function as an action. We will need to make some changes to achieve this. First
of all, we need the routes object to be a part of the Nexus system:

```clj
(defn main [store el]
  ,,,

  (r/set-dispatch!
   (fn [dispatch-data actions]
     (nxr/dispatch {:store store
                    :routes router/routes} dispatch-data actions)))

  ,,,)
```

We will only need the routing table in side-effecting effects, so the
`system->state` function can still just return the deref-ed store:

```clj
(nxr/register-system->state! (comp deref :store))
```

This way we won't need to change any of the action implementations, only the
`:store/save` effect:

```clj
(nxr/register-effect! :store/save
  ^:nexus/batch
  (fn [_ {:keys [store]} ops]
    (swap! store
           (fn [state]
             (reduce update-state state ops)))))
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
  (fn [state location]
    [[:effects/update-url location (:location state)]
     [:store/assoc-in [:location] location]]))
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
button handler will also update the store using an action:

```clj
(defn main [store el]
  (let [system {:store store
                :routes router/routes}]
    ,,,

    (js/document.body.addEventListener "click" #(route-click % system))

    (js/window.addEventListener
     "popstate"
     (fn [_]
       (nxr/dispatch system nil
        [[:store/assoc-in [:location] (get-current-location)]])))

    ,,,))
```

The final piece of the puzzle is to make sure routes are available as alias
data. The final `main` function looks like this:

```clj
(defn main [store el]
  (let [system {:store store
                :routes router/routes}]
    (add-watch store ::render
     (fn [_ _ _ state]
       (r/render el (ui/render-page state) {:alias-data {:routes router/routes}})))

    (r/set-dispatch!
     (fn [dispatch-data actions]
       (nxr/dispatch system dispatch-data actions)))

    (js/document.body.addEventListener "click" #(route-click % system))

    (js/window.addEventListener
     "popstate"
     (fn [_]
       (nxr/dispatch system nil
        [[:store/assoc-in [:location] (get-current-location)]])))

    ;; Trigger the initial render
    (swap! store assoc
           :app/started-at (js/Date.)
           :location (get-current-location))))
```

Now the app can generate links with the `:ui/a` alias just like in the routing
tutorial. You can also trigger navigation with an action, which can be useful
after posting a form (to simulate a redirect), after completing login, etc.

As usual, the [full source is available on
Github](https://github.com/cjohansen/replicant-state-atom).
