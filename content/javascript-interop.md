--------------------------------------------------------------------------------
:page/uri /tutorials/javascript-interop/
:page/title Using a JavaScript library
:page/kind :page.kind/tutorial
:page/category :tutorial.category/interop
:page/order 70

--------------------------------------------------------------------------------
:block/markdown

We can't always make everything ourselves, and so occasionally we'll want to use
a third-party JavaScript library for parts of the user interface. While
Replicant strives to keep your UI pure, simple and data-driven, it acknowledges
this fact and provides [life cycle-hooks](/life-cycle-hooks/) that give you
access to DOM nodes.

In this three-part tutorial we will walk through a practical demonstration of
using life-cycle hooks to integrate a third party JavaScript library by
rendering a [Mapbox](https://www.mapbox.com/) map in a Replicant UI. In this
first tutorial, we will handle the basics and get a map on screen.
[Next](/tutorials/interop-alias/) we'll use [aliases](/alias/) to build a
declarative map component, and in the final installment we'll see how to expand
the declarative component so it can work both on the server and on the client.

--------------------------------------------------------------------------------
:block/title Setup
:block/level 2
:block/id setup
:block/markdown

The setup for this tutorial is based on [the state management with an atom
tutorial](/tutorials/state-atom/). If you want to follow along, grab [the setup
on Github](https://github.com/cjohansen/replicant-maps/tree/setup), and follow
the README to get running.

--------------------------------------------------------------------------------
:block/title Reconnaissance
:block/level 2
:block/id reconnaissance
:block/markdown

When integrating third-party libraries like Mapbox, it's a good idea to start by
"gathering the pieces". I do this in the following way:

1. Get a minimal example working with HTML and JavaScript
2. Translate the minimal example to ClojureScript
3. Integrate the example into the code-base

This process minimizes the number of unknowns in each step and makes it a lot
easier to debug any problems that might occur along the way. There's no joy to
be found in debugging the details in your Replicant/Mapbox integration only to
find that you've misunderstood how the Mapbox API works.

--------------------------------------------------------------------------------
:block/title Minimal JavaScript example
:block/level 2
:block/id javascript
:block/markdown

To use [Mapbox](https://www.mapbox.com/) you will need to sign up for an account
and get an API token -- doing so is quick and free. With the token we can set up
an HTML page that loads the Mapbox CSS and JavaScript, and sets the token:

```html
<!DOCTYPE html>
<html>
  <head>
    <title>Mapbox JavaScript example</title>
    <link rel="stylesheet" type="text/css" href="https://api.mapbox.com/mapbox-gl-js/v2.14.1/mapbox-gl.css">
  </head>
  <body>
    <div id="app"></div>
    <script src="https://api.mapbox.com/mapbox-gl-js/v2.14.1/mapbox-gl.js"></script>
    <script type="text/javascript">
      mapboxgl.accessToken = "...";

      // Our test script goes here
    </script>
  </body>
</html>
```

Next up we'll try to load a map with a center point and a "city view" zoom
level:

```js
var el = document.getElementById("app");

var map = new mapboxgl.Map({
  container: el,
  style: "mapbox://styles/mapbox/streets-v12",
  center: [-122.475238, 37.807962],
  zoom: 11
});
```

Unfortunately, not much happens. There are no errors, so what's going on?
Peering around the inspector reveals that the `div` has no dimensions. We
obviously need to stretch it out ourselves. Let's try again:

```clj
var el = document.getElementById("app");
el.style.width = "800px";
el.style.height = "450px";

var map = new mapboxgl.Map({
  container: el,
  style: "mapbox://styles/mapbox/streets-v12",
  center: [-122.475238, 37.807962],
  zoom: 11
});
```

There we go! A nice little map of San Francisco.

![A map over parts of San Francisco](/images/san-francisco.png)

--------------------------------------------------------------------------------
:block/title Minimal ClojureScript example
:block/level 2
:block/id clojurescript
:block/markdown

The next step is to translate the (now) working JavaScript example to
ClojureScript. For this to work, the HTML file needs to load the Mapbox CSS and
JavaScript, and set the API token, but not render the map:

```html
<!DOCTYPE html>
<html>
  <head>
    <title>Replicant Maps</title>
    <link rel="stylesheet" type="text/css" href="/tailwind.css">
    <link rel="stylesheet" type="text/css" href="https://api.mapbox.com/mapbox-gl-js/v2.14.1/mapbox-gl.css">
  </head>
  <body>
    <div id="app"></div>
    <script src="https://api.mapbox.com/mapbox-gl-js/v2.14.1/mapbox-gl.js"></script>
    <script type="text/javascript">mapboxgl.accessToken = "pk.eyJ1I...";</script>
    <script src="/js/main.js"></script>
  </body>
</html>
```

Translating the code is pretty straight-forward:

```clj
(ns atlas.dev)

(defonce el (js/document.getElementById "app"))

(defn ^:dev/after-load main []
  (set! (.. el -style -width) "800px")
  (set! (.. el -style -height) "450px")

  (js/mapboxgl.Map.
   (clj->js
    {:container el
     :style "mapbox://styles/mapbox/streets-v12"
     :center [-122.475238 37.807962]
     :zoom 11})))
```

Performing these two extra steps doesn't take a lot of time, but it can save you
a lot of frustration in the case of mistakes. Had we not first made the
JavaScript version, I would've immediately suspected some interop problem when
the map wouldn't render, instead of the missing `div` dimensions that was the
real culprit.

--------------------------------------------------------------------------------
:block/title Integrating with Replicant
:block/level 2
:block/id replicant
:block/markdown

Now that we have a working ClojureScript version, we can move on to making
Replicant render the map for us.

Let's create the div with the correct dimensions first:

:block/a-lang :clj
:block/a-title src/atlas/ui.cljc
:block/a-code

(ns atlas.ui
  (:require [atlas.ui.map :as map]))

(defn render-page [state]
  [:main.m-4
   [:h1.text-xl.mb-2
    "Hello "
    [:span.line-through
     "world"]
    " San Francisco!"]
   (map/render-map)])

:block/b-lang :clj
:block/b-title src/atlas/ui/map.cljs
:block/b-code

(ns atlas.ui.map)

(defn render-map []
  [:div.aspect-video])

--------------------------------------------------------------------------------
:block/markdown

Note that `atlas.ui` is in a cljc file, and `atlas.ui.map` is in a cljs file.
Ideally we want to keep all the UI stuff in cljc files, so we can use them both
on the backend and frontend -- and so we know they don't contain heaps of
imperative JavaScript/browser-specific code. But there are exceptions, and a
Mapbox integration certainly is one of them.

Instead of hard-coding the dimensions, `.aspect-video` sets the element's aspect
ratio to a pleasing 16:9. You can still override it by setting width and height
for specific use cases.

Next up, we will add a [life-cycle hook](/life-cycle-hooks/) for the on mount
hook. It is a function that is called when the element is mounted to the DOM,
and it gives us access to the actual DOM node. With it, we can mount the map:

```clj
(defn mount-map [node]
  (js/mapboxgl.Map.
   (clj->js
    {:container node
     :style "mapbox://styles/mapbox/streets-v12"
     :center [-122.475238 37.807962]
     :zoom 11})))

(defn render-map []
  [:div.aspect-video
   {:replicant/on-mount #(mount-map (:replicant/node %))}])
```

And that's it! The map now renders through Replicant.

--------------------------------------------------------------------------------
:block/title Parameterizing the map
:block/level 2
:block/id parameterizing
:block/markdown

So far we're rendering a static map on mount. But we should be able to control
the details of the map with data -- and have the map update when data changes.

Let's start by parameterizing the map function:

```clj
(defn mount-map [node {:keys [center zoom]}]
  (js/mapboxgl.Map.
   (clj->js
    {:container node
     :style "mapbox://styles/mapbox/streets-v12"
     :center center
     :zoom zoom})))

(defn render-map [data]
  [:div.aspect-video.mb-4
   {:replicant/on-mount #(mount-map (:replicant/node %) data)}])
```

Now we can pass in the center and zoom. To test it out we will initialize the
application state with a list of cities:

:block/a-title src/atlas/data.cljc
:block/a-lang :clj
:block/a-code

(ns atlas.dev
  (:require [atlas.core :as app]
            [atlas.data :as data]))

(defonce store
  (atom {:cities data/cities}))

(defonce el (js/document.getElementById "app"))

(defn ^:dev/after-load main []
  ;; Add additional dev-time tooling here
  (app/main store el))

:block/b-lang :clj
:block/b-title src/atlas/data.cljc
:block/b-code

(ns atlas.data)

(def cities
  [{:id "san-francisco"
    :name "San Francisco"
    :position [-122.475238, 37.807962]
    :zoom 11}])

--------------------------------------------------------------------------------
:block/markdown

We then update the page rendering function to display a map when a city is
selected, and allow the user to select from a list of cities below the map:

```clj
(defn render-title [city-name]
  [:h1.text-xl.mb-2
   "Hello "
   [:span {:class (when city-name "line-through")}
    "world"]
   (when city-name
     (str " " city-name))
   "!"])

(defn render-page [{:keys [city cities]}]
  (let [{:keys [name position zoom]} city]
    [:main.m-4
     (render-title name)
     (when position
       (map/render-map
        {:center position
         :zoom (or zoom 11)}))
     [:h2.text-lg.mb-2 "Choose city"]
     [:ul
      (for [city cities]
        (if (= name (:name city))
          [:li (:name city)]
          [:li
           [:button.link
            {:on {:click [[:store/assoc-in [:city] city]]}}
            (:name city)]]))]]))
```

This will initially render the page without a map with a "list" (very short one)
of cities to choose from:

![The initial page with only one city to click](/images/hello-world.png)

Clicking the image will cause the map element to mount, which triggers the
life-cycle event, which in turn mounts the map:

![The page with a map of San Francisco loaded](/images/hello-san-francisco.png)

--------------------------------------------------------------------------------
:block/title Updating the rendered map
:block/level 2
:block/id updates
:block/markdown

The final step is to be able to update the map. We'll start with a heavy-handed
approach, and then refine it. Both methods are useful tools to have in your
toolkit.

Let's add some more cities to choose from:

```clj
(def cities
  [{:id "san-francisco"
    :name "San Francisco"
    :position [-122.475238, 37.807962]
    :zoom 11}
   {:id "london"
    :name "London"
    :position [-0.1276, 51.5072]
    :zoom 12}
   {:id "tokyo"
    :name "Tokyo"
    :position [139.6917, 35.6895]
    :zoom 12}
   {:id "cape-town"
    :name "Cape Town"
    :position [18.4241, -33.9249]
    :zoom 11}])
```

When you select the first city the map will mount. Clicking another city,
however, is a disappointing experience: nothing happens. The map was already
mounted, so nothing happens.

The heavy handed solution to this problem is to key the map on the selected
city. This will force Replicant to discard the element and mount a new one when
we change cities. Thus the map will be recreated with the correct position when
the new element mounts:

```clj
(defn render-map [data]
  [:div.aspect-video.mb-4
   {:replicant/key data
    :replicant/on-mount #(mount-map (:replicant/node %) data)}])
```

While this works, it's not particularly elegant, and can cause a jagged user
experience. A lighter touch is to use `:replicant/on-update` to adjust the
rendered map when data changes.

In order to update the map we will need a reference to the Mapbox instance.
Replicant doesn't have component-local state, but it does have "memory" between
life-cycle hooks to support this common use case is so common:

```clj
(defn render-map [data]
  [:div.aspect-video.mb-4
   {:replicant/key data
    :replicant/on-mount
    (fn [{:replicant/keys [node remember]}]
      (remember (mount-map node data)))}])
```

Whatever you pass to the `:replicant/remember` function will be available in
later life-cycle hooks as `:replicant/memory`. We can use this in the update
hook:

```clj
(defn update-map [^js map {:keys [center zoom]}]
  (.setZoom map zoom)
  (.panTo map (clj->js center)))

(defn render-map [data]
  [:div.aspect-video.mb-4
   {:replicant/on-mount
    (fn [{:replicant/keys [node remember]}]
      (remember (mount-map node data)))

    :replicant/on-update
    #(update-map (:replicant/memory %) data)}])
```

Note the `^js` type hint. This lets the ClojureScript compiler know that `map`
is a JavaScript object, and that it shouldn't munge the property names when
compiling with advanced optimizations.

Now we can reuse the same map instance, but have it update its position and zoom
when the user selects a city. If you ever need to access the map instance
outside of an event handler, you can do so by passing the DOM node to
`(replicant.dom/recall node)`, which returns whatever is passed as
`:replicant/memory` to your life-cycle hooks for the same `node`.

If you prefer a more ludicrous panning motion, you could try this:

```clj
(defn update-map [^js map {:keys [center zoom]}]
  (-> map
      (.flyTo
       (clj->js
        {:zoom zoom
         :center center}))))
```

In this tutorial we saw how to integrate a third party JavaScript library with
Replicant, and control its use with data from our data-driven rendering
functions. In [the next installment](/tutorials/interop-alias/) we'll see how to
package such integrations as fully data-driven aliases.

The full code listing is [available on
Github](https://github.com/cjohansen/replicant-maps/tree/interop).
