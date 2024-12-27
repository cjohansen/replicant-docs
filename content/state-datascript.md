:page/uri /tutorials/state-datascript/
:page/title State management with Datascript
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
Github](https://github.com/cjohansen/replicant-state-datascript): feel free to
use it as a starting template for building an app with Datascript based state
management. Also consider checking out the [routing
tutorial](/tutorials/routing/) and the [state management with an atom
tutorial](/tutorials/state-atom/).
