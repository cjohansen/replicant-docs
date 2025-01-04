:page/uri /life-cycle-hooks/
:page/title Life-cycle hooks
:page/kind :page.kind/guide
:page/order 20
:page/body

Life-cycle hooks give you access to the rendered DOM node. The special
"attribute" `:replicant/on-render` registers a life-cycle hook. It will be
called whenever the underlying DOM node is changed somehow:

```clj
(defn render-map [{:keys [places]}]
  [:div
   {:replicant/on-mount
    (fn [{:keys [replicant/node]}]
      (mount-map node))

    :replicant/on-render
    (fn [{:keys [replicant/node]}]
      (update-map-places node places))}])
```

The function is called with a single argument, which is a map of these keys:

- `:replicant/trigger` always has the value `:replicant.trigger/life-cycle`
- `:replicant/life-cycle` one of `:replicant.life-cycle/mount` (initial render),
  `:replicant.life-cycle/update` (successive updates) or
  `:replicant.life-cycle/unmount` (node is being removed from the DOM).
- `:replicant/node` the DOM node

`:replicant/on-render` triggers on all updates and gives you enough information
to know what happened. If you just want to do something on mount and/or unmount,
you can use `:replicant/on-mount` and `:replicant/on-unmount`, which work
exactly like `:replicant/on-render`, except they only trigger on their
respective life-cycle events.

Life-cycle hooks can also be expressed with data and handled via
`replicant.dom/set-dispatch!`:

```clj
(require '[replicant.dom :as r])

(r/set-dispatch!
 (fn [e hook-data]
   (when (= :replicant.trigger/life-cycle
            (:replicant/trigger e))
     (println "Life-cycle hook triggered!")
     (println "Life-cycle" (:replicant/life-cycle e))
     (println "Node:" (:replicant/node e))
     (println "Hook data:" hook-data))))

(defn render-map [{:keys [places]}]
  [:div
   {:replicant/on-render
    [::update-map-places places]}])
```

For more details on this mechanism, refer to the [event handler
guide](/event-handlers/) which describes it in detail. It works exactly
the same for DOM and life-cycle events. You can use `:replicant/trigger` to
distinguish them.

Note that the global dispatch will only be called for life-cycle events when you
explicitly set a non-function value to one of `:replicant/on-render`,
`:replicant/on-mount` or `:replicant/on-unmount` on a node. You cannot use this
mechanism to react to any change in the DOM.
