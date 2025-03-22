--------------------------------------------------------------------------------
:page/uri /tutorials/forms/
:page/title Data-driven form processing
:page/kind :page.kind/tutorial
:page/category :tutorial.category/forms
:page/order 65

--------------------------------------------------------------------------------
:block/markdown

Handling user input via forms can be challenging. It usually requires some state
management, you may want to add validation, and then processing the input and
possibly cleaning up the form for use again. This three-part tutorial will
demonstrate some techniques to data-driven form processing in increasing levels
of abstraction.

In this first tutorial we will use event handlers on individual fields to
process a form. This approach is fine for small forms, and gives you a lot of
control. In [the next installment](/tutorials/first-class-forms/) we will
investigate a more high-level approach to processing entire forms -- an approach
that requires a bit more setup, but can streamline form work a lot. Finally we
will develop that idea into fully [declarative, data-driven
forms](/tutorials/declarative-forms/).

--------------------------------------------------------------------------------
:block/title Setup
:block/level 2
:block/id setup
:block/markdown

The setup for this tutorial is based on [the state management with Datascript
tutorial](/tutorials/state-datascript/). If you want to follow along, grab [the
setup on Github](https://github.com/cjohansen/replicant-forms/tree/setup), and
follow the README to get running.

Any form of state management will work fine, but the way Datascript allows us to
easily address data will come in very handy in this exercise.

--------------------------------------------------------------------------------
:block/title The task
:block/level 2
:block/id task
:block/markdown

Gather 'round everyone, today we're beating the deadest of horses: we're
building a todo list UI. But don't worry, we won't call it a todo list, instead
we'll call it a practice log. It will offer us a nice opportunity to start with
a simple form of a single input field (what to practice), and later expand it
with some more details (how hard was it, how long did you work on it, etc).

In this first tutorial, we will build the UI to add items to the practice log,
display them in a list, and toggle them done.

--------------------------------------------------------------------------------
:block/title Rendering the form
:block/level 2
:block/id render
:block/markdown

    Our first order of business is to get a form on the screen. Open
`src/toil/ui.cljc` and update it with the following:

```clj
(ns toil.ui)

(defn render-task-form [db]
  [:form.mb-4.flex.gap-2.max-w-screen-sm
   [:input.input.input-bordered.w-full
    {:type "text"
     :name "name"
     :placeholder "What will you practice?"}]
   [:button.btn.btn-primary "Add"]])

(defn render-page [db]
  [:main.md:p-8.p-4.max-w-screen-m
   [:h1.text-2xl.mb-4 "Practice log"]
   (render-task-form db)])
```

--------------------------------------------------------------------------------
:block/markdown

Next up is the event handler. By adding an [input
event](https://developer.mozilla.org/en-US/docs/Web/API/Element/input_event)
handler on the input element we can update the database on every keystroke:

```clj
(defn render-task-form [db]
  [:form.mb-4.flex.gap-2.max-w-screen-sm
   [:input.input.input-bordered.w-full
    {:type "text"
     :name "name"
     :placeholder "What do you need to practice?"
     :on {:input [[:db/transact
                   [{:db/ident ::task
                     :task/name :event/target.value}]]]}}]
   [:button.btn.btn-primary "Add"]])
```

The text is transacted on a temporary database entity, identified by
`:db/ident ::task`. If you look at the console while typing in the field, you
will notice that each keystroke is being recorded:

```sh
| :db/transact [{:db/ident :toil.ui/task, :task/name "M"}]
| :db/transact [{:db/ident :toil.ui/task, :task/name "Ma"}]
| :db/transact [{:db/ident :toil.ui/task, :task/name "Maj"}]
| :db/transact [{:db/ident :toil.ui/task, :task/name "Majo"}]
| :db/transact [{:db/ident :toil.ui/task, :task/name "Major"}]
| :db/transact [{:db/ident :toil.ui/task, :task/name "Major "}]
| :db/transact [{:db/ident :toil.ui/task, :task/name "Major s"}]
| :db/transact [{:db/ident :toil.ui/task, :task/name "Major sc"}]
| :db/transact [{:db/ident :toil.ui/task, :task/name "Major sca"}]
| :db/transact [{:db/ident :toil.ui/task, :task/name "Major scal"}]
| :db/transact [{:db/ident :toil.ui/task, :task/name "Major scale"}]
```

Since the transacts will cause a re-render, we can now make render-time
decisions based on what the user has typed in so far. For example, we can make
sure the submit button is disabled until the user types something in.

First we need to retrieve what the user typed. In Datascript we can retrieve an
entity by a unique attribute with `(d/entity db [attribute value])`. An entity
behaves like a map so we can look up the key we wrote the text to previously.

Putting the pieces together, we get this:

```clj
(ns toil.ui
  (:require [datascript.core :as d]))                       ;; 1.

(defn render-task-form [db]
  (let [text (:task/name (d/entity db [:db/ident ::task]))] ;; 2.
    [:form.mb-4.flex.gap-2.max-w-screen-sm
     [:input ,,,]
     [:button.btn.btn-primary
      (cond-> {}
        (empty? text) (assoc :disabled "disabled"))         ;; 3.
      "Add"]]))
```

1. Require Datascript so we can use the `entity` function
2. Retrieve the currently typed in text
3. Disable the button when there's no text (once for the right DaisyUI look and
   again semantically for the browser)

--------------------------------------------------------------------------------
:block/level 2
:block/id submitting
:block/title Submitting the form
:block/markdown

After typing in some text, the user should be able to submit the form. We will
do this with the submit event on the form. This enables the user to simply hit
enter in the input field to submit the form, or click the button. By relying on
a standard browser feature, we're also making the solution accessible with
barely any effort.

One caveat with the submit event is that the browser's default behavior when
submitting a form is to perform an HTTP request. Without an [`action`
attribute](https://developer.mozilla.org/en-US/docs/Web/HTML/Element/form#action)
on the form, this will manifest as a page reload. To avoid this we need to call
`.preventDefault` on the event object. But we don't want to use an inline
function just to do this.

--------------------------------------------------------------------------------
:block/level 3
:block/id prevent-default
:block/title Prevent default, the data-driven way
:block/markdown

Our central event handler has access to the event object. This means we can
implement an action that will do the preventing of the default for us. It's
simple enough:

```clj
(defn execute-actions [conn ^js event actions]
  (doseq [[action & args] (remove nil? actions)]
    (apply prn action args)
    (case action
      :event/prevent-default (.preventDefault event) ;; <=
      :db/transact (apply d/transact! conn args)
      (println "Unknown action" action "with arguments" args))))
```

To try it out we can add a submit event to the form that only uses the new
action:

```clj
(defn render-task-form [db]
  (let [text (:task/name (d/entity db [:db/ident ::task]))]
    [:form.mb-4.flex.gap-2.max-w-screen-sm
     {:on {:submit [[:event/prevent-default]]}} ;; 1.
     [:input ,,,]
     [:button.btn.btn-primary
      (cond-> {:type "submit"}                  ;; 2.
        (empty? text) (assoc :disabled "disabled"))
      "Add"]]))
```

Adding `:type "submit"` to the button means it will also trigger a form submit
when clicked. When you submit this form nothing much happens, other than the
console logging that the `:event/prevent-default` action was triggered.

--------------------------------------------------------------------------------
:block/level 3
:block/id process-submit
:block/title Storing a new task
:block/markdown

Now we need to add an action to store the new task when the form is submitted.
We'll once again guard against empty text:

```clj
[:form.mb-4.flex.gap-2.max-w-screen-sm
 {:on {:submit [[:event/prevent-default]
                (when-not (empty? text)
                  [:db/transact
                   [{:task/name text}]])]}}
 ,,,]
```

It would be nice to record the time at which a todo was created, so we can sort
the tasks chronologically. But how do we get the created time? The rendering
function is both pure and in a CLJC file, so it really should not be doing
`(js/Date.)`. In any case, that would give us the time for when the UI was
rendered, not when the user submitted the form.

We will use another placeholder to record the created time: `:clock/now`. To add
support for it, we expand `toil.core/interpolate` like so:

```clj
(defn interpolate [event actions]
  (walk/postwalk
   (fn [x]
     (case x
       :event/target.value (.. event -target -value)
       :clock/now (js/Date.)
       x))
   actions))
```

Then we can add another attribute to the task to be created:

```clj
[:form.mb-4.flex.gap-2.max-w-screen-sm
 {:on {:submit [[:event/prevent-default]
                (when-not (empty? text)
                  [:db/transact
                   [{:task/name text
                     :task/created-at :clock/now}]])]}} ;; <=
 ,,,]
```

If you submit the form now, the console will verify that this works:

```sh
| :db/transact [{:task/name "Major scales", :task/created-at #inst "2025-03-08T09:48:14.781-00:00"}]
```

Relying on the console isn't much of a user interface, so let's take a small
detour and render the tasks on the main page.

--------------------------------------------------------------------------------
:block/level 2
:block/id render-tasks
:block/title Rendering tasks
:block/markdown

To render tasks we must first retrieve them from the database. We'll use a
Datascript query to do so, and sort them chronologically:

```clj
(defn get-tasks [db]
  (->> (d/q '[:find ?e ?created
              :where [?e :task/created-at ?created]]
            db)
       (sort-by second)
       (mapv #(d/entity db (first %)))
       seq))
```

To render the tasks we will use [Phosphoricons](https://phosphoricons.com/) via
[phosphor-clj](https://github.com/cjohansen/phosphor-clj) for a nice little
graphical element:

```clj
(ns toil.ui
  (:require [datascript.core :as d]
            [phosphor.icons :as icons])) ;; <=

,,,

(defn render-task [task]
 [:div.flex.place-content-between
  [:button.cursor-pointer.flex.items-center
   [:span.w-8.pr-2
    (icons/render (icons/icon :phosphor.regular/square)
                  {:focusable "false"})]
   (:task/name task)]])

(defn render-tasks [db]
  [:ol.mb-4.max-w-screen-sm
   (for [task (get-tasks db)]
     [:li.bg-base-200.my-2.px-4.py-3.rounded.w-full
       (render-task task)])])

(defn render-page [db]
  [:main.md:p-8.p-4.max-w-screen-m
   [:h1.text-2xl.mb-4 "Practice log"]
   (render-task-form db)
   (render-tasks db)])
```

For the sake of completeness we will also make it possible to click tasks to
complete them, and add a few bells and whistles:

```clj
(defn render-task [task]
 [:div.flex.place-content-between
  [:button.cursor-pointer.flex.items-center
   {:aria-label (if (:task/complete? task)
                  "Click to complete"
                  "Click to un-complete")
    :on {:click
          [[:db/transact
            [{:db/id (:db/id task)
            :task/complete? (not (:task/complete? task))}]]]}} ;; 1.
   (if (:task/complete? task)                                  ;; 2.
     [:span.w-8.pr-2.tilt.transition.duration-1000             ;; 3.
      {:replicant/key :done                                    ;; 4.
       :replicant/mounting {:class ["text-success"]}}          ;; 5.
      (icons/render (icons/icon :phosphor.regular/check-square)
                    {:focusable "false"})]
     [:span.w-8.pr-2
      (icons/render (icons/icon :phosphor.regular/square)
                    {:focusable "false"})])
   [:span {:class (when (:task/complete? task)
                    "line-through")}
    (:task/name task)]]])
```

1. When clicking an item, toggle its complete state.
2. Display different icons depending on the complete state.
3. The `tilt` class has an animation on it, causing the element to wiggle on
   mount. The `transition` and `duration-1000` classes enable transitions for 1
   second.
4. Using a `:replicant/key` on this element ensures a new element is rendered
   when toggling to complete, which in turn ensures that the `tilt` animation is
   triggered.
5. Apply the `text-success` class briefly during mounting. This causes a nice
   little transition in conjunction with the classes mentioned in 3.

<img src="/images/tilt.gif" class="img" alt="Tilt toggle animation in action">

--------------------------------------------------------------------------------
:block/level 2
:block/id cleaning-up
:block/title Cleaning up
:block/markdown

Now that the tasks are rendered on screen, it has become obvious that our form
is missing one crucial feature. Submitting the form causes the new task to
appear in the list, which is great. Unfortunately, the task name lingers in the
form, which is less great.

We are already rendering on every keystroke and we have the text in the
database. Clearing the form can then be achieved by "controlling" the input
field, which means to control what its value is at any point:

```clj
(defn render-task-form [db]
  (let [text (:task/name (d/entity db [:db/ident ::task]))]
    [:form.mb-4.flex.gap-2.max-w-screen-sm
     ,,,
     [:input.input.input-bordered.w-full
      {:type "text"
       :name "name"
       :value text ;; <==
       :placeholder "What do you need to practice?"
       :on {:input [[:db/transact
                     [{:db/ident ::task
                       :task/name :event/target.value}]]]}}]
     ,,,]))
```

Now we can clear the form by setting the text to an empty string when storing
the task:

```clj
(defn render-task-form [db]
  (let [text (:task/name (d/entity db [:db/ident ::task]))]
    [:form.mb-4.flex.gap-2.max-w-screen-sm
     {:on {:submit [[:event/prevent-default]
                    (when-not (empty? text)
                      [:db/transact
                       [{:task/name text
                         :task/created-at :clock/now}
                        {:db/ident ::task
                         :task/name ""}]])]}} ;; <==
     [:input.input.input-bordered.w-full
      {:type "text"
       :name "name"
       :value text
       :placeholder "What do you need to practice?"
       :on {:input [[:db/transact
                     [{:db/ident ::task
                       :task/name :event/target.value}]]]}}]
     [:button.btn.btn-primary
      (cond-> {:type "submit"}
        (empty? text) (assoc :disabled "disabled"))
      "Add"]]))
```

Now the form clears beautifully when adding a task.

In this tutorial we built a form by controlling every keystroke of a single
input field. This approach gives us maximum control, but comes with some manual
book-keeping that can be a bit bothersome on larger forms. In [the next
installment](/tutorials/first-class-forms/) we will try a more systematic
approach to form handling.

The full code listing [is on
Github](https://github.com/cjohansen/replicant-forms/tree/forms-tutorial).
