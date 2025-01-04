:page/uri /tutorials/network/
:page/title Backend APIs and network
:page/kind :page.kind/tutorial
:page/order 40
:page/body

Most frontend apps need to talk to a backend API over the network to do useful
things. If you're coming from a component-based framework, you may have been
accustomed to making HTTP requests from components. That isn't possible when you
render with Replicant, so what can you do? That's what we'll explore in this
tutorial.

Because networking is a big topic, this tutorial comes in three parts. The one
you're reading now gives you a quick and dirty example of hooking a frontend up
to an API. If you have the time and patience for it, I strongly recommend
skipping the rest of this introduction and reading the in-depth ones instead:

- [A data-driven system for network reads](/tutorials/network-reads/)
- [A data-driven system for network writes](/tutorial/network-write/)

These in-depth tutorials give you a system to build on. The quick example
that follows in this introduction will demonstrate the basics of networking in a
[top-down](/top-down/) rendered app, but will not scale in any meaningful way.

## HTTP requests in a top-down world

The setup for this tutorial is based on [the state management with an atom
tutorial](/tutorials/state-atom/), including the routing extension and a small
backend. If you want to follow along, grab [the setup on
Github](https://github.com/cjohansen/replicant-networking/tree/setup), and
follow the README to get running.

In this quick tutorial we want to fire off an HTTP request and render the
results in our UI. So the first decision is: how to trigger the request? Well,
the setup has a system for triggering actions from DOM events, so let's create
one that makes an HTTP request.

From the [state management with an atom tutorial](/tutorials/state-atom/) we
have an action dispatch in `execute-actions`, let's add a new entry here:

```clj
(defn execute-actions [store actions]
  (doseq [[action & args] actions]
    (case action
      :store/assoc-in (apply swap! store assoc-in args)
      :backend/fetch-todo-items (fetch-todo-items store)         ;; <==
      (println "Unknown action" action "with arguments" args))))
```

Since the store atom is the app's only source of data, information about the
network request also needs to go in here. The first bit of information is that
the request is underway. We can use this to mark the UI as loading:

```clj
(defn fetch-todo-items [store]
  (swap! store assoc :loading-todos? true))
```

With this in place, we can already try the action from the UI:

```clj
(defn render-frontpage [state]
  [:main.p-8.max-w-screen-lg
   [:h1.text-2xl.mb-4 "Toil and trouble: Todos over the network"]
   [:button.btn.btn-primary
    (if (:loading-todos? state)
      {:disabled true}
      {:on {:click [[:backend/fetch-todo-items]]}})
    (when (:loading-todos? state)
      [:span.loading.loading-spinner])
    "Fetch todos"]])
```

Clicking this button will re-render it disabled with a spinner. Not much more
will happen, though. The next step is to make the HTTP request. The backend has
a `/query` endpoint that supports a query for the todo items, and we can use
[`fetch`](https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API/Using_Fetch)
to post to it:

```clj
(defn fetch-todo-items [store]
  (swap! store assoc :loading-todos? true)
  (js/fetch "/query" #js {:method "post"
                          :body (pr-str {:query/kind :query/todo-items})}))
```

`fetch` returns a promise. We will store the response in the state atom as well:

```clj
(defn receive-todo-items [state response]
  (cond-> (dissoc state :loading-todos?)
    (:success? response)
    (assoc :todo-items (:result response))

    (not (:success? response))
    (assoc :error "Failed to load todos")))

(defn fetch-todo-items [store]
  (swap! store assoc :loading-todos? true)
  (-> (js/fetch "/query" #js {:method "post"
                              :body (pr-str {:query/kind :query/todo-items})})
      (.then #(.text %))
      (.then #(swap! store receive-todo-items (reader/read-string %)))))
```

When receiving a response we also remove the `:loading-todos?` key. Note that we
didn't handle HTTP errors such as a broken connection. To do so, add a `.catch`
to the promise and swap in an error.

The render function can now render the data when available:

```clj
(defn render-frontpage [state]
  [:main.p-8.max-w-screen-lg
   [:h1.text-2xl.mb-4 "Toil and trouble: Todos over the network"]
   (when-let [todos (:todo-items state)]
     [:ul.mb-4
      (for [item todos]
        [:li.my-2
         [:span.pr-2
          (if (:todo/done? item)
            "✓"
            "▢")]
         (:todo/title item)])])
   [:button.btn.btn-primary
    (if (:loading-todos? state)
      {:disabled true}
      {:on {:click [[:backend/fetch-todo-items]]}})
    (when (:loading-todos? state)
      [:span.loading.loading-spinner])
    "Fetch todos"]])
```

If you click the button, the list should appear. In fact, the list will likely
appear so fast you can't even see the loading state. Because of this,
intermittent states like these can be hard to work with. Fortunately, the UI is
a pure function, so we can just call it with the right data to trigger the
loading view:

```clj
(render-frontpage {:loading-todos? true})
```

You can do this in something like
[Portfolio](https://github.com/cjohansen/portfolio) for a visual inspection,
just like we did in [the Tic Tac Toe tutorial](/tutorials/tic-tac-toe/). You can
also do it in a unit test. Isn't functional programming great?

## Next step

So that's it: a very quick and dirty way to add an HTTP request to a top-down
rendered app. But our solution leaves a lot to be desired: the action is
specific to a single API call. Adding more would mean copy-pasting a lot of
imperative code in the core namespace, and you'd end up with a lot of more or
less randomly named keys to juggle in the state atom.

A better solution, [alluded to](/top-down/#system-design) in the top-down
article, is to provide some central infrastructure for all your HTTP needs. By
generalizing the solution a little we could make it so new API calls can be
added without writing a single line of imperative code. That's exactly what we
do in [the more in-depth network reads tutorial](/tutorials/network-reads/).
This tutorial also offers a way to load data as we navigate to specific pages.

The code from this tutorial is available in [a branch on
Github](https://github.com/cjohansen/replicant-networking/tree/quick-and-dirty).
