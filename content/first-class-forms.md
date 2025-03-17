--------------------------------------------------------------------------------
:page/uri /tutorials/first-class-forms/
:page/title Data-driven first class forms
:page/kind :page.kind/tutorial
:page/order 66

--------------------------------------------------------------------------------
:block/markdown

In this second tutorial on data-driven form processing, we'll pick up where [the
first form tutorial](/tutorials/forms/) left off, and devise an approach to form
processing that makes forms a first-class concept in the code base.

In this tutorial:

- [Rendering the form](#render)
- [Extracting form data](#form-data)
- [Submitting the form](#submit)
- [Validation](#validation)
- [Final touches](#final-touches)

--------------------------------------------------------------------------------
:block/title Setup
:block/level 2
:block/id setup
:block/markdown

The setup for this tutorial is the result of the first form tutorial, which in
turn is based on [the state management with Datascript
tutorial](/tutorials/state-datascript/). If you want to follow along, grab [the
setup on
Github](https://github.com/cjohansen/replicant-forms/tree/forms-tutorial), and
follow the README to get running.

--------------------------------------------------------------------------------
:block/title The task
:block/level 2
:block/id task
:block/markdown

In this tutorial we will add an edit button to each task. When clicked, it will
display a form that can edit not only the task name, but also details like
priority and duration. We will handle validation and processing for the form as
a whole, as opposed to per field like in the previous tutorial.

--------------------------------------------------------------------------------
:block/title Rendering the form
:block/level 2
:block/id render
:block/markdown

To get off the ground we need to call the form into being. We will start by
adding a button to each task:

```clj
(defn render-task [task]
  [:div.flex.place-content-between
   ,,,
   [:button.w-6
    {:aria-label "Edit"
     :on {:click [[:db/transact
                   [[:db/add (:db/id task)
                     :task/editing? true]]]]}}
    (icons/render
     (icons/icon :phosphor.regular/gear)
     {:focusable "false"})]])
```

When `:task/editing?` is `true`, we will display a form in place of the current
view. We can do that with an `if` in the loop through all the tasks:

```clj
(defn render-edit-form [task]
  "I am a form")

(defn render-tasks [db]
  [:ol.mb-4.max-w-screen-sm
   (for [task (get-tasks db)]
     [:li.bg-base-200.my-2.px-4.py-3.rounded.w-full
      (if (:task/editing? task)
        (render-edit-form task)
        (render-task task))])])
```

We'll start the form with a single input field for editing the task name:

```clj
(defn render-edit-form [task]
  [:form.my-4.flex.flex-col.gap-4
   [:div.flex.items-center
    [:label.basis-24 {:for "task/name"} "Task"]
    [:input.grow.input.input-bordered
     {:type "text"
      :name "task/name"
      :id "task/name"
      :default-value (:task/name task)}]]
   [:div.flex.flex-row.gap-4
    [:button.btn.btn-primary {:type "submit"}
     "Save"]]])
```

In the first tutorial, we implemented fully controlled input fields and set the
`:value` on every render. This time we're not doing that, so instead of setting
`:value`, we're using `:default-value`. This will ensure that the field is
mounted with the stored value, but Replicant won't update it after the user
starts editing it.

The interesting bits in the form quickly drown in
[DaisyUI](https://daisyui.com/)/[Tailwind](https://tailwindcss.com/) classes.
Before we add more fields to this, let's introduce some helper functions. First
of all, we'll probably need more input fields:

```clj
(defn keyword->s [k]
  (if-let [ns (namespace k)]
    (str ns "/" (name k))
    (name k)))

(defn text-input [m k]
  (let [id (keyword->s k)]
    [:input.grow.input.input-bordered
     {:type "text"
      :name id
      :id id
      :default-value (get m k)}]))

(defn render-edit-form [task]
  [:form.my-4.flex.flex-col.gap-4
   [:div.flex.items-center
    [:label.basis-24 {:for "task/name"} "Task"]
    (text-input task :task/name)]
   [:div.flex.flex-row.gap-4
    [:button.btn.btn-primary {:type "submit"}
     "Save"]]])
```

You might wonder why we're using `"task/name"` for the input field's name and id
fields. This will become clear when we extract data from the form, but the short
answer is the following:

```clj
(keyword "task/name") ;;=> :task/name
```

Extracting the `text-input` function made the form slightly more readable. But
there is some ugly duplication between the label and the input field. Let's add
another helper to deal with that:

```clj
(defn input-field [label m k f]
  [:div.flex.items-center
   [:label.basis-24 {:for (keyword->s k)} label]
   (f m k)])

(defn render-edit-form [task]
  [:form.my-4.flex.flex-col.gap-4
   (input-field "Task" task :task/name text-input)
   [:div.flex.flex-row.gap-4
    [:button.btn.btn-primary {:type "submit"}
     "Save"]]])
```

This looks pretty good. Adding another field is a breeze now:

```clj
(defn render-edit-form [task]
  [:form.my-4.flex.flex-col.gap-4
   (input-field "Task" task :task/name text-input)
   (input-field "Duration" task :task/duration text-input)
   [:div.flex.flex-row.gap-4
    [:button.btn.btn-primary {:type "submit"}
     "Save"]]])
```

Duration probably should be a number. We could add a `number-field`, but we
could also just pass some options to the `text-input` function:

```clj
(defn text-input [m k & [attrs]] ;; <=
  (let [id (keyword->s k)]
    [:input.grow.input.input-bordered
     (into ;; <=
      {:type "text"
       :name id
       :id id
       :default-value (get m k)}
      attrs)])) ;; <=

(defn input-field [label m k f & args] ;; <=
  [:div.flex.items-center
   [:label.basis-24 {:for (keyword->s k)} label]
   (apply f m k args)]) ;; <=

(defn render-edit-form [task]
  [:form.my-4.flex.flex-col.gap-4
   ,,,
   (input-field "Duration" task :task/duration
     text-input {:type "number"})
   ,,,])
```

Now the duration field only allows typing numbers.

The next field will be a select that allows the user to specify the task's
priority. First we'll add a select function:

```clj
(defn select [m k options]
  (let [selected (get m k)
        id (keyword->s k)]
    [:select.grow.select.select-bordered
     {:name id
      :id id}
     (for [{:keys [value label]} options]
       [:option
        (cond-> {:value (cond-> value
                          (keyword? value) keyword->s)}
          (= value selected) (assoc :default-selected true))
        label])]))
```

Then we can add a priority field to the form:

```clj
(def priorities
  [{:value :task.priority/high
    :label "High"}
   {:value :task.priority/medium
    :label "Medium"}
   {:value :task.priority/low
    :label "Low"}])

(defn render-edit-form [task]
  [:form.my-4.flex.flex-col.gap-4
   ,,,
   (input-field "Priority" task :task/priority select priorities)
   ,,,])
```

The final touch will be to introduce a checkbox, and add a field to control
the complete state of the task:

```clj
(defn checkbox [m k]
  [:input.checkbox
   (cond-> {:type "checkbox"
            :name (keyword->s k)}
     (get m k) (assoc :default-checked "checked"))])

,,,

(defn render-edit-form [task]
  [:form.my-4.flex.flex-col.gap-4
   (input-field "Task" task :task/name text-input)
   (input-field "Duration" task :task/duration text-input {:type "number"})
   (input-field "Priority" task :task/priority select priorities)
   (input-field "Complete?" task :task/complete? checkbox)
   [:div.flex.flex-row.gap-4
    [:button.btn.btn-primary {:type "submit"}
     "Save"]]])
```

And there we have our form. Now let's process it.

--------------------------------------------------------------------------------
:block/title Extracting form data
:block/level 2
:block/id form-data
:block/markdown

Our goal is to process the form as a whole, not as a series of individual
fields. If there was a way to get the current form fields as data, it could look
something like this:

```clj
[:form {:on {:submit [[:action :event/form-data]]}}
  ,,,]
```

In other words, `:event/form-data` would be a placeholder like
`:event/target.value` that gets replaced with the full form data.
[`FormData`](https://developer.mozilla.org/en-US/docs/Web/API/FormData/FormData)
can do just this for us. Since we used keyword-able names in the form fields, we
can even get a nice Clojure map out of it with little effort:

```clj
(defn gather-form-data [form-el]
  (some-> (js/FormData. form-el)
          into-array
          (.reduce
           (fn [res [key value]]
             (assoc res (keyword key) value))
           {})))
```

We can test this function by hand from a REPL by evaluating the following
snippet:

```clj
(gather-form-data (aget js/document.forms 1))

;;=>
{:task/name "Play major scales"
 :task/duration "15"
 :task/priority "task.priority/medium"}
```

This is pretty good, but still leaves something to be desired. The duration
should be a number, the priority should be a keyword, and the complete state
isn't even present.

`FormData` is an old API intended to prepare form data for sending to the
server, so it doesn't provide mechanisms for working with anything other than
strings. The fact that it doesn't even include the checkbox when it isn't
selected makes it a little inconvenient to work with. A better option is to use
`form.elements` to enumerate all the input fields:

```clj
(defn gather-form-data [^js form-el]
  (some-> (.-elements form-el)
          into-array
          (.reduce
           (fn [res ^js el]
             (assoc res (keyword (.-name el)) (.-value el)))
           {})))
```

This version includes the checkbox, but now we're getting more than we bargained
for:

```clj
{:task/name "Play major scales"
 :task/duration "15"
 :task/priority "task.priority/medium"
 :task/complete? "on"
 : ""}
```

That last blank entry is the submit button, which doesn't have a name attribute.
We can make the `gather-form-data` function a little more defensive:

```clj
(defn gather-form-data [^js form-el]
  (some-> (.-elements form-el)
          into-array
          (.reduce
           (fn [res ^js el]
             (let [k (some-> el .-name not-empty keyword)]
               (cond-> res
                 k (assoc k (.-value el)))))
           {})))
```

We can now focus on the value types. The duration should be easy enough, since
its input field already has `type="number"`:

```clj
(defn get-input-value [^js element]
  (cond
    (= "number" (.-type element))
    (when (not-empty (.-value element))
      (.-valueAsNumber element))

    :else
    (.-value element)))

(defn gather-form-data [^js form-el]
  (some-> (.-elements form-el)
          into-array
          (.reduce
           (fn [res ^js el]
             (let [k (some-> el .-name not-empty keyword)]
               (cond-> res
                 k (assoc k (get-input-value el)))))
           {})))
```

Checkboxes have two modes of operation. When a checkbox has a `value`, we want
the `FormData` behavior: include the value when it's selected, exclude it
otherwise. The other mode is where there is no value and we just want
checked/unchecked to represent `true`/`false`.

We can use `.hasAttribute` to check for an explicit value. We don't want to
exclude all empty values from the extracted form data -- an empty text input
should still result in an entry in the map. We'll extract a function to extract
the key, and include any non-nil keys in the resulting map:

```clj
(defn get-input-value [^js element]
  (cond
    ,,,

    (= "checkbox" (.-type element))
    (if (.hasAttribute element "value")
      (when (.-checked element)
        (.-value element))
      (.-checked element))

    ,,,))

(defn get-input-key [^js element]
  (when-let [k (some-> element .-name not-empty keyword)]
    (when (or (not= "checkbox" (.-type element))     ;; 1
              (.-checked element)                    ;; 2
              (not (.hasAttribute element "value"))) ;; 3
      k)))

(defn gather-form-data [^js form-el]
  (some-> (.-elements form-el)
          into-array
          (.reduce
           (fn [res ^js el]
             (let [k (get-input-key el)]
               (cond-> res
                 k (assoc k (get-input-value el)))))
           {})))
```

That `or` is a doozy and deserves an explanation:

1. Elements that aren't checkboxes should be included.
2. Checkboxes that are checked should be included.
3. Checkboxes that don't have an explicit `value` should behave as booleans, and
   should be included even when unchecked (e.g. as `false`).

To have the priority revived as a keyword, we can give ourselves a type hint via
a custom attribute on the select:

```clj
(defn select [m k options]
  (let [selected (get m k)
        id (keyword->s k)
        sample-value (-> options first :value)] ;; <==
    [:select.grow.select.select-bordered
     (cond-> {:name id
              :id id}
       (keyword? sample-value)
       (assoc :data-type "keyword")) ;; <==
     ,,,]))
```

We can use this hint in `get-input-value` to read the value as the correct type:

```clj
(defn get-input-value [^js element]
  (cond
    ,,,

    (= "keyword" (aget (.-dataset element) "type"))
    (keyword (.-value element))

    ,,,))
```

With these changes, our form data is extracted in the correct representation:

```clj
{:task/name "Play major scales"
 :task/duration 15
 :task/priority :task.priority/medium
 :task/complete? true}
```

Note that this mapping is good enough for our use, but not complete. It doesn't
cover radio buttons, it doesn't cover selects for numbers, etc. Additional types
can be added as needed, e.g. like this to support `data-type="number"`, which
could be used with a select:

```clj
(defn get-input-value [^js element]
  (cond
    ,,,

    (= "number" (aget (.-dataset element) "type"))
    (when (not-empty (.-value element))
      (parse-long (.-value element)))

    ,,,))
```

I like this approach to programming: Solve problems specifically for our
use-case but in a systematic way, as if it was a library. However, since we're
not actually building a library we don't have to cater to every conceivable
situation, keeping code size and complexity in check.

--------------------------------------------------------------------------------
:block/title Submitting the form
:block/level 2
:block/id submit
:block/markdown

Now that we can extract form data, we need to enable the `:event/form-data`
placeholder as discussed above:

```clj
(defn interpolate [event actions]
  (walk/postwalk
   (fn [x]
     (case x
       :event/target.value (.. event -target -value)
       :event/form-data (some-> event .-target gather-form-data) ;; <=
       :clock/now (js/Date.)
       x))
   actions))
```

The map that comes out of the form is _almost_ enough to update the database.
However, it's missing the id of the entity, so transacting it would wrongfully
create a new task. We can fix this by using a hidden field for the id:

:block/lang :clj
:block/size :large
:block/code

(defn render-edit-form [task]
  [:form.my-4.flex.flex-col.gap-4
   {:on {:submit [[:event/prevent-default]
                  [:db/transact
                   [:event/form-data
                    [:db/retract (:db/id task) :task/editing?]]]]}}
   (text-input task :db/id {:type "hidden" :data-type "number"})
   (input-field "Task" task :task/name text-input)
   (input-field "Duration" task :task/duration text-input {:type "number"})
   (input-field "Priority" task :task/priority select priorities)
   (input-field "Complete?" task :task/complete? checkbox)
   [:div.flex.flex-row.gap-4
    [:button.btn.btn-primary {:type "submit"}
     "Save"]]])

--------------------------------------------------------------------------------
:block/markdown

Clicking the button now updates the todo and closes the form. Pretty neat! Let's
quickly review the details:

```clj
(text-input task :db/id {:type "hidden" :data-type "number"})
```

This ensures that the form data will contain `:db/id 3` (or whatever id the task
has), which means the transact will update that task in the database.

```clj
[:db/transact
 [:event/form-data
  [:db/retract (:db/id task) :task/editing?]]]
```

This transaction does two things. First, `:event/form-data` will be replaced by
the form data map, causing the described update. The second tuple tells
Datascript to remove the `:task/editing?` attribute for the task, which will
cause the form to close, and the normal view to be rendered.

--------------------------------------------------------------------------------
:block/title Validation
:block/level 2
:block/id validation
:block/markdown

We can now create forms and handle their happy path pretty easily with our new
first-class support for form data. However, most forms require some more care
put into them than "type in some text and dump it in the database".

Form data should be validated before it's processed, and we may need to
pre-process the data a little before we can just send it off with an action like
`:db/transact`. We will introduce a new form handling action that gives us some
more flexibility.

Here's how we can use the suggested new action:

```clj
(defn render-edit-form [task]
  [:form.my-4.flex.flex-col.gap-4
   {:on {:submit [[:event/prevent-default]
                  [:form/submit :forms/edit-task (:db/id task)]]}}
   ,,,])
```

`:form/submit` is the action we will implement. `:forms/edit-task` is the
identifier of this particular form -- this will be tied to a function that can
process it. Finally, we pass in any additional arguments required for the
processing -- the task id in this case. This ability to pass data directly to
the processing function obviates the need for the hidden field we used
previously.

So what does this new action do?

1. It calls on a pure function associated with the form identifier, which will
   return some actions.
2. It executes the new set of actions.

That's it. There are several ways to do the dispatch outlined in step 1, but I
prefer the straightforwardness of a `case` when I can get away with it. Since
we're not writing a library, there is little benefit to be had from the
indirection of something like a multi-method.

Here's the code:

```clj
(ns toil.core
  (:require ,,,
            [toil.forms :as forms] ;; <=
            ,,,))

,,,

(declare execute-actions)

(defn submit-form [conn ^js event form-id & args]
  (let [data (gather-form-data (.-target event))
        actions (case form-id
                  :forms/edit-task
                  (apply forms/submit-edit-task data args))]
    (execute-actions conn event actions)))

(defn execute-actions [conn ^js event actions]
  (doseq [[action & args] (remove nil? actions)]
    (apply prn action args)
    (case action
      :event/prevent-default (.preventDefault event)
      :form/submit (apply submit-form conn event args) ;; <=
      :db/transact (apply d/transact! conn args)
      (println "Unknown action" action "with arguments" args))))
```

Note the `declare` here. It's used since `execute-actions` calls `submit-form`
which calls `execute-actions`.

We can now implement a pure function that can use the actual form data to decide
what should happen on submit. It will report validation failures if there are
any, otherwise it will persist the edit.

For validation we can check that the task name isn't blank. Just for the sake of
the example having more than one rule, we'll also enforce the arbitrary
constraint that the duration is no more than 60 minutes.

```clj
(ns toil.forms)

(defn validate-edit-task [data]
  (->> [(when (empty? (:task/name data))
          {:validation-error/field :task/name
           :validation-error/message "Please type in some text"})
        (when (< 60 (or (:task/duration data) 0))
          {:validation-error/field :task/duration
           :validation-error/message "Duration can not exceed 60 minutes"})]
       (remove nil?)))

(defn submit-edit-task [data task-id]
  (if-let [errors (seq (validate-edit-task data))]
    [[:db/transact
      [{:form/id form-id
        :form/validation-errors errors}]]]
    ,,,))
```

In order for this to work as intended, we need to tell Datascript that
`:form/id` is a unique attribute:

```clj
(ns toil.schema)

(def schema
  {:form/id {:db/unique :db.unique/identity}})
```

If you're following along, this change requires a browser refresh, since we need
the database recreated from the schema.

If we now open the edit form, blank out the task name and submit the form,
nothing much will happen. We'll need to render the validation errors as well. To
do so, we will pass the form entity to the input field function, and use it to
look for relevant validation messages:

```clj
(defn render-tasks [db]
  [:ol.mb-4.max-w-screen-sm
   (for [task (get-tasks db)]
     [:li.bg-base-200.my-2.px-4.py-3.rounded.w-full
      (if (:task/editing? task)
        (render-edit-form
          (d/entity db [:form/id :forms/edit-task]) ;; <=
          task)
        (render-task task))])])
```

`render-edit-form` simply passes the form on to `input-field`:

```clj
(defn render-edit-form [form task]
  [:form.my-4.flex.flex-col.gap-4
   {:on {:submit [[:event/prevent-default]
                  [:form/submit :forms/edit-task (:db/id task)]]}}
   (input-field form "Task" task :task/name text-input)
   (input-field form "Duration" task :task/duration text-input {:type "number"})
   (input-field form "Priority" task :task/priority select priorities)
   (input-field form "Complete?" task :task/complete? checkbox)
   [:div.flex.flex-row.gap-4
    [:button.btn.btn-primary {:type "submit"}
     "Save"]]])
```

Finally, `input-field` is where the magic happens:

```clj
(defn input-field [form label m k f & args]
  (let [error (->> (:form/validation-errors form)
                   (filter (comp #{k} :validation-error/field))
                   first)]
    (list [:div.flex.items-center
           [:label.basis-24 {:for (keyword->s k)} label]
           (cond-> (apply f m k args)
             error (hiccup/update-attrs update :class conj "input-error"))]
          (when error
            [:div.validator-hint.text-error.ml-24.-m-2.mb-2
             (:validation-error/message error)]))))
```

First we look through the form's validation errors. If there are any for the
current field, we add the `input-error` class to the input field, and we include
another `div` with the error message.

To add the class we use
[`replicant.hiccup/update-attrs`](https://cljdoc.org/d/no.cjohansen/replicant/2025.03.02/api/replicant.hiccup#update-attrs),
which is a useful helper function for manipulating hiccup structures. It works
like `update` but specifically for attributes in a hiccup element, even when the
element doesn't have explicit attributes, as in `[:h1 "Hi!"]`.

--------------------------------------------------------------------------------
:block/title Final touches
:block/level 2
:block/id final-touches
:block/markdown

### Completing the form submit

Now that we render validation errors we can complete the form processing
function:

```clj
(defn submit-edit-task [data task-id]
  (if-let [errors (seq (validate-edit-task data))]
    [[:db/transact
      [{:form/id form-id
        :form/validation-errors errors}]]]
    [[:db/transact
      [(-> data
           (assoc :db/id task-id)
           (assoc :task/editing? false))
       [:db/retractEntity [:form/id :forms/edit-task]]]]]))
```

When there are no validation errors, we transact the data like before. Since we
removed the hidden input for the `:db/id` we now add it to the data to transact
manually. Since we can now programatically manipulate the map being transacted,
we can also add `:task/editing? false` directly onto it.

The last transaction data makes sure to clear out the form.

There is one little problem remaining: If you don't fill in a duration, its
value will be `nil`. We can't simply stick a `nil` in Datascript, we need to
convert it to an explicit retraction. Here's the final version:

```clj
(defn submit-edit-task [data task-id]
  (if-let [errors (seq (validate-edit-task data))]
    [[:db/transact
      [{:form/id form-id
        :form/validation-errors errors}]]]
    (let [nil-ks (map key (filter (comp nil? val) data))]
      [[:db/transact
        (into
         [(-> (apply dissoc data nil-ks)
              (assoc :db/id task-id)
              (assoc :task/editing? false))
          [:db/retractEntity [:form/id :forms/edit-task]]]
         (for [k nil-ks]
           [:db/retract task-id k]))]])))
```

### Clearing validation errors

We currently only run our validation logic when the user submits the form. If we
could remove validation errors the moment they no longer apply, we would give
the user some positive feedback. We can achieve this by re-evaluating the
evaluation logic on input, but only when there are validation errors (instant
negative feedback isn't as helpful).

We will add another action that only validates. Like the form processing action
it will take a form id. We'll still consider the whole form, as validating
individual fields will require some adjustments to the Datascript schema and/or
more work in the actions. Besides, working on individual fields makes it hard to
clear validation errors that involve multiple fields (like "one of these must be
filled out").

We'll add another action:

```clj
(defn execute-actions [conn ^js event actions]
  (doseq [[action & args] (remove nil? actions)]
    (apply prn action args)
    (case action
      :event/prevent-default (.preventDefault event)
      :form/validate (apply validate-form conn event args) ;; <==
      :form/submit (apply submit-form conn event args)
      :db/transact (apply d/transact! conn args)
      (println "Unknown action" action "with arguments" args))))
```

`validate-form` will look very similar to `submit-form`:

```clj
(defn validate-form [conn ^js event form-id & args]
  (let [form ^js (.closest (.-target event) "form")
        data (gather-form-input-data form)
        actions (case form-id
                  :forms/edit-task
                  (apply forms/validate-edit-task-form form-id data args))]
    (execute-actions conn event actions)))
```

Since this action will be triggered from a single input instead of a form, we
use
[`closest()`](https://developer.mozilla.org/en-US/docs/Web/API/Element/closest)
to locate the form and extract the data. `forms/validate-edit-task-form` looks a
lot like the processing form from before:

```clj
(defn validate-edit-task-form [form-id data]
  [[:db/transact
    [{:form/id form-id
      :form/validation-errors (validate-edit-task data)}]]])
```

The final piece of the puzzle is to use the new action on input fields that have
validation errors:

```clj
(defn input-field [form label m k f & args]
  (let [error (->> (:form/validation-errors form)
                   (filter (comp #{k} :validation-error/field))
                   first)]
    (list [:div.flex.items-center
           [:label.basis-24 {:for (keyword->s k)} label]
           (cond-> (apply f m k args)
             error
             (hiccup/update-attrs
              #(-> %
                   (update :class conj "input-error")
                   (assoc-in
                    [:on :input]
                    [[:form/validate (:form/id form) k]]))))]
          (when error
            [:div.validator-hint.text-error.ml-24.-m-2.mb-2
             (:validation-error/message error)]))))
```

And with that, the form can automatically clear validation errors as you type.

In this tutorial we raised the abstraction level for forms by building some
utility functions. We can now process forms as a whole with dedicated functions
for validation and submits. This makes it easier to work with larger forms and
reduces the amount of manual book-keeping required. In [the next
installment](/tutorials/declarative-forms/) we will raise the bar even further
by making most of pre-processing data-driven.

The full code listing [is on
Github](https://github.com/cjohansen/replicant-forms/tree/first-class-forms-tutorial).
