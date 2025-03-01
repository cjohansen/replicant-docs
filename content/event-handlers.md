--------------------------------------------------------------------------------
:page/uri /event-handlers/
:page/title Event handlers
:page/kind :page.kind/guide
:page/order 10
--------------------------------------------------------------------------------
:block/markdown

All event handlers go in a map under the `:on` attribute key. Event handler
names are the same ones as in the browser. Replicant does not keep a list of
valid names to use -- whatever you pass it, it will pass to `addEventListener`:

--------------------------------------------------------------------------------

:block/a-lang :clj
:block/a-title Hiccup
:block/a-code

[:button
 {:on {:click
       (fn [e]
         (js/alert "Hello!"))}}
 "Click it"]

:block/b-title Result
:block/b-example repliweb.demos.oneoffs/button-click-example

--------------------------------------------------------------------------------
:block/markdown

Replicant does no special handling of the event handler function: it will behave
exactly as if you added it with `.addEventListener`. That means that `e` is a
plain old JavaScript `Event` object.

## Functions are not data

One problem with event handlers is that functions are not data. This is
unfortunate for a few reasons.

```clj
(defn render-like-button [{:keys [video user]}]
  [:button.btn
   {:on {:click (fn [e]
                  (like-video video user))}}
   "Like"])
```

This function will produce a new event handler function every time it's called.
This will cause Replicant to replace the event handler on every render, even
when it's functionally the same.

Functions not being data also means that you can't serialize the UI, limiting
your options.

<a id="data"></a>
## Event handlers as data

Replicant offers a solution to this problem by allowing event handlers to be
expressed as data. To use this feature, you must first register a global event
handler. Whenever Replicant encounters an event handler that is not a function,
it will pass the event handler data to the global handler function instead.

The global handler only needs to be registered once:

```clj
(require '[replicant.dom :as r])

(r/set-dispatch!
 (fn [event-data handler-data]
   (when (= :replicant.trigger/dom-event
            (:replicant/trigger event-data))
     (println "Event triggered!")
     (println "Event:" (:replicant/dom-event event-data))
     (println "Node:" (:replicant/node event-data))
     (println "Handler data:" handler-data))))
```

Now you can express event handlers with arbitrary data:

```clj
(defn render-like-button [{:keys [video user]}]
  [:button.btn
   {:on {:click [:like-video video user]}}
   "Like"])
```

When clicking this button, the global handler will print the following:

```
Event triggered!
Event: #object[PointerEvent [object PointerEvent]]
Node: #object[HTMLButtonElement [object HTMLButtonElement]]
Handler data: [:like-video {:video/id v7c8b} {:user/id u23f4}]
```

As you can see, the event handler data is passed through as is. Replicant infers
no meaning from this data, it is up to you to define the desired behavior.

### The trigger

In the above example, `:replicant/trigger` was checked before dispatching the
action. This key can have one of two values:

- `:replicant.trigger/dom-event` for DOM events
- `:replicant.trigger/life-cycle` for [life-cycle hooks](/life-cycle-hooks/)

When it has the value `:replicant.trigger/life-cycle`, the dispatch function
will only be called with one argument.

<a id="action-dispatch"></a>
## The action dispatch pattern

Here's one way to use data for event handlers. In response to an event from our
app, we want one of a few things to happen. We can describe an action with a
vector containing a keyword naming the type of action, and optional additional
arguments:

```clj
;; Trigger an alert
[:action/alert "Hello world!"]

;; Issue a command to the backend
[:action/issue-command
 {:command/kind :video/like-video
  :command/data {:video/id v7c8b
                 :user/id u23f4}}]
```

It can be quite handy to execute multiple actions for one event, so the contract
for our event handler data will be a collection of these action tuples:

```clj
(defn render-like-button [state]
  [:button
   {:on
    {:click [[:action/alert "You clicked!"]
             [:action/issue-command
              {:command/kind :video/like-video
               :command/data {:video/id v7c8b
                              :user/id u23f4}}]]}}
   "Like it"])
```

To make this work, we will need a dispatching function:

```clj
(defn execute-actions [actions]
  (doseq [[action & args] actions]
    (case action
      :action/alert
      (js/alert (apply str args))

      :action/issue-command
      (apply backend/issue-command args))))
```

Finally, hook it up to Replicant:

```clj
(require '[replicant.dom :as r])

(r/set-dispatch!
 (fn [event-data actions]
   (execute-actions actions)))
```

And that's it, a bare-bones action dispatching system for your app. You can make
this as sophisticated as you want. Or you could use `set-dispatch!` to hook into
a library that provides similar functionality. Your imagination is the limit.

--------------------------------------------------------------------------------
:block/id prevent-default
:block/level 3
:block/title Declaratively imperative(!?)
:block/markdown

Sometimes you need to imperatively control the event object -- e.g. by calling
`.preventDefault` on it -- but don't want to replace your data "handlers" with
functions. Since the global event handler receives the event object, you can do
it from there, and use your event handler data to communicate when you want to
do so.

We can add an action to our dispatch system that handles preventing the default
event action.

:block/lang clj
:block/code

(require '[replicant.dom :as r])

(defn execute-actions [{:keys [replicant/dom-event]} actions]
  (doseq [[action & args] actions]
    (case action
      :action/prevent-default
      (.preventDefault dom-event)

      :action/alert
      (js/alert (apply str args))

      :action/issue-command
      (apply backend/issue-command args))))

(r/render js/document.body
 [:a {:href "/"
      :on {:click [[:action/prevent-default]
                   [:action/alert "Clickety click"]]}}
  "Click!"])

### Event data

One thing missing from the action dispatch system is access to values from the
event object. Let's say you wanted to access text in an input field. How can you
do that with data? With placeholders!

`clojure.walk` is perfect for tiny data-driven templating systems. We'll use
`:event/target.value` as a placeholder for the event's target node's `value`
property. Before we run through the actions and dispatch them, we will find any
placeholders and replace them with the current value:

```clj
(require '[clojure.walk :as walk])
(require '[replicant.dom :as r])

(defn interpolate-actions [event actions]
  (walk/postwalk
   (fn [x]
     (case x
       :event/target.value (.. event -target -value)
       ;; Add more cases as needed
       x))
   actions))

(r/set-dispatch!
 (fn [event-data actions]
   (->> actions
        (interpolate-actions
         (:replicant/dom-event event-data))
        execute-actions)))
```

Now this will prompt you with what you wrote on leaving the input field.

```clj
[:input
 {:on
  {:blur
   [[:action/alert "You typed: " :event/target.value]]}}]
```
