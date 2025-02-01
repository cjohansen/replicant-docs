--------------------------------------------------------------------------------
:page/uri /tutorials/network-writes/
:page/title Data-driven commands
:page/kind :page.kind/tutorial
:page/order 60

--------------------------------------------------------------------------------
:block/markdown

In this third part of the [networking tutorial](/tutorials/network/), we will
build a data-driven system for writing data over the network.

We pick up where [data-driven network reads](/tutorials/network-reads/) left
off, and the setup is the code we wrote there. The starting point is available
as [a branch on
Github](https://github.com/cjohansen/replicant-networking/tree/network-reads).

--------------------------------------------------------------------------------
:block/title Design goal
:block/level 2
:block/id design
:block/markdown

To perform writes over the network, we will follow a very similar design to the
one we developed when reading over the network. Reading and writing really is
quite similar, but we want slightly more pointed write semantics.

Since we already have a dedicated mechanism for reading data, we want to avoid
writes becoming a secondary read channel. For this reason, writes will only
result in success or error, and possibly novelty such as newly created ids. In
other words, we will not return newly created entities from writes. Instead
we'll add a mechanism for reloading data after performing writes.

Network reads were dubbed queries. We will refer to writes as commands. Just
like with queries, we will use a data structure to talk about commands in the
frontend, but it is up to you if you want your backend to support these commands
natively, or if you will translate commands to some REST API request or similar.

The data structure for commands is very similar to the one we made for queries:

```clj
{:command/kind :command/create-todo
 :command/data {:todo/title "Implement commands"}}
```

--------------------------------------------------------------------------------
:block/title Answering questions
:block/level 2
:block/id questions
:block/markdown

Just like we did for queries, we'll keep a log for commands, and write a few
functions to answer some important questions. The details of these are very
similar to the ones in the reads tutorial, so I'll point you to [the
code](https://github.com/cjohansen/replicant-networking/tree/network-writes) for
details.

This test sums up the gist of it:

```clj
(testing "Received successful response"
  (is (false?
       (-> (command/issue-command {} #inst "2025-01-02T06:44:13" command)
           (command/receive-response #inst "2025-01-02T06:44:13" command {:success? true})
           (command/issued? command)))))
```

--------------------------------------------------------------------------------
:block/title Making HTTP requests
:block/level 2
:block/id http-requests
:block/markdown

Once again, refer to [the query
tutorial](/tutorials/network-reads/#http-requests) for a discussion about how to
map the client-side commands to your specific backend API. The sample app has a
`/command` endpoint that can take commands directly.

We can start with basically the exact same function as last time, only changing
"query" to "command":

```clj
(defn issue-command [store command]
  (swap! store command/issue-command (js/Date.) command)
  (-> (js/fetch "/command" #js {:method "POST"
                                :body (pr-str command)})
      (.then #(.text %))
      (.then reader/read-string)
      (.then #(swap! store command/receive-response (js/Date.) command %))
      (.catch #(swap! store command/receive-response (js/Date.) command {:error
      (.-message %)}))))
```

This will allow us to issue commands. But since we decided to not mix reads with
writes, we need a way to trigger a refresh of the data after issuing some
commands. To do this, we will allow command actions to include actions to
perform on successful completion of the command (phew!). It will look like this:

```clj
[[:data/command
  {:command/kind :command/toggle-todo
   :command/data {:todo/id "ac564c"}}
  {:on-success [[:data/query items-query]]}]]
```

To support this, we need to declare `execute-actions`, since we want to call it
from a function that is itself called from `execute-actions`:

```clj
(declare execute-actions)

(defn issue-command [store command & [{:keys [on-success]}]]
  (swap! store command/issue-command (js/Date.) command)
  (-> (js/fetch "/command" #js {:method "POST"
                                :body (pr-str command)})
      (.then #(.text %))
      (.then reader/read-string)
      (.then (fn [res]
               (swap! store command/receive-response
                            (js/Date.) command res)
               (when on-success
                 (execute-actions store on-success))))
      (.catch #(swap! store command/receive-response
                            (js/Date.) command {:error (.-message %)}))))

(defn execute-actions [store actions]
  (doseq [[action & args] actions]
    (case action
      :store/assoc-in (apply swap! store assoc-in args)
      :data/query (apply query-backend store args)
      :data/command (apply issue-command store args)             ;; <==
      (println "Unknown action" action "with arguments" args))))
```

--------------------------------------------------------------------------------
:block/title Issuing commands
:block/level 2
:block/id issuing-commands
:block/markdown

With our new command action in place, we can make todo items togglable on the
frontpage:

```clj
(ns toil.frontpage
  (:require [toil.command :as command]
            [toil.query :as query]))

(def items-query
  {:query/kind :query/todo-items})

(defn render [state]
  [:main.p-8.max-w-screen-lg
   [:h1.text-2xl.mb-4 "Toil and trouble: Todos over the network"]
   (when-let [todos (query/get-result state items-query)]
     [:ul.mb-4
      (for [item todos]
        (let [command {:command/kind :command/toggle-todo
                       :command/data item}]
          [:li.my-2
           [:button.cursor-pointer
            (if (command/issued? state command)
              {:disabled true}
              {:on {:click
                    [[:data/command command
                      {:on-success [[:data/query items-query]]}]]}})
            [:span.pr-2
             (if (:todo/done? item)
               "✓"
               "▢")]]
           (:todo/title item)
           " ("
           [:ui/a.link
            {:ui/location
             {:location/page-id :pages/user
              :location/params {:user/id (:todo/created-by item)}}}
            (:todo/created-by item)]
           ")"]))])
   (if (query/loading? state items-query)
     [:button.btn.btn-primary {:disabled true}
      [:span.loading.loading-spinner]
      "Fetching todos"]
     [:button.btn.btn-primary
      {:on {:click [[:data/query items-query]]}}
      "Fetch todos"])])
```

We can now click the box/checkmark next to each item to toggle them back and
forth. And just like with queries, we can now add new commands to our frontend
without writing a single line of imperative code. Pretty nice.

--------------------------------------------------------------------------------
:block/title Issuing commands with user input
:block/level 2
:block/id issuing-commands-user-input
:block/markdown

As a final task, we will add a input field to input new todos in. For this to
work we need to grab the text from the input and include it in a command.
Luckily, we already added event interpolation in the setup in [the state
management tutorial](/tutorials/state-atom/).

Let's start by adding the form:

```clj
[:form.flex.gap-4.mb-4
 [:input.input.input-bordered.w-full.max-w-xs
  {:type "text"
   :placeholder "New todo"}]
 [:button.btn.btn-primary {:type "submit"}
  "Save todo"]]
```

To breathe life into this, we will add two actions. The first one will store the
input's text in the global store on the input event:

```clj
[:input.input.input-bordered.w-full.max-w-xs
 {:type "text"
  :placeholder "New todo"
  :value (::todo-title state)
  :on {:input [[:store/assoc-in [::todo-title] :event/target.value]]}}]
```

The second one is a click action on the button that will use this value in a
command to create a todo. On successful creation, the input field is emptied and
the todo list is refreshed:

```clj
(defn render [state]
  [:main.p-8.max-w-screen-lg
   [:h1.text-2xl.mb-4 "Toil and trouble: Todos over the network"]
   [:form.flex.gap-4.mb-4
    [:input.input.input-bordered.w-full.max-w-xs
     {:type "text"
      :placeholder "New todo"
      :value (::todo-title state)
      :on {:input [[:store/assoc-in [::todo-title] :event/target.value]]}}]
    [:button.btn.btn-primary
     {:type "button"
      :on
      (when-let [title (not-empty (::todo-title state))]
        {:click [[:data/command
                  {:command/kind :command/create-todo
                   :command/data {:todo/created-by "alice"
                                  :todo/title title}}
                  {:on-success [[:store/assoc-in [::todo-title] ""]
                                [:data/query items-query]]}]]})}
     "Save todo"]]
   ,,,])
```

A better version of this code would use a submit event on the form and a proper
`[:button {:type "submit"} ,,,]`. Doing so would require some work in
`toil.core` to be able to `.preventDefault` the event. You could also do without
the explicit `::todo-title` in the global store with more interpolation options.

The [full code is on Github](https://github.com/cjohansen/replicant-networking).
