--------------------------------------------------------------------------------
:page/uri /tutorials/declarative-forms/
:page/title Declarative forms
:page/kind :page.kind/tutorial
:page/order 67

--------------------------------------------------------------------------------
:block/markdown

In this third tutorial on data-driven form processing, we'll pick up where [the
second form tutorial](/tutorials/first-class-forms/) left off, and streamline
the validation and submit logic in fully declarative forms.

--------------------------------------------------------------------------------
:block/title Setup
:block/level 2
:block/id setup
:block/markdown

The setup for this tutorial is the result of the previous form tutorial, which
in turn is based on [the state management with Datascript
tutorial](/tutorials/state-datascript/). If you want to follow along, grab [the
setup on
Github](https://github.com/cjohansen/replicant-forms/tree/first-class-forms-tutorial),
and follow the README to get running.

--------------------------------------------------------------------------------
:block/title The task
:block/level 2
:block/id task
:block/markdown

In this tutorial we won't add any new features. Instead we will rework the code
in an attempt at simplifying the act of creating new forms without losing too
much control to convention and assumptions.

After the previous tutorial, this is roughly the life-cycle of a form in our
code-base:

1. Forms use the `[:form/submit form-id & args]` action for its submit action
2. The submit action validates the form and either puts validation errors in the
   state, or produces some submit actions.
3. When there are validation errors, validation will be re-evaluated on input.

`:form/submit` really just dispatches to a function with the form id, so our
form-specific handler looked like the following:

```clj
(defn process-edit-task [data task-id]
  (if-let [errors (seq (validate-edit-task data))]
    ;; validation actions
    ;; processing actions
  ))
```

We can raise the service level from our central machinery by codifying this
flow, and by providing a declarative solution for the most common validations.

Imagine that we could describe our form like this:

```clj
(defn edit-task [data task-id]
  (let [nil-ks (map key (filter (comp nil? val) data))]
    [[:db/transact
      (into
       [(-> (apply dissoc data nil-ks)
            (assoc :db/id task-id)
            (assoc :task/editing? false))]
       (for [k nil-ks]
         [:db/retract task-id k]))]]))

(def edit-task-form
  {:form/id :forms/edit-task
   :form/fields
   [{:k :task/name
     :validations [{:validation/kind :required}]}
    {:k :task/duration
     :validations
     [{:validation/kind :max-num
       :validation/message "Duration can not exceed 60 minutes"
       :max 60}]}]

   :form/handler edit-task})
```

In this scenario all the explicit validation code is offloaded to "the
framework". That means less manual coding for each new form, and fewer
opportunities for bugs. By the end of this tutorial we will be able to express
the form fully with data.

--------------------------------------------------------------------------------
:block/title Generalized validation
:block/level 2
:block/id validation
:block/markdown

We'll start by writing some functions to perform validations from the form data
structure. We can write some tests to help us along the way. The first function
will take a form description and form data, and return a list of validation
errors:

```clj
(deftest validate-form-data-test
  (testing "Validates required field"
    (is (= (forms/validate-form-data
            {:form/id :forms/test-form
             :form/fields
             [{:k :task/name
               :validations {:validation/kind :required}}]}
            {:task/name nil})
           [{:validation-error/field :task/name
             :validation-error/message "Please type in some text"}]))))
```

To implement this test we'll loop through each field, and then evaluate each
validation, collecting any validation errors we encounter:

```clj
(defn validate-field [field validation data]
  (case (:validation/kind validation)
    :required
    (when (nil? data)
      {:validation-error/field field
       :validation-error/message "Please type in some text"})))

(defn validate-form-data [form data]
  (->> (:form/fields form)
       (mapcat
        (fn [{:keys [k validations]}]
          (let [field-data (get data k)]
            (keep #(validate-field k % field-data) validations))))))
```

Our goal data structure provided a custom `:validation/message` for one of the
validations, so let's support that for the required validation as well:

```clj
(testing "Validates required field with custom message"
  (is (= (forms/validate-form-data
          {:form/id :forms/test-form
           :form/fields
           [{:k :task/name
             :validations [{:validation/kind :required
                            :validation/message "Oh no!"}]}]}
          {:task/name nil})
         [{:validation-error/field :task/name
           :validation-error/message "Oh no!"}])))
```

The fix is a well-placed `or`:

```clj
(defn validate-field [field validation data]
  (case (:validation/kind validation)
    :required
    (when (nil? data)
      {:validation-error/field field
       :validation-error/message
       (or (:validation/message validation)
           "Please type in some text")})))
```

Adding support for the max number validation is straight-forward:

```clj
(defn validate-field [field validation data]
  (case (:validation/kind validation)
    :required
    ,,,

    :max-num
    (when (< (:max validation) (or data 0))
      {:validation-error/field field
       :validation-error/message
       (or (:validation/message validation)
           (str "Should be max " (:max validation)))})))
```

Check [the code on
Github](https://github.com/cjohansen/replicant-forms/tree/declarative-forms)
for full details on all the tests.

The next function we need is one that can be used as a validation action. It
will be very similar to the one we wrote before, but not specific to any one
form:

```clj
(defn validate [form data]
  [[:db/transact
    [{:form/id (:form/id form)
      :form/validation-errors (validate-form-data form data)}]]])
```

--------------------------------------------------------------------------------
:block/title Generalized submits
:block/level 2
:block/id submit
:block/markdown

Now that we can validate forms based on declarative form descriptions, it's time
to handle submits. We can drive this implementation with tests as well.

Our current form processing action looks like this:

```clj
[:form/submit :forms/edit-task (:db/id task)]
```

This means our new submit function should be able to take in whatever arguments
an individual form needs (like the task id) and pass it to the handler. Here's
the first test:

```clj
(deftest submit-form-test
  (testing "Validates form"
    (is (= (forms/submit
            {:form/id :forms/test-form
             :form/fields
             [{:k :task/name
               :validations [{:validation/kind :required}]}]}
            {:task/name nil}
            1 ;; task id
            )
           [[:db/transact
             [{:form/id :forms/test-form
               :form/validation-errors
               [{:validation-error/field :task/name
                 :validation-error/message "Please type in some text"}]}]]]))))
```

The implementation will look a lot like `validate-form`:

```clj
(defn submit [form data & args]
  (if-let [errors (seq (validate-form-data form data))]
    [[:db/transact
      [{:form/id (:form/id form)
        :form/validation-errors errors}]]]))
```

Next up, we will make sure that the `:form/handler` is called for a valid form:

```clj
(testing "Calls form handle when form is valid"
  (is (= (forms/submit
          {:form/id :forms/test-form
           :form/handler (fn [data task-id]
                           [[:db/transact [(assoc data :db/id task-id)]]])}
          {:task/name "Do it!"}
          1)
         [[:db/transact
           [{:db/id 1
             :task/name "Do it!"}]]])))
```

And here's the implementation:

```clj
(defn submit [form data & args]
  (if-let [errors (seq (validate-form-data form data))]
    [[:db/transact
      [{:form/id (:form/id form)
        :form/validation-errors errors}]]]
    (apply (:form/handler form) data args)))
```

This is pretty good, but we're missing some details from our previous
implementation. Specifically, the form state is not cleaned up.

Whether cleaning up is the job of individual forms or the machinery is up for
discussion. If the machinery does it, it will be impossible to keep the form in
its final state when submitting it. If, on the other hand, individual forms must
clean up, you will end up duplicating that a lot. Since it seems unlikely that
you'll want old validation errors lingering after a submit, we will put it in
the machinery -- after all, we could always make this optional at a later time.

The next question becomes how to do it. Cleaning up is achieved with this
action:

```clj
[[:db/transact [[:db/retractEntity [:form/id :forms/edit-task]]]]]
```

We _could_ add it to the list of actions returned by the handler, but if the
form is already doing a transact, this will lead to two consecutive renders. So
let's try to stick it in the existing transaction if it's there, or add an extra
action if not:

```clj
(defn submit [form data & args]
  (if-let [errors (seq (validate-form-data form data))]
    [[:db/transact
      [{:form/id (:form/id form)
        :form/validation-errors errors}]]]
    (let [actions (vec (apply (:form/handler form) data args))
          idx (.indexOf (map first actions) :db/transact)
          cleanup-tx [:db/retractEntity [:form/id (:form/id form)]]]
      (if (<= 0 idx)
        (update-in actions [idx 1] conj cleanup-tx)
        (conj actions [:db/transact [cleanup-tx]])))))
```

Again, you'll find tests for this [on
Github](https://github.com/cjohansen/replicant-forms/tree/declarative-forms).

--------------------------------------------------------------------------------
:block/title Connecting the dots
:block/level 2
:block/id wiring
:block/markdown

Now that we have support for declarative forms, let's put it to use. Now that
most of the form processing code is completely generic, it seems like it would
be a good idea to more clearly separate the generic bits from the "edit task"
specific bits. To this end we'll create a dedicated task namespace:

```clj
(ns toil.task)

(defn edit-task [data task-id]
  (let [nil-ks (map key (filter (comp nil? val) data))]
    [[:db/transact
      (into
       [(-> (apply dissoc data nil-ks)
            (assoc :db/id task-id)
            (assoc :task/editing? false))]
       (for [k nil-ks]
         [:db/retract task-id k]))]]))

(def edit-form
  {:form/id :forms/edit-task
   :form/fields
   [{:k :task/name
     :validations [{:validation/kind :required}]}
    {:k :task/duration
     :validations
     [{:validation/kind :max-num
       :validation/message "Duration can not exceed 60 minutes"
       :max 60}]}]

   :form/handler edit-task})
```

Next, we'll make a list of forms in `toil.core`:

```clj
(ns toil.core
  (:require ,,,
            [toil.task :as task]))

,,,

(def forms
  (->> [task/edit-form]
       (map (juxt :form/id identity))
       (into {})))
```

And then we'll update the two form actions:

```clj
(defn submit-form [conn ^js event form-id & args]
  (->> (apply forms/submit
              (get forms form-id)
              (gather-form-data (.-target event))
              args)
       (execute-actions conn event)))

(defn validate-form [conn ^js event form-id]
  (->> (forms/validate
        (get forms form-id)
        (gather-form-data (.closest (.-target event) "form")))
       (execute-actions conn event)))
```

### Reorganizing code

We can now clean up the implementation a little more by moving the generic form
rendering functions into our forms namespace, which has become a general purpose
form library. Since we now have a dedicated task namespace, we can move the task
specific render functions from `toil.ui` there as well. Which leaves us with
this ui namespace:

```clj
(ns toil.ui
  (:require [toil.task :as task]))

(defn render-page [db]
  [:main.md:p-8.p-4.max-w-screen-m
   [:h1.text-2xl.mb-4 "Practice log"]
   (task/render-task-form db)
   (task/render-tasks db)])
```

Supporting this is a 97 line form library (of 100% pure functions) in
[`toil.forms`](https://github.com/cjohansen/replicant-forms/blob/declarative-forms/src/toil/forms.cljc)
and 119 lines of pure functions in
[`toil.task`](https://github.com/cjohansen/replicant-forms/blob/declarative-forms/src/toil/task.cljc).
What more could you want?

--------------------------------------------------------------------------------
:block/title Fully declarative forms
:block/level 2
:block/id fully-declarative
:block/markdown

So there was something more to want, eh? Well, I did promise that by the end of
this tutorial we'd get rid of the custom form submit function entirely. So let's
see what we can do about it.

```clj
(defn edit-task [data task-id]
  (let [nil-ks (map key (filter (comp nil? val) data))]
    [[:db/transact
      (into
       [(-> (apply dissoc data nil-ks)
            (assoc :db/id task-id)
            (assoc :task/editing? false))]
       (for [k nil-ks]
         [:db/retract task-id k]))]]))
```

This function does two things:

1. Convert nils to retractions
2. Add `:db/id` and `:task/editing?` to the map

Making sure `nil` becomes a retraction is such a general concept that we could
just offer it as a separate action. The main challenge with this is to find a
more generalized way to find the task id:

```clj
[:db/retract task-id k]
```

We can use either `:db/id` or any uniquely identifying attribute in this
position. So let's make a list of unique attributes:

```clj
(ns toil.schema)

(def schema
  {:form/id {:db/unique :db.unique/identity}})

(def unique-attrs
  (->> schema
       (filterv (comp :db/unique val))
       (mapv first)
       set))
```

With this list we can find the identity like this:

```clj
(or (:db/id m)
    (when-let [attr (some m schema/unique-attrs)]
      [attr (attr m)]))
```

Which becomes either a number (e.g. from `:db/id`) or an entity ref like
`[:form/id :forms/edit-task]`. Our new action looks like the following:

```clj
(defn transact-w-nils [conn txes]
  (d/transact!
   conn
   (mapcat
    (fn [tx]
      (if (map? tx)
        (let [nil-ks (map key (filter (comp nil? val) tx))
              identity (or (:db/id tx)
                           (when-let [attr (some tx schema/unique-attrs)]
                             [attr (attr tx)]))]
          (conj (for [k nil-ks]
                  [:db/retract identity k])
                (apply dissoc tx nil-ks)))
        [tx]))
    txes)))

(defn execute-actions [conn ^js event actions]
  (doseq [[action & args] (remove nil? actions)]
    (case action
      ,,,
      :db/transact-w-nils (apply transact-w-nils conn args)
      ,,,)))
```

The only remaining part of the `edit-task` function is to add the `:db/id` and
`:task/editing?`. Borrowing a trick from the [first form
tutorial](/tutorials/forms/), we can just include these in the form as hidden
fields:

```clj
(defn render-edit-form [form task]
  [:form.my-4.flex.flex-col.gap-4
   {:on {:submit [[:event/prevent-default]
                  [:form/submit :forms/edit-task (:db/id task)]]}}
   (forms/text-input task :db/id {:type "hidden" :data-type "number"})
   (forms/text-input task :task/editing? {:type "hidden"
                                          :value "false"
                                          :data-type "boolean"})
   ,,,])
```

Now we want to express the form entirely with data, including the submit
actions. Something like this:

```clj
(def edit-form
  {:form/id :forms/edit-task
   :form/fields
   [{:k :task/name
     :validations [{:validation/kind :required}]}
    {:k :task/duration
     :validations
     [{:validation/kind :max-num
       :validation/message "Duration can not exceed 60 minutes"
       :max 60}]}]

   :form/submit-actions
   [[:db/transact-w-nils [:event/form-data]]]})
```

The first step on the way is to check for `:form/submit-actions` before
expecting `:form/handler` to be a function:

```clj
(defn submit [form data & args]
  (if-let [errors (seq (validate-form-data form data))]
    ,,,
    (let [actions (vec (or (:form/submit-actions form)
                           (when-let [handler (:form/handler form)]
                             (apply (handler form) data args))))
          ,,,]
      (if (<= 0 idx)
        (update-in actions [idx 1] conj cleanup-tx)
        (conj actions [:db/transact [cleanup-tx]])))))
```

In order for this to work, we need the action to be updated to include the
actual form data, not the `:event/form-data` placeholder. The form should be
subjected to the same interpolation that our actions go through:

```clj
(defn submit-form [conn ^js event form-id & args]
  (let [actions (apply forms/submit
                       (get forms form-id)
                       (gather-form-data (.-target event))
                       args)]
    (->> (interpolate event actions)
         (execute-actions conn event))))
```

This will work, but it's a little inefficient. We're already gathering the form
data to pass to `:form/handler` when there is one. There's no reason why
`interpolate` should do it again when it encounters `:event/form-data`. We can
avoid this by passing a map of already resolved placeholder values to
`interpolate`:

```clj
(defn interpolate [event actions & [interpolations]] ;; <=
  (walk/postwalk
   (fn [x]
     (or (get interpolations x) ;; <=
         (case x
           :event/target.value (.. event -target -value)
           :event/form-data (some-> event .-target gather-form-data)
           :clock/now (js/Date.)
           x)))
   actions))

,,,

(defn submit-form [conn ^js event form-id & args]
  (let [form-data (gather-form-data (.-target event))
        actions (apply forms/submit
                       (get forms form-id)
                       form-data
                       args)]
    (->> (interpolate event actions {:event/form-data form-data}) ;; <=
         (execute-actions conn event))))

```

And there you have it. Fully functional fully data-driven forms. Check out the
full code base [full code base on
Github](https://github.com/cjohansen/replicant-forms/tree/fully-declarative-forms).

--------------------------------------------------------------------------------
:block/title In conclusion
:block/level 2
:block/id conclusion
:block/markdown

In this tutorial we kept working on our little form framework until we landed on
a solution that supports fully declarative form processing: Describe the form's
fields with their validation rules, and describe what actions should be
performed when a valid form is submitted.

Whether or not this abstraction level pays off depends on how many forms you
plan to make. If you only have one form, it's probably a bit over-done, but
already at a handful of forms this approach will pay off handsomely.

Note that we could have described all the fields of the form, even those without
specific validation rules, and used the data for rendering as well. This can
work, but it's very easy to end up with too many assumptions and a too
inflexible solution this way. Some powerful helper functions go a long way in
reducing manual rendering work while maintaining all the necessary control over
the layout.

I hope these three tutorials have given you some ideas on how to work with forms
in a data-driven frontend. You don't have to copy any of them as is, but feel
free to use elements as you see fit, or devise your own solution.
