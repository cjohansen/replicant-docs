:page/uri /keys/
:page/title Keys
:page/kind :page.kind/guide
:page/order 30
:page/body

`:replicant/key` can be used to uniquely identify an element among its siblings.
This can help Replicant make fewer DOM operations by moving a node instead of
removing it, only to recreate it and insert it in a different position.

The classic (as in, used in React since 2013) example of explicit keying an
element is when you have a list of elements of the same tag name:

```clj
;; Render 1
[:ul
 (for [fruit ["Banana" "Apple" "Orange"]]
   [:li fruit])]

;; Render 2
[:ul
 (for [fruit ["Apple" "Orange" "Banana"]]
   [:li fruit])]
```

Intuitively we perceive these two renders as reorderings. Replicant, on the
other hand, sees `li` elements that have the wrong text content. In this case,
Replicant would perform `createTextNode` and `replaceChild` three times.

Using a key can help Replicant understand the semantic difference better:

```clj
;; Render 1
[:ul
 (for [fruit ["Banana" "Apple" "Orange"]]
   [:li {:replicant/key fruit} fruit])]

;; Render 2
[:ul
 (for [fruit ["Apple" "Orange" "Banana"]]
   [:li {:replicant/key fruit} fruit])]
```

In this case, Replicant would only perform a single DOM operation: move the
first `li` element to the end of the list. `:replicant/key` can be anything, not
just strings like in this example.

It's worth noting that `:replicant/key` is a double-edged sword. It can reduce
the number of DOM operations needed. At the same time it imposes more
constraints which can make Replicant's rendering algorithm less efficient.

## Same same, but different

It's helpful to understand what Replicant considers to be "the same" node before
deciding to add a key. Any two elements currently in the same position with the
same tag name and same key are considered to be reusable — meaning that
Replicant will perform the necessary changes to turn the existing node into
whatever is occupying its space in the current hiccup.

This means that if you have a node whose children all have different tag names,
you don't need keys. If you don't have [life-cycle
hooks](/life-cycle-hooks/),
[transitions](/hiccup/#mounting-unmounting), or stateful elements like
form fields or elements with `contenteditable`, you don't need explicit keys.

In short: there's no need to proactively add `:replicant/key`.
