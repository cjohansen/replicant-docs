--------------------------------------------------------------------------------
:page/uri /tutorials/interop-alias/
:page/title Wrapping a library in an alias
:page/kind :page.kind/tutorial
:page/category :tutorial.category/interop
:page/order 71

--------------------------------------------------------------------------------
:block/markdown

In this second tutorial on integrating third party JavaScript libraries with
Replicant, we'll add some features to the Mapbox component we built in the
[introductory tutorial](/tutorials/javascript-interop/) and build an
[alias](/alias/) around it so maps can be expressed with data, like everything
else.

--------------------------------------------------------------------------------
:block/title Setup
:block/level 2
:block/id setup
:block/markdown

The setup for this tutorial is the result of the first interop tutorial, which
in turn is based on [the state management with an atom
tutorial](/tutorials/state-atom/). If you want to follow along, grab [the setup
on Github](https://github.com/cjohansen/replicant-maps/tree/interop), and follow
the README to get running.

--------------------------------------------------------------------------------
:block/title The task
:block/level 2
:block/id task
:block/markdown

In this tutorial we will add the ability to render markers in the map. We will
then wrap the component in a Replicant alias for a nice data-driven entry point.
But first we will simplify using the map component.

--------------------------------------------------------------------------------
:block/title Loading Mapbox on demand
:block/level 2
:block/id loading
:block/markdown

Using the map component currently requires manually loading the Mapbox CSS and
JavaScript files, and setting the API token. This means we're loading those even
on pages that don't use maps, and the map component relies on manual setup
elsewhere. We can fix both by having the on-mount hook load Mapbox on first use.

To load the CSS file we construct a `link` element and add it to the document's
head:

```clj
(let [link (.createElement js/document "link")]
  (set! (.-rel link) "stylesheet")
  (set! (.-type link) "text/css")
  (set! (.-href link) "https://api.mapbox.com/mapbox-gl-js/v2.14.1/mapbox-gl.css")
  (.appendChild js/document.head link))
```

To load the script we construct a `script` element and add it to the document as
well:

```clj
(let [script (.createElement js/document "script")]
    (set! (.-src script) "https://api.mapbox.com/mapbox-gl-js/v2.14.1/mapbox-gl.js")
  (.appendChild js/document.head link))
```

To set the API token, we need the script to have loaded. This happens
asynchronously, so we'll have to add an event listener that triggers when the
script is available. This also applies to any other use of the `mapboxgl`
object, so our loading function should return a promise that resolves when
Mapbox is ready to use.

Since we only want to load the script once, we'll check if Mapbox is available
before loading it. Here's the full loading function

```clj
(defn load-mapbox [^js el api-token]
  (js/Promise.
   (fn [res]
     (if js/window.mapboxgl
       (res)
       (let [link (.createElement el.ownerDocument "link")
             script (.createElement el.ownerDocument "script")]
         (.addEventListener script "load"
          (fn [_]
            (set! el.ownerDocument.defaultView.mapboxgl.accessToken api-token)
            (res))
          #js {:once true})
         (set! (.-rel link) "stylesheet")
         (set! (.-type link) "text/css")
         (set! (.-href link) "https://api.mapbox.com/mapbox-gl-js/v2.14.1/mapbox-gl.css")
         (set! (.-src script) "https://api.mapbox.com/mapbox-gl-js/v2.14.1/mapbox-gl.js")
         (.appendChild el.ownerDocument.head link)
         (.appendChild el.ownerDocument.head script))))))
```

This function uses `el.ownerDocument` in place of `js/document`. This helps the
component work across iframes, which is useful if you want to showcase the map
in Portfolio.

We can now update `mount-map` to use this function:

```clj
(def ^:dynamic *mapbox-api-token* nil)

(defn mount-map [^js node {:keys [center zoom]}]
  (-> (load-mapbox node *mapbox-api-token*)
      (.then
       #(js/mapboxgl.Map.
         (clj->js
          {:container node
           :style "mapbox://styles/mapbox/streets-v12"
           :center center
           :zoom zoom})))))
```

We can set the API token in the dev namespace:

```clj
(ns atlas.dev
  (:require [atlas.core :as app]
            [atlas.data :as data]
            [atlas.ui.map :as map]))

(set! map/*mapbox-api-token* "pk.eyJ...")
```

We can now remove the corresponding `link` and `script` elements from the
`index.html` file.

--------------------------------------------------------------------------------
:block/title Rendering markers
:block/level 2
:block/id markers
:block/markdown

Let's build out the map component by adding some markers. Adding markers to a
map consists of the following steps:

1. Load a png marker image
2. Add a data source with marker data
3. Add a layer that displays markers

There's a lot more you can do, but we're not really here to learn Mapbox, so
we'll settle for a few markers.

### Loading markers

To avoid pixelated markers, the marker image should be twice the intended size
and loaded with double the pixel density. We'll use this image:

![A map marker](/images/map-marker.png)

And here's how to load it:

```clj
(defn load-marker [^js map id url]
  (js/Promise.
   (fn [resolve reject]
     (.loadImage map url
      (fn [error image]
        (.addImage map id image #js {:pixelRatio 2})
        (if error
          (reject error)
          (resolve id)))))))
```

The next step is to add a data source. Mapbox expects a data structure like
this:

```clj
{:type "FeatureCollection"
 :features
 [{:type "Feature"
   :geometry {:type "Point"
              :coordinates [-122.00004 37.571414]}
   :properties {:id ",,,"
                ,,,}}
  ,,,]}
```

You can put arbitrary data under `:properties` and use them in layers. It would
be nice if we could create points with a slightly more compact data structure,
so we'll create the Mapbox representation from the following data:

```clj
(def cities
  [{:id "san-francisco"
    :name "San Francisco"
    :position [-122.475238, 37.807962]
    :zoom 11
    :points
    [{:point/label "Bulbasaur"
      :point/latitude 37.807962
      :point/longitude -122.475238}
     {:point/label "Charmander"
      :point/latitude 34.062759
      :point/longitude -118.35718}
     {:point/label "Squirtle"
      :point/latitude 37.805929
      :point/longitude -122.429582}
     {:point/label "Magnemite"
      :point/latitude 37.8269775
      :point/longitude -122.425144}
     {:point/label "Magmar"
      :point/latitude 37.571414
      :point/longitude -122.00004}]}
   ,,,])
```

The mapping is straight-forward:

```clj
(defn points->feature-collection [points]
  {:type "FeatureCollection"
   :features
   (mapv
    (fn [{:point/keys [label longitude latitude]}]
      {:type "Feature"
       :geometry {:type "Point"
                  :coordinates [longitude latitude]}
       :properties {:id label
                    :label label}})
    points)})
```

Next we'll add a function to "configure" the map: load the marker image, add the
data source, and render a layer with our markers.

```clj
(defn configure-map [^js map {:keys [points]}]
  (-> (load-marker map "blue-marker" "/map-marker.png")
      (.then
       (fn [_]
         (.addSource
          map "points"
          (clj->js
           {:type "geojson"
            :data (points->feature-collection points)}))

         (.addLayer map 
                    (clj->js
                     {:id "points"
                      :type "symbol"
                      :source "points"
                      :layout {:icon-image "blue-marker"
                               :icon-allow-overlap true
                               :text-field '[get label]
                               :text-font ["Open Sans Semibold"]
                               :text-offset [0 0.5]
                               :text-allow-overlap true
                               :text-anchor "top"}}))

         map))))
```

We will call this function from `mount-map`, after we have made sure the map is
fully loaded, otherwise we can run into trouble with the map marker:

```clj
(defn mount-map [^js node {:keys [center zoom] :as data}]
  (-> (load-mapbox node *mapbox-api-token*)
      (.then
       (fn []
         (let [map (js/mapboxgl.Map.
                    (clj->js
                     {:container node
                      :style "mapbox://styles/mapbox/streets-v12"
                      :center center
                      :zoom zoom}))]
           (set! (.-map node) map)
           (js/Promise.
            (fn [res]
              (.on map "load" #(res map)))))))
      (.then #(configure-map % data))))
```

The very final piece of the puzzle is to pass the points to the map when we use
it:

```clj
(defn render-page [{:keys [city cities]}]
  (let [{:keys [name position zoom points]} city]
    [:main.m-4
     (render-title name)
     (when position
       (map/render-map
        {:center position
         :zoom (or zoom 11)
         :points points})) ;; <==
     [:h2.text-lg.mb-2 "Choose city"]
     ,,,]))
```

![Map markers indicating Pokémons in San Francisco](/images/san-francisco-pokemons.png)

### Point data updates

Let's add some more Pokémons to our data set:

```clj
(def cities
  [{:id "san-francisco"
    :name "San Francisco"
    :position [-122.475238, 37.807962]
    :zoom 11
    :points
    [{:point/label "Bulbasaur"
      :point/latitude 37.807962
      :point/longitude -122.475238}
     ,,,]}
   {:id "london"
    :name "London"
    :position [-0.1276, 51.5072]
    :zoom 12
    :points
    [{:point/label "Pikachu"
      :point/latitude 51.5081
      :point/longitude -0.1281}
     {:point/label "Eevee"
      :point/latitude 51.5074
      :point/longitude -0.1657}
     {:point/label "Snorlax"
      :point/latitude 51.5081
      :point/longitude -0.0759}
     {:point/label "Gengar"
      :point/latitude 51.5663
      :point/longitude -0.1464}
     {:point/label "Lapras"
      :point/latitude 51.5055
      :point/longitude -0.0754}]}
   ,,,])
```

This works reasonably well: You can refresh the page and select San Francisco
and see Pokémons. You can then refresh and click London and see other Pokémons.
However, if you first click one city, then navigate to the other -- there will
be no Pokémons in the second city.

The reason this happens is that we're not dealing with changes to `:points`. In
our particular example there are few enough points that you could throw all of
them at Mapbox at once and have it figure things out. But that doesn't teach us
anything about integrating a third party JavaScript library with Replicant.

We already have an `update-map` function. To refresh the dataset on update, we
just need to call `.setData` on the Mapbox feature set:

```clj
(defn update-map [^js map {:keys [center zoom points]}]
  (.setZoom map zoom)
  (.panTo map (clj->js center))
  (.setData (.getSource map "points") ;; <==
            (clj->js (points->feature-collection points))))
```

And with that, we always see the relevant points.

--------------------------------------------------------------------------------
:block/title Adding an alias
:block/level 2
:block/id alias
:block/markdown

With all the necessary features implemented, let's make an [alias](/alias/) for
a nice data-driven interface to the map.

If we think of the markers as the content of the map, we might want to include
these as children of the map. The children _can_ be maps (as in Clojure maps),
but Replicant will always treat the first map child of a hiccup node as the
attribute map, so using maps for children can be a bit of a footgun. Instead,
we'll use hiccup-like nodes for the markers:

```clj
(require '[atlas.ui.map :as map])

[map/marker-map {::map/center position
                   ::map/zoom zoom}
 [::map/marker
  {:point/label "Pikachu"
   :point/latitude 51.5081
   :point/longitude -0.1281}]
 [::map/marker
  {:point/label "Eevee"
   :point/latitude 51.5074
   :point/longitude -0.1657}]]
```

A few things to note:

1. Parameters to the map are passed as namespaced keys in the attribute map.
   Replicant does not treat namespaced keys as attributes, so this way we can
   also pass attributes to the wrapping element without the map component having
   to know about specific attributes.
2. `::map/marker` does not need to be an alias for this to work: the map alias
   can simply unwrap the attribute maps and use them for the points data.
3. Replicant will "normalize" the hiccup before invoking the alias function, so
   this will work even if there are `nil`s between markers, or some markers are
   nested in a list, etc.

To implement the alias we will slightly adjust our previous `render-map`
function like so:

```clj
(defalias marker-map [attrs children]
  (let [attrs (assoc attrs ::points (mapv second children))]
    [:div.aspect-video
     (-> attrs
         (assoc :replicant/on-mount
                (fn [{:replicant/keys [node remember]}]
                  (-> (mount-map node attrs)
                      (.then #(remember %)))))
         (assoc :replicant/on-update
                #(update-map (:replicant/memory %) attrs)))]))
```

Using it looks like this:

```clj
(defn render-page [{:keys [city cities]}]
  (let [{:keys [name position zoom points]} city]
    [:main.m-4
     (render-title name)
     (when position
       [map/marker-map
        {:class "mb-4" ;; <==
         ::map/center position
         ::map/zoom (or zoom 11)}
        (for [point points]
          [::map/marker point])])
     [:h2.text-lg.mb-2 "Choose city"]
     [:ul
      (for [city cities]
        (if (= name (:name city))
          [:li (:name city)]
          [:li.link
           {:on {:click [[:store/assoc-in [:city] city]]}}
           (:name city)]))]]))
```

Note how we kept the page-specific margin-bottom class in the page.

--------------------------------------------------------------------------------
:block/title Closing words
:block/level 2
:block/id summary
:block/markdown

In this tutorial we developed a data-driven interface to Mapbox for use in our
application. Let's review some of the benefits of this approach:

1. No imperative code or inline functions necessary to draw a map with some
   markers on screen.
2. A data representation for the map, meaning you can still write tests for your
   UI that make sure there is a map with the appropriate markers, without
   getting lost in Mapbox details.
3. A level of indirection that can help this component move seamlessly to the
   server.

This last point will be the focus of the [third and final installment of this
tutorial](/tutorials/server-alias/), where we'll make sure this alias can also
be used on server-rendered pages.

It's also worth noting that the 9 line alias implementation is the only code
specific to Replicant: All the rest is just ClojureScript. You don't need "X for
Replicant", all you need is a thin wrapper.

As with all good things, this approach also has its limitations.

The Mapbox API is enourmous, and wrapping all of it is probably not feasible.
But that doesn't mean you can't make a bunch of small aliases that cater to your
specific uses.

The Mapbox API is capable of efficiently hold a lot of data. The approach
demonstrated in this tutorial requires us to convert ClojureScript data between
two representations and convert the result to JavaScript before we feed it to
Mapbox. This only works with so many markers.

If you want to display thousands of markers you probably want to take a more
direct route and use the Mapbox tooling to filter visibility etc. Even if you
end up with this use case, you could still use the same basic approach, but
instead of passing explicit markers maybe you'd pass a URL to a JSON document or
some such. Use your imagination and adjust to your context.

As always [the full code-listing is on
Github](https://github.com/cjohansen/replicant-maps/tree/alias). 
