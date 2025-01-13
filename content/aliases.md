--------------------------------------------------------------------------------
:page/uri /alias/
:page/title Aliases
:page/kind :page.kind/guide
:page/order 50
--------------------------------------------------------------------------------
:block/markdown

Aliases let you define custom hiccup tag names that can be used like any other
tag. When Replicant needs to render an aliased node, it will call on a
function you provide to expand the alias into the desired hiccup.

Aliases must be namespaced keywords. Here's a quick example:

--------------------------------------------------------------------------------
:block/comparison-size :large
:block/a-lang :clj
:block/a-title Hiccup
:block/a-code

[:daisy.ui/button.btn-primary
 {:daisy.ui/loading? true
  :on {:click [:browser/alert "Thanks!"]}}
 "Click it"]

:block/b-lang :html
:block/b-title Resulting HTML
:block/b-code

<button type="button"
        aria-busy="true"
        tabindex="-1"
        class="btn btn-primary">
  Click it
</button>

--------------------------------------------------------------------------------
:block/markdown

Like all other hiccup nodes, aliases can have an [id](/hiccup/#id) and
[classes](/hiccup/#class) on the tag name, can [nest children](/hiccup/#lists)
arbitrarily, and can [optionally provide an attributes
map](/hiccup/#attributes).

Aliases can expand to arbitrary hiccup, it doesn't have to produce a single
node:

--------------------------------------------------------------------------------
:block/comparison-size :large
:block/a-lang :clj
:block/a-title Hiccup
:block/a-code

(require '[daisy.ui :as ui])

[::ui/toast {::ui/type :info}
 "New message arrived"]

:block/b-lang :html
:block/b-title Resulting HTML
:block/b-code

<div class="toast">
  <div class="alert alert-info">
    <span>New message arrived.</span>
  </div>
</div>

--------------------------------------------------------------------------------
:block/markdown

Aliases can nest, a feature that has some interesting applications that are
further explored in the [sortable table tutorial](/tutorials/sortable-table/).

:block/comparison-size :large
:block/a-lang :clj
:block/a-title Hiccup
:block/a-code

(require '[daisy.ui :as ui])

[::ui/tabs
 [::ui/tab {:href "/#preview"}
  "Preview"]
 [::ui/tab {::ui/active? true}
  "HTML"]
 [::ui/tab {:href "/#hiccup"}
  "Hiccup"]]

:block/b-lang :html
:block/b-title Resulting HTML
:block/b-code

<div role="tablist" class="tabs">
  <a role="tab"
     class="tab"
     href="/#preview">Preview</a>
  <a role="tab"
     class="tab tab-active">HTML</a>
  <a role="tab"
     class="tab"
     href="/#hiccup">Hiccup</a>
</div>

--------------------------------------------------------------------------------
:block/id defining
:block/title Defining aliases
:block/level 2
:block/markdown

To use aliases, you must tell Replicant how it should expand them. You have two
options: passing alias definitions when rendering, or globally registering
alias functions.

An alias function always receives two arguments: an attribute map and a
collection of children. The attribute map may be empty. If it contains `:class`,
it is always a collection. Children are always a flat list of child nodes,
regardless of the nesting in the source hiccup.

:block/a-lang :clj
:block/a-code

;; Alias
[:ui/bold "My bold"]

;; Attributes argument
{}

;; Children argument
'("My bold")

:block/b-lang :clj
:block/b-code

;; Alias
[:ui/btn.btn-primary
  {:class :btn-round}
  "Click" '(" " "button")]

;; Attributes argument
{:class #{:btn-round
          "btn-primary"}}

;; Children argument
'("Click" " " "button")

--------------------------------------------------------------------------------
:block/id passing
:block/level 3
:block/title Passing alias functions
:block/markdown

Alias functions can be passed to `replicant.dom/render` and
`replicant.string/render`.

```clj
(defn render-button-alias [attrs children]
  [:button.btn
   (cond-> (assoc attrs :type "button")
     (::loading? attrs) (merge :aria-busy "true"
                               :tabindex "-1"))
   children])
```

--------------------------------------------------------------------------------
:block/comparison-size :large
:block/a-lang :clj
:block/a-title Update the DOM
:block/a-code

(require '[replicant.dom :as d])

(r/render (js/document.getElementById "app")
  [:ui/btn "Click"]
  {:aliases {:ui/btn render-button-alias}})

:block/b-lang :clj
:block/b-title Render to a string
:block/b-code

(require '[replicant.string :as s])

(s/render
  [:ui/btn "Click"]
  {:aliases {:ui/btn render-button-alias}})

--------------------------------------------------------------------------------
:block/markdown

The map passed as `:aliases` is a map of alias tag name to alias render
function. If you omit aliases used in your hiccup, Replicant will render an
empty `div` with data-attributes explaining the problem.

--------------------------------------------------------------------------------
:block/id defines
:block/level 3
:block/title Defining alias functions
:block/markdown

Alias functions can be registered globally with `replicant.alias/defalias`:

```clj
(ns daisy.ui
  (:require [replicant.alias :refer [defalias]]))

(defalias btn [attrs children]
  [:button.btn
   (cond-> (assoc attrs :type "button")
     (::loading? attrs) (merge :aria-busy "true"
                               :tabindex "-1"))
   children])
```

So long as this namespace has been required, the alias will be available
anywhere without passing it explicitly to `render`. But by what name? `defalias`
creates aliases with the namespace they're created in and the name passed to
`defalias`. So in the above example: `:daisy.ui/btn`.

```clj
(require '[daisy.ui :as ui])
(require '[replicant.dom :as r])

(r/render
  (js/document.getElementById "app")
  [::ui/btn "Click"])
```

`defalias` also creates a var in the namespace with the provided name, so
`daisy.ui/btn` will be a var whose value is the alias keyword. That means that
you can also do this:

```clj
(require '[daisy.ui :as ui])
(require '[replicant.dom :as r])

(r/render
  (js/document.getElementById "app")
  [ui/btn "Click"])
```

When using vars you'll also explicitly require aliases, meaning that things like
"go to definition" in LSP will work as expected. The explicit dependency is also
essential for optimizing ClojureScript builds. The only drawback with this
approach is that you can't tack on class names to the var:
`[::ui/btn.btn-primary ,,,]` works, but `[ui/btn.btn-primary ,,,]` does not. You
can still add classes with `[ui/btn {:class :btn-primary} ,,,]`.

--------------------------------------------------------------------------------
:block/id registering
:block/level 3
:block/title Registering alias functions
:block/markdown

If you want to have globally registered alias functions, but want more control
over the alias keyword, you can use `replicant.alias/register!`:

```clj
(require '[replicant.alias :as a])

(defn render-button-alias [attrs children]
  [:button.btn
   (cond-> (assoc attrs :type "button")
     (::loading? attrs) (merge :aria-busy "true"
                               :tabindex "-1"))
   children])

(a/register! :ui/btn render-button-alias)
```

--------------------------------------------------------------------------------
:block/id debuggable-fns
:block/level 3
:block/title Debuggable alias functions
:block/markdown

Alias functions defined with `defalias` have some additional benefits. During
development, Replicant will keep track of which alias function created a piece
of hiccup, and with what arguments. This information is used to give better
error messages.

These functions may also omit the second argument if it is not used:

```clj
(defalias spinner [attrs]
  [:div.spinner attrs])
```

You can reap the same benefits without using `defalias` as well: just define the
function with `aliasfn`:

```clj
(require '[replicant.dom :as d])
(require '[replicant.alias :refer [aliasfn]])

(def render-button-alias
  (aliasfn [attrs children]
    [:button.btn
     (cond-> (assoc attrs :type "button")
       (::loading? attrs) (merge :aria-busy "true"
                                 :tabindex "-1"))
     children]))

(r/render
 (js/document.getElementById "app")
 [:ui/btn "Click"]
 {:aliases {:ui/btn render-button-alias}})
```

--------------------------------------------------------------------------------
:block/id alias-data
:block/level 2
:block/title Alias data
:block/markdown

Aliases sometime benefit from closing over data. However, if you provide an
alias definition to `render` with something like `(partial my-alias
my-alias-data)`, the alias function will be unique in every render call, which
will cause Replicant to rebuild your entire user interface on every render. Not
ideal.

The better approach is to pass data to `:alias-data` when you render. This data
will be available on the alias attribute map as `:replicant/alias-data`.
Replicant will re-render everything whenever `:alias-data` changes, so use it
sparingly, and only for more or less static data that isn't globally available,
like routing data, i18n dictionaries, theme definitions, etc.

For a practical example of using `:alias-data`, see the [i18n with aliases
tutorial](/tutorials/i18n-alias/).

--------------------------------------------------------------------------------
:block/id attributes
:block/level 2
:block/title Attributes vs parameters
:block/markdown

Namespaced keywords in the attribute map are ignored by Replicant when building
DOM nodes. You can use this fact to your advantage when building aliases.

If your alias needs parameters that should not end up as DOM attributes, name
them with namespaced keywords. This way you can pass the attribute map directly
to the node you are creating to allow users of the alias to customize it however
they see fit:

```clj
(defn render-button-alias [attrs children]
  [:button.btn
   (cond-> (assoc attrs :type "button")
     (::loading? attrs) (merge :aria-busy "true"
                               :tabindex "-1"))
   children])
```

Now consumers can do things like this, without you predicting every need that
might arise:

```clj
[:daisy.ui.btn-primary
 {:data-my-custom "lol"
  :daisy.ui/loading? true}
 "Click!"]
```

Had you not namespaced `:daisy.ui/loading?`, Replicant would have attempted to
add the DOM attribute `loading?`, which would have failed horribly.

--------------------------------------------------------------------------------
:block/id benefits
:block/level 2
:block/title Benefits
:block/markdown

This is all well and good, but what are the benefits of using aliases over
functions that return hiccup?

--------------------------------------------------------------------------------
:block/id late-bound
:block/level 3
:block/title Late bound
:block/markdown

Aliases are not expanded until their attributes or children change. A function
that returns hiccup is typically executed for every render. You can memoize the
function to achieve something of the same effect, but then you have a cache to
manage. Aliases leverage Replicant's internal representation of the DOM to do
their caching. This characteristic means aliases can improve rendering
performance.

--------------------------------------------------------------------------------
:block/id top-down
:block/level 3
:block/title Top-down versus bottom-up
:block/markdown

Related to the above point, aliases resolve top-down, as opposed to function
calls, which resolve bottom-up. An alias can manipulate its children before
nested aliases expand, whereas with nested function calls, the inner function
calls (e.g. those producing the children) will be called first.

This subtle difference means that aliases can manipulate the attributes or
overall structure of their nested aliases. This idea is further explored in the
[sortable table tutorial](/tutorials/sortable-table/).

--------------------------------------------------------------------------------
:block/id abstraction-level
:block/level 3
:block/title Raising the abstraction level of hiccup
:block/markdown

One disadvantage of writing tests against hiccup is that it contains a lot of
UX/UI details such as class names, inline styles, various accessibility
attributes, etc. These details are all more reliably verified either visually or
with browser tooling, and they are also typically more volatile than the
underlying transformation of your domain data to a user interface.

To demonstrate this point, let's look at some hiccup from the [Tic Tac Toe
tutorial](/tutorials/tic-tac-toe/):

```clj
[:div.board
 ([:div.row
   ([:button.cell
     {:on {:click nil}, :class ["cell-dim"]}
     [:div.cell-content
      {:replicant/mounting {:class "transparent"},
       :replicant/unmounting {:class "transparent"}}
      [:svg
       {:xmlns "http://www.w3.org/2000/svg", :viewBox "0 -10 108 100"}
       [:path
        {:fill "currentColor",
         :d "m1.753 69.19.36-1.08q.35-1.09 1.92-2.97 1.58-1.87 3.85-3.84 ..."}]
       [:path
        {:fill "currentColor",
         :d
         "m28.099 4.991 2.69 1.97q2.69 1.96 4.5 3.22 1.8 1.28 4.54 3.46 2.74
         ..."}]]]]
    ,,,,)]
  ,,,)]
```

A few things that would be helpful to test here are:

- That there are 3 rows with 3 cells
- That the cells contain the glyph corresponding to the right player
- That the cells not on the winning path are dimmed

Some things that are not helpful to hammer down in automated tests are:

- The level of nesting around the nodes (an artifact of the code that generated
  the data) - whether `[:div.row ,,,]` is a direct child of `[:div.board ,,,]`,
  or in a list makes no difference to the resulting view.
- [Mounting and unmounting](/hiccup/#mounting-unmounting) attributes
  Transitions are better demonstrated visually
- The exact content of the SVG

Some of these can be solved by asking directed questions with something like
[lookup](https://github.com/cjohansen/lookup), instead of writing asserts on the
raw hiccup data. But consider how aliases could have raised the level of
abstraction to tell a more compelling story:

```clj
(require '[tic-tac-toe.ui :as ui])

[::ui/board
 [::ui/row
  [::ui/cell {::ui/dim? true}
   [::ui/icon :tic-tac-toe.ui.icon/x]]
  ,,,]
 ,,,]
```

You could test and verify each of these building blocks individually both with
unit tests and with visual tools like
[Portfolio](https://github.com/cjohansen/portfolio). The test for the overall
rendering of the game could then focus on how the game is converted to a user
interface, without all the non-structural details.

### Alias data

[Side-chained data](#alias-data) is a powerful mechanism that are only available
to aliases. This can be used to "wire in" more or less static data, and can be
useful as a way to integrate third party libraries that need some level of
global configuration. See the [i18n with aliases
tutorial](/tutorials/i18n-alias/) for a practical demonstration.

### Normalized hiccup

Aliases are normalized just like all other hiccup. This means that you can use
aliases with or without attribute maps, nest the children as much as you want,
and still receive data in a uniform interface.

--------------------------------------------------------------------------------
:block/id drawbacks
:block/level 2
:block/title Drawbacks
:block/markdown

Nothing is free, and while there are many benefits to using aliases, there are
some drawbacks as well. The main drawback is indirection, which has some
distinct consequences.

### Debugging

Because aliases are expanded during rendering, Replicant does not allow them to
throw exceptions. Exceptions during rendering would leave your UI entirely
broken, as it corrupts Replicant's internal representation -- in addition to
abort the rendering of your UI.

For this reason, errors from aliases are softly communicated via development
asserts and as empty `div`s with some data attributes on them. This can be less
obvious and frustrating to work with.

Future versions of Replicant may add a more in your face error reporting to
Replicant that can alleviate this problem.

### Reduced dead code elimination

When you use aliases, it is harder for the compiler to do effective
function-level tree-shaking. You are not calling alias functions directly.
Instead, aliases are registered, either with `defalias` (or
`replicant.alias/register!`), or manually by passing them in a map to `render`,
and then indirectly referred by the hiccup. All registered aliases will make it
into your build, even the ones you are not using. From the compilers
perspective, you _are_ using them when you register them.

This means that aliases have namespace-level dead code elimination instead of
function-level dead code elimination. This doesn't need to be a huge problem.
However, if you have a library of aliases, or use code splitting with modules in
your production build, you should favor more smaller namespaces with alias
definitions over fewer bigger ones.

Using `defalias` and referring to the vars in your hiccup (e.g.
`[ui/btn "Click"]`, not `[::ui/btn "Click"]` ) helps keep relevant
alias definitions in your build, and irrelevant ones out.

--------------------------------------------------------------------------------
:block/id components
:block/level 2
:block/title Differences from components
:block/markdown

Aliases have a lot in common with "components" as they appear in React and its
peers, but there are some important differences.

### Data vs functions and objects

[Reagent](https://reagent-project.github.io/) is one of several React wrappers
for ClojureScript. On the surface, Replicant aliases look **a lot** like
Reagent's components. Here's Reagent:

```clj
(defn reagent-hello-component [name]
  [:p "Hello, " name "!"])

(defn reagent-say-hello []
  [reagent-hello-component "world"])

(require '[reagent.dom :as rd])
(rd/render [reagent-say-hello] js/document.body)
```

And here's Replicant:

```clj
(defalias replicant-hello-component [_ name]
  [:p "Hello, " name "!"])

(defn replicant-say-hello []
  [replicant-hello-component "world"])

(require '[replicant.dom :as r])
(r/render (replicant-say-hello) js/document.body)
```

When you use `defalias`, the usage in `reagent-say-hello` and
`replicant-say-hello` looks exactly the same, but don't be fooled! Replicant's
aliases are data, while Reagent's are not.

```clj
(prn [reagent-hello-component "world"])
;;=> [#object[myapp$ui$reagent_hello_component] "world"]

(prn [replicant-hello-component "world"])
;;=> [:myapp.ui/replicant-hello-component "world"]
```

The distinction is clearer without `defalias`:

```clj
(defn replicant-hello-component [_ name]
  [:p "Hello, " name "!"])

(defn replicant-say-hello []
  [::hello-component "world"])

(require '[replicant.dom :as r])

(r/render
 (say-hello)
 js/document.body
 {:aliases {::hello-component replicant-hello-component}})
```

### No local state

Components can have local state, and have their own life-cycle. When state
changes, the component is re-rendered. This means that components can offer
performance benefits at the cost of a more complex data-flow.

Replicant aliases do not have local state or separate life-cycles. The latter
point is important: You could always close over some state (e.g. an atom) in a
function, but there is no way to have Replicant re-render an alias in isolation.
This is by design, as it yields the simplest possible data-flow: top-down.

By virtue of their local state, components are essentially stateful and mutable
objects, while aliases are pure functions.

## When should you use aliases?

You may not need to use aliases at all -- that's completely fine. Aliases are an
optional feature. You can use Replicant with great success without ever using
aliases.

Having said that, here are some use cases that can benefit from aliases:

- Implementing design system components (like many of the examples in this
  document)
- Implementing compound custom elements, see the [sortable table
  tutorial](/tutorials/sortable-table/)
- Integrating third party libraries, like i18n, theming, and other cross-cutting
  concerns, see the [i18n with aliases tutorial](/tutorials/i18n-alias/)
- Encapsulating the business domain to UI conversion, see the [Tic-Tac-Toe with
  aliases tutorial](/tutorials/tic-tac-toe-alias/)
