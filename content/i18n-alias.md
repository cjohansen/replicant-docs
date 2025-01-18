--------------------------------------------------------------------------------
:page/uri /tutorials/i18n-alias/
:page/title Alias powered i18n
:page/kind :page.kind/tutorial
:page/order 80

--------------------------------------------------------------------------------
:block/markdown

In this tutorial we will use [aliases](/aliases/) to introduce an i18n element
to Replicant's hiccup dialect, by integrating the
[m1p library](https://github.com/cjohansen/m1p).

--------------------------------------------------------------------------------
:block/title m1p at a glance
:block/id m1p
:block/level 2
:block/markdown

[m1p](https://github.com/cjohansen/m1p) is a data interpolation library that can
be used as a DIY i18n toolkit. m1p dictionaries are maps from keywords to
arbitrary Clojure data. The values may refer to parameters that will be provided
at lookup time.

Let's see an example:

```clj
(require '[m1p.core :as m1p])

(def dictionary
  (m1p/prepare-dictionary ;; 1
   {:header/title [:fn/str "Hello, {{:greetee}}!"]}))

(m1p/lookup {} dictionary :header/title {:greetee "Internet"}) ;; 2

;;=> "Hello, Internet!"
```

1. While dictionaries are just maps, `prepare-dictionary` converts it to a
   structure that allows better performance during lookups.
2. The last argument to `lookup` is a map of parameters that can be referred by
   the value in the dictionary.

Since m1p dictionaries can contain arbitrary data, they can also contain hiccup.

--------------------------------------------------------------------------------
:block/title The goal
:block/id goal
:block/level 2
:block/markdown

The goal for this tutorial is to be able to make m1p-powered i18n lookups in
hiccup, e.g.:

```clj
(defn render-header [{:user/keys [given-name]}]
  [:h1 [:i18n/k {:greetee given-name} ::greeting]])
```

The key and parameters have now switched positions. This plays well into
Replicant's idea of what [hiccup](/hiccup/) is -- a vector with a keyword, an
optional map, and some children. In other words, designing the alias this way
means we can make lookups without arguments like this:

```clj
[:i18n/k ::status-message]
```
--------------------------------------------------------------------------------
:block/title Implementing the alias
:block/id implementing-the-alias
:block/level 2
:block/markdown

If you want to follow along, check out the [setup
tag](https://github.com/cjohansen/replicant-m1p-tutorial/releases/tag/setup)
from the tutorial's github repo.

For starters, we will use a static dictonary and establish that we're able to
make the m1p lookup with Replicant hiccup. Create `src/replicant_i18n/i18n.cljc`
with the following:

```clj
(ns replicant-i18n.i18n
  (:require [m1p.core :as m1p]
            [replicant.alias :refer [defalias]]))

(def dictionary
  (m1p/prepare-dictionary
   {:page/title "Welcome!"
    :user/greeting [:fn/str "Nice to see you, {{:user/given-name}}!"]}))

(defalias k [params [k]]
  (m1p/lookup {} dictionary k params))
```

We can verify that this works from another namespace:

```clj
(require '[replicant-i18n.i18n :as i18n])
(require '[replicant.string :as rs])

(rs/render [:h1 [i18n/k :page/title]])
;;=> "<h1>Welcome!</h1>"
```

### Different tongues

This is great and all, but it's not a real i18n solution yet. At the very least,
there should be more than one language. Let's add another dictionary. While
we're at it, let's move the dictionaries to a separate namespace.

Create `src/replicant_i18n/i18n/nb.cljc` with:

```clj
(ns replicant-i18n.i18n.nb)

(def dictionary
  {:page/title "Velkommen!"
   :user/greeting [:fn/str "Hyggelig å se deg, {{:user/given-name}}!"]})
```

Then create `src/replicant_i18n/i18n/en.cljc` with:

```clj
(ns replicant-i18n.i18n.en)

(def dictionary
  {:page/title "Welcome!"
     :user/greeting [:fn/str "Nice to see you, {{:user/given-name}}!"]})
```

Then update the dictionaries definition in `replicant-i18n.i18n` to:

```clj
(ns replicant-i18n.i18n
  (:require [m1p.core :as m1p]
            [replicant.alias :refer [defalias]]
            [replicant-i18n.i18n.nb :as nb]
            [replicant-i18n.i18n.en :as en]))

(def dictionaries
  (-> {:nb nb/dictionary
       :en en/dictionary}
      (update-vals m1p/prepare-dictionary)))
```

Now we need to adjust the alias as well - it will need to know the locale to
make a lookup. We'll start by making the locale explicit in the lookup, and
investigate our options later:

```clj
(defalias k [params [locale k]]
  (m1p/lookup {} (get dictionaries locale) k params))
```

Now we can use our multi-lingual i18n alias like so:

```clj
(require '[replicant-i18n.i18n :as i18n])
(require '[replicant.string :as rs])

(rs/render [:h1 [i18n/k :nb :page/title]])
;;=> "<h1>Velkommen!</h1>"
(rs/render [:h1 [i18n/k :en :page/title]])
;;=> "<h1>Welcome!</h1>"
```

### Loosely coupled dictionaries

If we wanted to provide the `i18n/k` alias as a library, it's quite unfortunate
that it is directly referencing the dictionaries. Ideally, the alias should work
with user-provided dictionaries. But we probably don't want to pass the
dictionaries around the entire UI -- after all, dictionaries are static data at
runtime.

Static data is exactly what Replicant's [alias data](/aliases/#alias-data) was
made to support. Let's make an adjustment to the alias definition:

```clj
(ns replicant-i18n.i18n
  (:require [m1p.core :as m1p]
            [replicant.alias :refer [defalias]]))

(defalias k [params [locale k]]
  (let [dictionary (-> (:replicant/alias-data params)
                       :dictionaries
                       (get locale))])
  (m1p/lookup {} dictionary k params))
```

Now the alias no longer has an explicit dependency on the dictionaries. Instead,
we'll have to pass them as `:alias-data` when rendering:

```clj
(ns replicant-i18n.core
  (:require [replicant-i18n.i18n :as i18n]
            [replicant.string :as rs]
            [replicant-i18n.i18n.nb :as nb]
            [replicant-i18n.i18n.en :as en]
            [m1p.core :as m1p]))

(def dictionaries
  (-> {:nb nb/dictionary
       :en en/dictionary}
      (update-vals m1p/prepare-dictionary)))

(comment
  (rs/render [:h1 [i18n/k :nb :page/title]]
             {:alias-data {:dictionaries dictionaries}})
  ;;=> "<h1>Velkommen!</h1>"
  (rs/render [:h1 [i18n/k :en :page/title]]
             {:alias-data {:dictionaries dictionaries}})
  ;;=> "<h1>Welcome!</h1>"
)
```

Note that this works exactly the same for frontend rendering with
`replicant.dom`, e.g.:

```clj
(require '[replicant.dom :as r])

(r/render [:h1 [i18n/k :nb :page/title]]
          {:alias-data {:dictionaries dictionaries}})
```

Passing dictionaries to `:alias-data` means we don't have to pass it through all
the functions that produce hiccup for the UI. It's worth noting that if the
dictionaries change, Replicant will re-render the entire UI. This will only
happen at runtime during development, so should be fine.

--------------------------------------------------------------------------------
:block/title Implicit locale
:block/id locale
:block/level 2
:block/markdown

The solution we have so far uses an explicit locale. This is nice and
predictable, if a little cumbersome. In practice you will end up passing the
locale around "everywhere". What concessions do we have to make to have an
implicit locale?

As we just learned, we can pass the locale along with the dictionaries as
`:alias-data`:

```clj
(ns replicant-i18n.i18n
  (:require [m1p.core :as m1p]
            [replicant.alias :refer [defalias]]))

(defalias k [params [k]]
  (let [{:keys [dictionaries locale]} (:replicant/alias-data params)]
    (m1p/lookup {} (get dictionaries locale) k params)))
```

With this change, locales are implicit, and none of our hiccup-making functions
need to know about it:

```clj
(rs/render [:h1 [i18n/k :page/title]]
           {:alias-data {:dictionaries dictionaries
                         :locale :nb}})
;;=> "<h1>Velkommen!</h1>"

(rs/render [:h1 [i18n/k :page/title]]
           {:alias-data {:dictionaries dictionaries
                         :locale :en}})
;;=> "<h1>Welcome!</h1>"
```

This is a pretty big win, as the UI can talk about text strings in the abstract
using our alias, and don't even need to care about the current locale.

What are the drawbacks of this approach? Since the locale now is part of
`:alias-data`, Replicant will re-render the entire UI when it changes. In most
cases this is not a big loss. Changing locales is a rare occurrence, and
unlikely to happen halway through filling out a form (a full re-render
means lost state, including in input fields). Whether or not this trade-off is
acceptable is up to you. If you want to transition translations in your UI, you
will have to work with an explicit locale.

--------------------------------------------------------------------------------
:block/title But why?
:block/id why
:block/level 2
:block/markdown

You might wonder what we gained from all this. You could just use m1p and call
`lookup` everywhere we used the `i18n/k` alias. The benefits are pretty much the
same as the ones mentioned in the [aliases guide](/aliases/):

- [Late bound](/aliases/#late-bound): The i18n alias only re-evaluates whenever
  you change the key or one of its parameters.
- [Raised abstraction level](/aliases/#abstraction-level): `[i18n/k user
  :user/greeting]` communicates the intention of welcoming the user, while
  "Welcome Christian!" is a very specific formulation. Being able to test this
  intention without getting hung up on the detais is a major benefit.

--------------------------------------------------------------------------------
:block/title Final code-listing
:block/id code-listing
:block/level 2
:block/markdown

Here is a small UI that makes use of our newly created alias and the
dictionaries. See the [tutorial
repo](https://github.com/cjohansen/replicant-m1p-tutorial) for the full details:

```clj
(ns replicant-i18n.core
  (:require [m1p.core :as m1p]
            [replicant-i18n.i18n :as i18n]
            [replicant-i18n.i18n.en :as en]
            [replicant-i18n.i18n.nb :as nb]
            [replicant.dom :as r]))

(def dictionaries
  (-> {:nb nb/dictionary
       :en en/dictionary}
      (update-vals m1p/prepare-dictionary)))

(defn render-ui [user]
  [:div
   [:h1 [i18n/k :page/title]]
   [:p [i18n/k user :user/greeting]]
   [:button {:on {:click [:switch-locale]}}
    [i18n/k :locale/switch]]])

(defn render [state]
  (r/render
   (js/document.getElementById "app")
   (render-ui {:user/given-name "Christian"})
   {:alias-data
    {:dictionaries dictionaries
     :locale (:locale state)}}))

(def other-locale
  {:en :nb
   :nb :en})

(def store (atom {}))

(defn main []
  (r/set-dispatch!
   (fn [_ _]
     (swap! store update :locale other-locale)))

  (add-watch store ::render (fn [_ _ _ state] (render state)))
  (swap! store assoc :locale :en))
```
