:page/uri /tutorials/routing/
:page/title Data-driven routing for Replicant
:page/body

In this tutorial we will develop a small routing system. You will learn how
routing can fit into Replicant's model where rendering is always top-down. We'll
also explore using the URL as an alternative to component local state.

With the exception of the final section, the ideas in this tutorial are not
specific to Replicant, and can be used with a wide variety of rendering
libraries.

## Example setup

For this tutorial, we will build a small app to view [Parens of the
dead](https://www.parens-of-the-dead.com/) episodes. You can [get the setup on
Github](https://github.com/cjohansen/replicant-routing/tree/initial-setup) if
you want to follow along.

The `parens.data` namespace has some data for us to work with:

```clj
(ns parens.data)

(def data
  {:videos
   [{:video/url "https://www.youtube.com/watch?v=6qnNtVdf08Q"
     :video/thumbnail "/images/parens1.png"
     :episode/id "s2e1"
     :episode/number 1
     :episode/title "It Lives Again"
     :episode/description "Starting with an empty folder sure is a choice. Much like digging your way out of a grave, it can be intimidating, perhaps because it's not something you do every day. Watch us struggle with these issues and more in the very first episode of Parens of the Dead."}
    {:video/url "https://www.youtube.com/watch?v=CyveUnHzc7g"
     :video/thumbnail "/images/parens2.png"
     :episode/id "s2e2"
     :episode/number 2
     :episode/title "Shambling Along"
     :episode/description "Watch us totally ruin a pristine, beautiful lightly tinted purple page by adding decrepit buildings to it in this very second episode of Parens of the Dead. We'll get ClojureScript up and running with surprisingly little hassle. There might even be jokes."}
    {:video/url "https://www.youtube.com/watch?v=_6tVIijfRzQ"
     :video/thumbnail "/images/parens3.png"
     :episode/id "s2e3"
     :episode/number 3
     :episode/title "Stumbling out of the Graveyard"
     :episode/description "Someone eated our brains! In this very third episode, we're struggling to get more wiring wired properly while also typing with our fingers on the keyboard. It's a hard life, being a zombie developer. Web socket setup surely is no joke."}]})
```

The `parens.ui` namespace renders a list of video titles:

```clj
(ns parens.ui)

(defn render-page [{:keys [videos]}]
  [:div
   [:h1 "Parens of the dead"]
   [:ul
    (for [{:keys [episode/title]} videos]
      [:li title])]])
```

The `parens.core` namespace is where we call `replicant.dom/render`, and where
we'll add additional central infrastructure:

```clj
(ns parens.core
  (:require [parens.ui :as ui]
            [replicant.dom :as r]))

(defn main [el state]
  (r/render el (ui/render-page state)))

(defn bootup [el state]
  ;; Perform bootup steps that should only be done once here
  (main el state))
```

Finally, there's a dev namespace to kick things off:

```clj
(ns parens.dev
  (:require [parens.core :as app]
            [parens.data :as data]))

(defonce el (js/document.getElementById "app"))
(defonce started (app/bootup el data/data))

(defn ^:dev/after-load main []
  ;; Add additional dev-time tooling here
  (app/main el data/data))
```

## System design

By adding routing to the app we want to have different render functions at
different URLs. To achieve this, we will extract information from the URL and
use it to dispatch to the right render function.

When someone visits the path `"/"`, we want to show them the frontpage. When
they visit `"/episodes/s2e1"`, we want to show information about the episode
with id `"s2e1"`. In other words, every path like `"/episodes/??"` represents
the same page/render function, but with different parameters.

We will use the term "page" to mean the render function for a specific route.
The episode page has many concrete instances: `/episodes/s2e1`,
`/episodes/s2e2`, etc. Routing allows us to break down the URL to a data
structure describing a specific instance of a page. We will use the term
"location" for this data structure.

Given this route description:

```clj
{:location/page-id :pages/episode
 :location/route ["episodes" :episode/id]}
```

The URL `/episodes/s2e1` will be represented by this location:

```clj
{:location/page-id :pages/episode
 :location/params {:episode/id "s2e1"}}
```

The location map can also hold information from the query string and hash if we
need it:

```clj
{:location/page-id :pages/episode
 :location/params {:episode/id "s2e1"}
 :location/query-params {:view "related"}
 :location/hash-params {:menu-expanded "1"}}
```

We'll start with a bareboned version of the routing function:

```clj
(defn extract-location [path]
  (or (when (= "/" path)
        {:location/page-id :pages/frontpage})
      (when-let [[_ id] (re-find #"/episodes/(\w+)" path)]
        {:location/page-id :pages/episode
         :location/params {:episode/id id}})))
```

## Working with locations

We need to extract the location data when the app boots, and when the URL changes.
To do it when the app boots we'll call `extract-location` from `main` and pass
the data to the render function:

```clj
(defn main [el state]
  (r/render el (ui/render-page state (extract-location js/location.href))))
```

### Page dispatch

We can rename the `render-page` function to `render-frontpage`, and write a new
`render-page` that chooses the right render function:

```clj
(ns parens.ui)

(defn render-frontpage [{:keys [videos]} _]
  [:div
   [:h1 "Parens of the dead"]
   [:ul
    (for [{:keys [episode/title]} videos]
      [:li title])]])

(defn render-not-found [_ _]
  [:h1 "Not found"])

(defn render-page [state location]
  (let [f (case (:location/page-id location)
            :pages/frontpage render-frontpage
            render-not-found)]
    (f state location)))
```

Because the `location` map may contain useful information (routing and query
parameters, etc), we pass it as a separate argument to each page's render
function.

### Browser navigation

Routing at bootup is well and fine, but we also need to change the active
location when the user navigates the app. We could add a navigation action and
use it to change the location, but an even easier approach is to piggie-back on
what the browser already does. This approach has the benefit of being less
specific to our chosen tools, and is easier to move to the server if we wish to
do so later.

We will add a click event listener to the body of the page. If the target of the
click is an anchor (or inside one), and its `href` attribute matches any of our
routes, we will route it -- otherwise, we'll let the browser do its thing.

Let's start by finding the target URL for a click event, if any:

```clj
(defn find-target-href [e]
  (some-> e .-target               ;; 1
          (.closest "a")           ;; 2
          (.getAttribute "href"))) ;; 3
```

1. The target of the event is whatever DOM element received the click
2. `.closest` returns the element itself if it matches the CSS selector, or the
   closest parent that does. This allows us to also react to clicks on elements
   nested inside anchor elements, e.g. `<a href="/videos/ac2"><img
   src="/ac2.png"></a>`.
3. Get the `href` attribute

We will call this function whenever a `click` event triggers. If the `href`
resolves to a location, we will route the click and manually update the browser
URL, otherwise we do nothing.

First, we'll refactor the `main` function slightly, to avoid duplicating details
about the render:

```clj
(defn render-location [el state location]
  (r/render el (ui/render-page state location)))

(defn main [el state]
  (render-location el state (extract-location js/location.pathname)))
```

Next, we'll add the event listener in `bootup` (we don't want this re-evaluated
when code is hot reloaded):

```clj
(defn bootup [el state]
  (js/document.body.addEventListener
   "click"
   (fn [e]
     (let [href (find-target-href e)]
       (when-let [location (extract-location href)]
         (.preventDefault e)
         (.pushState js/history nil "" href) ;; Update browser URL
         (render-location el state location)))))

  (main el state))
```

To test our new capability we will add a render function for the episode page:

```clj
(defn render-episode [state location]
  [:main
   (if-let [episode (get-episode state location)]
     (list [:h1 (:episode/title episode)]
           [:p (:episode/description episode)])
     [:h1 "Unknown episode"])
   [:p [:a {:href "/"} "Back to episode listing"]]])
```

Then include it in the main dispatch:

```clj
(defn render-page [state location]
  (let [f (case (:location/page-id location)
            :pages/frontpage render-frontpage
            :pages/episode render-episode
            render-not-found)]
    (f state location)))
```

And finally add some links from the frontpage:

```clj
(defn render-frontpage [{:keys [videos]} _]
  [:div
   [:h1 "Parens of the dead"]
   [:ul
    (for [{:keys [episode/title episode/id]} videos]
      [:li [:a {:href (str "/episodes/" id)} title]])]])
```

### Going back

We now have routing for links, but if we try to go back nothing much happens.
The URL changes, but the page doesn't re-render. We can fix this with a
[popstate](https://developer.mozilla.org/en-US/docs/Web/API/Window/popstate_event)
handler.

```clj
(defn bootup [el state]
  ,,,

  (js/window.addEventListener
   "popstate"
   (fn [_]
     (render-location el state (extract-location js/location.pathname))))

  ,,,)
```

With this handler in place, going back works as well.

## Routing mechanics

We now have basic routing in place, but with hard-coded URLs and some
duplication between the routing logic and generating links. We can fix this by
using a routing library that has two way routing. We will use
[silk](https://github.com/DomKM/silk) to demonstrate, but any library that has
bi-directional routing and uses data rather than macros for routes will do.

To avoid being overly reliant on the specific routing library, we will create
our own `router` namespace. It will also use `lambdaisland/uri` to parse the URL.

```clj
(ns parens.router
  (:require [domkm.silk :as silk]
            [lambdaisland.uri :as uri]))

(def routes
  (silk/routes
   [[:pages/episode [["episodes" :episode/id]]]
    [:pages/frontpage []]]))

(defn url->location [routes url]
  (let [uri (cond-> url (string? url) uri/uri)]
    (when-let [arrived (silk/arrive routes (:path uri))]
      (let [query-params (uri/query-map uri)
            hash-params (some-> uri :fragment uri/query-string->map)]
        (cond-> {:location/page-id (:domkm.silk/name arrived)
                 :location/params (dissoc arrived
                                          :domkm.silk/name
                                          :domkm.silk/pattern
                                          :domkm.silk/routes
                                          :domkm.silk/url)}
          (seq query-params) (assoc :location/query-params query-params)
          (seq hash-params) (assoc :location/hash-params hash-params))))))

(defn location->url [routes {:location/keys [page-id params query-params hash-params]}]
  (cond-> (silk/depart routes page-id params)
    (seq query-params)
    (str "?" (uri/map->query-string query-params))

    (seq hash-params)
    (str "#" (uri/map->query-string hash-params))))
```

We can now remove the `extract-location` we wrote earlier and call
`url->location` instead, e.g.:

```clj
(ns parens.core
  (:require [parens.router :as router]
            [parens.ui :as ui]
            [replicant.dom :as r]))

,,,

(defn main [el state]
  (->> js/location.pathname
       (router/url->location router/routes)
       (render-location el state)))
```

The route data is a global def now but we don't want to cement that, so both
routing functions expect to be passed the routes. This means that the render
functions need access to the routes as well. Update `main` like so:

```clj
(ns parens.core
  (:require [parens.router :as router]
            [parens.ui :as ui]
            [replicant.dom :as r]))

,,,

(defn main [el state]
  (->> js/location.pathname
       (router/url->location router/routes)
       (render-location el state router/routes)))
```

Pass along the routes when rendering:

```clj
(defn render-location [el state routes location]
  (r/render el (ui/render-page state routes location)))
```

Then update the render functions accordingly:

```clj
(ns parens.ui
  (:require [parens.router :as router]))

(defn render-frontpage [{:keys [videos]} routes _]
  [:div
   [:h1 "Parens of the dead"]
   [:ul
    (for [{:keys [episode/title episode/id]} videos]
      [:li
       [:a
        {:href (router/location->url routes
                 {:location/page-id :pages/episode
                  :location/params {:episode/id id}})}
        title]])]])

(defn get-episode [{:keys [videos]} {:keys [location/params]}]
  ,,,)

(defn render-episode [state routes location]
  [:main
   ,,,
   [:p
    [:a {:href (router/location->url routes {:location/page-id :pages/frontpage})}
     "Back to episode listing"]]])

(defn render-not-found [_ _ _]
  ,,,)

(defn render-page [state routes location]
  (let [f ,,,]
    (f state routes location)))
```

And with that, we have bi-directional routing powered by a routing library. And
the routing library is merely an implementation detail of the app's own router
namespace. Win-win.

## Using the URL for state transfer

URLs are great because they can address specific states in your user interface.
By making some minor adjustments to the routing system, the URL can be used in
place of component local state with the added benefit of making any state in the
UI bookmarkable and shareable like only URLs can be.

If we are to put UI state in the URL, we need to do so without adding to the
browser's history. Otherwise, we would effectively break the back button by
causing it to back through any minor state change. We can use the URL hash
fragment for this.

When routing clicks, we will check if the old and new locations are the same if
we ignore the hash params. If they are, we will use `replaceState` instead of
`pushState` to avoid adding a history entry.

We'll start with a function in the router namespace that can decide if two
locations are essentially the same:

```clj
(defn essentially-same? [l1 l2]
  (and (= (:location/page-id l1) (:location/page-id l2))
       (= (not-empty (:location/params l1))
          (not-empty (:location/params l2)))
       (= (not-empty (:location/query-params l1))
          (not-empty (:location/query-params l2)))))
```

Next, we'll extract the body click handler to a separate function. In the
`bootup` function:

```clj
(js/document.body.addEventListener "click" #(route-click % el state router/routes))
```

And here's the updated code in a separate function:

```clj
(defn get-current-location []
  (->> js/location.pathname
       (router/url->location router/routes)))

(defn route-click [e el state routes]
  (let [href (find-target-href e)]
    (when-let [location (router/url->location routes href)]
      (.preventDefault e)
      (if (router/essentially-same? location (get-current-location))
        (.replaceState js/history nil "" href)
        (.pushState js/history nil "" href))
      (render-location el state router/routes location))))
```

With this little tweak, pages can now use addressable state over the URL:

```clj
(defn render-episode [state routes location]
  (let [episode (get-episode state location)]
    [:main
     [:h1 (or (:episode/title episode)
              "Unknown episode")]
     (if (-> location :location/hash-params :description)
       (list
        [:p (:episode/description episode)]
        [:a {:href (router/location->url routes
                     (update location :location/hash-params dissoc :description))}
         "Hide description"])
       (when (:episode/description episode)
         [:a {:href (router/location->url routes
                      (assoc-in location [:location/hash-params :description] "1"))}
          "Show description"]))
     [:p
      [:a {:href (router/location->url routes {:location/page-id :pages/frontpage})}
       "Back to episode listing"]]]))
```

Using the URL for state transfer only works for small pieces of data, but for
things like toggling menus, sorting tables, etc, it works perfectly and even
improves UX by making those states addressable.

An interesting aspect about `render-episode` is that while Replicant doesn't
allow component local state, this page/"component" has all the knowledge about
the state it depends on. All it relies on is a little help from the central
infrastructure, and it can "do" whatever it needs.

## The router alias

Making links that use the router is a little bit cumbersome:

```clj
[:a {:href (router/location->url routes
             {:location/page-id :pages/frontpage})}
 "Back to the frontpage"]
```

As a final touch, we will introduce a routing [alias](/aliases/) that can
[side-chaine the route data](/aliases/#alias-data) to reduce this down to:

```clj
[:ui/a {:ui/location {:location/page-id :pages/frontpage}}
 "Back to the frontpage"]
```

We'll define the alias in the `core` namespace. This way we can do away with the
router dependency in the `ui` namespace later:

```clj
(ns parens.core
  (:require [parens.router :as router]
            [parens.ui :as ui]
            [replicant.alias :as alias]
            [replicant.dom :as r]))

(defn routing-anchor [attrs children]
  (let [routes (-> attrs :replicant/alias-data :routes)]
    (into [:a (cond-> attrs
                (:ui/location attrs)
                (assoc :href (router/location->url routes
                               (:ui/location attrs))))]
          children)))

(alias/register! :ui/a routing-anchor)

,,,
```

Next we'll update the call to Replicant's render function. We'll make two
changes: pass the routes as alias data, and stop passing the routes to the UI
elements - they no longer need to receive them explicitly.

```clj
(defn render-location [el state routes location]
  (r/render
   el
   (ui/render-page state location)
   {:alias-data {:routes routes}}))
```

With explicit routing out of the way, the UI namespace no longer depends
directly on the router, and `render-episode` is deliciously declarative:

```clj
(defn render-episode [state location]
  (let [episode (get-episode state location)]
    [:main
     [:h1 (or (:episode/title episode)
              "Unknown episode")]
     (if (-> location :location/hash-params :description)
       (list
        [:p (:episode/description episode)]
        [:ui/a {:ui/location (update location :location/hash-params dissoc :description)}
         "Hide description"])
       (when (:episode/description episode)
         [:ui/a {:ui/location (assoc-in location [:location/hash-params :description] "1")}
          "Show description"]))
     [:p
      [:ui/a {:ui/location {:location/page-id :pages/frontpage}}
       "Back to episode listing"]]]))
```

Remember that aliases behave just like any other hiccup element, so you can add
classes with `:ui/a.btn`, give it attributes, and so on.

The [full code-listing is available on
github](https://github.com/cjohansen/replicant-routing).

## Further reading

If you have added state management to your app, you might be wondering how to
combine routing and state management. The details of doing this is covered in
the state management tutorials: [with an atom](/tutorial/state-atom/) or [with
Datascript](/tutorial/datascript/).
