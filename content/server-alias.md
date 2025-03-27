--------------------------------------------------------------------------------
:page/uri /tutorials/server-alias/
:page/title Server-side JS interop alias
:page/kind :page.kind/tutorial
:page/category :tutorial.category/interop
:page/order 72

--------------------------------------------------------------------------------
:block/markdown

In [Wrapping a library in an alias](/tutorials/interop-alias/), we built a
Replicant alias that offered integration with the Mapbox mapping library. In
this tutorial, we make the alias usable when rendering on the server as well --
where there are no life-cycle hooks.

--------------------------------------------------------------------------------
:block/title Setup
:block/level 2
:block/id setup
:block/markdown

The setup for this tutorial is the result of the previous interop tutorial,
which in turn is based on [the state management with an atom
tutorial](/tutorials/state-atom/). If you want to follow along, grab [the setup
on Github](https://github.com/cjohansen/replicant-maps/tree/alias), and follow
the README to get running.

--------------------------------------------------------------------------------
:block/title The task
:block/level 2
:block/id task
:block/markdown

In this tutorial we will implement a version of the `::map/marker-map` alias
that can be used with `replicant.string/render` to render server-side HTML. Then
we'll add some client-side code to ensure it can still load Mapbox and render an
interactive map on the client.

--------------------------------------------------------------------------------
:block/title The server-side alias
:block/level 2
:block/id server-side-alias
:block/markdown

The `::map/marker-map` alias is full of browser-specific ClojureScript that
won't sit well in a `cljc` file. The server-side version of the alias will be a
separate implementation in `atlas/ui/map.clj`.

This implementation will just render a placeholder for the map, and leave some
hints for a client-side script to pick up. Here it is:

```clj
(ns atlas.ui.map
  (:require [replicant.alias :refer [defalias]]))

(defalias marker-map [attrs children]
  [:div.aspect-video (assoc attrs :data-client-feature "marker-map")
   [:script
    {:type "application/edn"
     :innerHTML
     (pr-str
      (->> (keys attrs)
           (filter (comp #{"atlas.ui.map"} namespace))
           (select-keys attrs)
           (into {::points (mapv second children)})))}]])
```

This component won't be much to look at without some help. It's just an empty
`div` with whatever attributes you pass it. Using a script tag to pass the data
ensures it won't be visible on screen.

--------------------------------------------------------------------------------
:block/title Server rendering
:block/level 2
:block/id server-rendering
:block/markdown

To render the map server-side we need a server. To serve the page we can use the
`index.html` file we already serve the client with as a template:

```clj
(ns atlas.server
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [replicant.string :as r]
            ,,,))

(def template (slurp (io/resource "public/index.html")))
```

This file contains a placeholder div for the client to render in. Since we won't
be needing that for the server-rendered pages, we can instead use it as a
literal placeholder for the HTML string Replicant produces:

```clj
(defn serve-page [hiccup]
  {:status 200
   :headers {"content-type" "text/html"}
   :body
   (->> (r/render hiccup)
        (str/replace template #"<div id=\"app\"></div>"))})
```

The Ring handler will do some very crude routing:

```clj
(defn render-city-page [city-id]
  [:h1 "Hello, " city-id])

(defn handler [{:keys [uri]}]
  (cond
    (= "/" uri)
    (response/resource-response "/index.html" {:root "public"})

    (str/starts-with? uri "/city")
    (serve-page (render-city-page (str/replace uri #"^/city/" "")))

    :else
    {:status 404
     :headers {"content-type" "text/html"}
     :body "<h1>Page not found</h1>"}))
```

The rest of the server isn't all that interesting, but you can find [the details
on
Github](https://github.com/cjohansen/replicant-maps/tree/main/src/atlas/server.clj).

The whole point of this exercise was to render the same UI on the server as we
did client-side. So how can we do that? Well, now that we have a
`::map/marker-map` alias implementation available for Clojure as well, we can
simply call the same rendering function:

```clj
(ns atlas.server
  (:require [atlas.data :as data]
            [atlas.ui :as ui]
            ,,,))

(defn render-city-page [city-id]
  (ui/render-page
   {:city (first (filter (comp #{city-id} :id) data/cities))
    :cities data/cities}))
```

The only caveat with this approach is the `button` used to navigate cities. The
server-rendered version won't have click event handlers, so it needs regular
links. There's no reason why we can't do both:

```clj
(defn render-page [{:keys [city cities]}]
  (let [{:keys [name position zoom points]} city]
    [:main.m-4
     ,,,
     [:ul
      (for [city cities]
        (if (= name (:name city))
          [:li (:name city)]
          [:li
           [:a.link
            {:href (str "/city/" (:id city))
             :on {:click [[:store/assoc-in [:city] city]]}}
            (:name city)]]))]]))
```

This works on the server, but only almost works on the client. The problem is
that clicking the links will go to the URL in the `:href`, not perform our click
actions. To fix this for the client we need to call `.preventDefault` on the
event object in the click handler.

The central event dispatch function has access to the event object, so we can
add an action that does the preventing of the default for us:

```clj
(defn execute-actions [store e actions]
  (doseq [[action & args] actions]
    (case action
      :store/assoc-in (apply swap! store assoc-in args)
      :event/prevent-default (.preventDefault e) ;; <==
      (println "Unknown action" action "with arguments" args))))
```

And then we can have links that work as intended when rendered both from the
client and from the server:

```clj
(defn render-page [{:keys [city cities]}]
  (let [{:keys [name position zoom points]} city]
    [:main.m-4
     ,,,
     [:ul
      (for [city cities]
        (if (= name (:name city))
          [:li (:name city)]
          [:li
           [:a.link
            {:href (str "/city/" (:id city))
             :on {:click [[:store/assoc-in [:city] city]
                          [:event/prevent-default]]}}
            (:name city)]]))]]))
```

The final caveat is that the server-rendered page loads the ClojureScript
bundle, which now fails because there is no `<div id="app">` on it. Fixing that
is easy:

```clj
(ns atlas.dev
  ,,,)

,,,

(defn ^:dev/after-load main []
  (when el
    (app/main store el)))
```

--------------------------------------------------------------------------------
:block/title Loading the map
:block/level 2
:block/id loading-map
:block/markdown

To bring the map to life, we will employ a trusty old technique known as
[progressive
enhancement](https://developer.mozilla.org/en-US/docs/Glossary/Progressive_Enhancement).
The server renders the map with a data attribute. We can look for any element
with a `data-client-feature` attribute and use the value to decide what to do:

```clj
(ns atlas.progressive-enhancement)

(defn revive-map [el]
  ,,,)

(defn main []
  (doseq [el (js/document.querySelectorAll "[data-client-feature]")]
    (case (.getAttribute el "data-client-feature")
      "marker-map"
      (revive-map el))))
```

Now, how do we revive the map? First of all, the div contains a script tag with
the necessary EDN data in it, so we can start by reading that:

```clj
(ns atlas.progressive-enhancement
  (:require [cljs.reader :as reader]))

(defn revive-map [el]
  (let [data (->> (.querySelector el "script")
                  .-innerText
                  reader/read-string)]
    ))
```

It just so happens that we already have a `mount-map` function that takes a DOM
node and some map data, and loads and renders Mapbox in the div. So let's just
call that:

```clj
(ns atlas.progressive-enhancement
  (:require [atlas.ui.map :as map]
            [cljs.reader :as reader]))

(defn revive-map [el]
  (let [data (->> (.querySelector el "script")
                  .-innerText
                  reader/read-string)]
    (map/mount-map el data)))
```

And that's it! Except for a warning from Mapbox about the containing element not
being empty. We can clear that by emptying the `div` after loading the data:

```clj
(defn revive-map [el]
  (let [data (->> (.querySelector el "script")
                  .-innerText
                  reader/read-string)]
    (set! (.-innerHTML el) "")
    (map/mount-map el data)))
```

The server-rendered page now renders the highly interactive map.

--------------------------------------------------------------------------------
:block/title Conclusion
:block/level 2
:block/id conclusion
:block/markdown

With this effort we achieved some interesting things

1. We can render the exact same UI on the server or on the client.
2. The server-rendered page can still use interactive components with some
   progressive enhancement.
3. Representing the UI with data makes it easy to branch off parts of it between
   Clojure and ClojureScript.
4. The progressive enhancement version uses the exact same code as the "SPA"
   version.

Why does this matter? Being able to switch between rendering on the server and
on the client is a huge benefit. There's no reason to render articles and other
static content in a Single Page Application just because you want it to contain
some interactive elements.

By providing both a string renderer and a DOM reconciler, Replicant gives you
the option to render on the server or client, and it's all data and
straight-forward Clojure and/or ClojureScript.

[The full code-listing is on
Github](https://github.com/cjohansen/replicant-maps).
