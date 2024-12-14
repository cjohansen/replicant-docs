:page/uri /nil/
:page/title Explicit nils can be a good thing
:page/body

As stated in the [guide to Replicant flavored hiccup](/hiccup/), explicit
`nil`s in place of missing child nodes can sometimes be beneficial. Because
Replicant expects your hiccup to be produced by code, it uses `nil`s to
understand how two versions of the same node relate. Let's illustrate with an
example.

Imagine this function:

```clj
(defn render-greeting [{:keys [user]}]
  [:div
   [:h1 "Hello Clojure enthusiast!"]
   (when user
     [:p "Nice to see you, " (:user/given-name user)])
   [:p "Hope all is well today!"]])
```

If we render this twice, first without a user, then with a user, we would
produce the two following snippets of hiccup:

```clj
;; The first call with explicit nil
[:div
 [:h1 "Hello Clojure enthusiast!"]
 nil
 [:p "Hope all is well today!"]]

;; The second call with explicit nil
[:div
 [:h1 "Hello Clojure enthusiast!"]
 [:p "Nice to see you, Christian"]
 [:p "Hope all is well today!"]]
```

When rendering the second snippet, Replicant will know to insert a new node
between the `h1` and the `p` containing "Hope all is well today!". Contrast that
with leaving out the `nil`:

```clj
;; The first call without explicit nil
[:div
 [:h1 "Hello Clojure enthusiast!"]
 [:p "Hope all is well today!"]]

;; The second call without explicit nil
[:div
 [:h1 "Hello Clojure enthusiast!"]
 [:p "Nice to see you, Christian"]
 [:p "Hope all is well today!"]]
```

In this case, Replicant wil perform a different set of operations:

1. Change the text of the first `p` from `"Hope all is well today!"` to `"Nice
   to see you, Christian"`
2. Create a new `p` element with the text `"Hope all is well today!"`
3. Insert the new `p` at the end

This happens because elements are not keyed on their content, thus Replicant
concludes that the new structure has a new `p` element at the end. Normally this
isn't a problem, but if you have CSS transitions or life-cycle events on any of
these nodes, the difference would be noticable.

In short: Using explicit `nil`s to stand in for failed conditionals and similar
situations helps Replicant make better decisions.

Note that you could achieve the same effect without the explicit `nil` by adding
[`:replicant/key`](/keys/) to the last `p`. Explicit `nil`s reduce the
need for manual keying.
