:page/uri /tutorials/state-atom/
:page/title State management with an atom
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
Github](https://github.com/cjohansen/replicant-state-atom): feel free to use it
as a starting template for building an app with atom based state management.
Also consider checking out the [routing tutorial](/tutorials/routing/) and the
[state management with Datascript tutorial](/tutorials/state-datascript/).
