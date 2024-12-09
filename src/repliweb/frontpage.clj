(ns repliweb.frontpage
  (:require [phosphor.icons :as icons]
            [repliweb.elements.examples :as examples]
            [repliweb.elements.layout :as layout]
            [repliweb.elements.media :refer [Media MediaList]]
            [repliweb.elements.typography :as typo]))

(defn logo [& [{:keys [size]}]]
  [:pre {:class (or size "text-xl")}
   [:span.font-bold.text-cljblue "["]
   [:span.text-cljlightgreen ":replicant"]
   [:span.font-bold.text-cljblue "]"]])

(defn ^{:indent 1} section [attrs & body]
  (into [:div.py-20.px-4 attrs] body))

(defn render-page [_ctx _page]
  (layout/layout
   {:title "Replicant - Data-driven UIs for Clojure"}
   [:div.m-4 (logo)]
   (section {:class ["bg-gradient-to-b" "from-base-200" "to-base-300"
                     "flex" "flex-col" "items-center"]}
     (typo/h1 "Replicant")
     (typo/lead "A functional Clojure(Script) library for data-driven user-interfaces on the web.")
     [:div.flex.gap-2
      [:a.btn.btn-primary {:href ""} "Learn Replicant"]
      [:a.btn.btn-primary.btn-outline {:href ""} "API Reference"]])

   (section {}
     [:div.max-w-3xl.mx-auto
      (typo/h2 {:class ["text-center"]} "Create user interfaces with data")
      (typo/p {:class ["text-center"]}
              "Replicant lets you build user interfaces with " [:em "hiccup"]
              " - plain old Clojure data literals like vectors, keywords,
      maps and strings. Structure your UI in reusable bits and pieces with
      regular Clojure functions. Keep these functions in cljc files and use them
      on the server or on the client.")]
     [:div.lg:flex.items-center.bg-base-200.lg:bg-gradient-to-r.lg:from-base-100.lg:to-base-300.max-w-6xl.mx-auto.rounded-md.border.border-neutral
      [:pre.bg-base-300.text-sm.rounded-md.flex-1
       [:code.clojure "(defn Media [{:keys [theme thumbnail url
                     title text button]}]
  [:div.rounded-2xl.p-4 {:data-theme theme}
   [:div.flex.flex-row.items-center.gap-3
    [:div.w-40
     (Thumbnail thumbnail)]
    [:a.grow {:href url}
     [:h2.font-bold title]
     [:p text]]
    (Button (assoc button :style :ghost))]])"]]
      [:div.flex-1.p-4
       (Media
        {:thumbnail
         {:image "/images/data-driven.png"
          :alt "Watch talk: Stateless, Data-driven UIs"
          :url "https://vimeo.com/861600197"
          :icon (icons/icon :phosphor.regular/play)
          :icon-size :small}

         :title "Stateless, Data-driven UIs"
         :text "How to build data-driven components and what they're good at"
         :url "https://vimeo.com/861600197"
         :theme "cupcake"

         :button
         {:title "Like video"
          :icon (icons/icon :phosphor.regular/heart)
          :actions [[:action/execute-command
                     [:command/like-video {:video/id "bc231"}]]]}})
       ]]

     (typo/p {:class ["max-w-3xl" "mx-auto" "text-center"]}
             "While domain-aware UI components like "
             [:code.text-secondary "Video"] "
     and " [:code.text-secondary "LikeButton"] " can look neat in quick demos
     and conference talks, components that couple the business domain with
     rendering logic scale poorly and lead to duplicated UI code. Replicant
     encourages generic data-driven UI elements by not providing a stateful
     component abstraction."))

   (section {:class ["bg-gradient-to-b" "from-base-300" "to-base-100"]}
     [:div.max-w-3xl.mx-auto
      (typo/h2 {:class ["text-center"]} "Combine UI elements with regular Clojure functions")
      (typo/p {:class ["text-center"]}
        "When your UI is made from regular Clojure data structures, you can use all the amazing stuff in "
        [:code.text-secondary "clojure.core"] " and your favorite libraries to
        build them. Have a list of videos to show? " [:code.text-secondary "map"]
        " over your collection with a function that returns hiccup and render
        the result.")]
     [:div.lg:flex.items-center.bg-base-100.max-w-6xl.mx-auto.rounded-md.border.border-neutral
      [:pre.bg-base-200.text-sm.rounded-md.flex-1
       [:code.clojure "(defn MediaList [{:keys [title medias]}]
  [:div.flex.flex-col.gap-2
   (typo/h2 title)
   (map Media medias)])

;; Take some domain data
(let [videos {,,,}]
  ;; ...convert it to generic UI data
  (->> {:title (str (count videos) \" videos\")
        :medias (map video->media-data videos)}
       ;; ...turn it into hiccup
       MediaList
       ;; ...and render it
       (r/render el)))"]]
      [:div.flex-1.p-4
       (let [videos examples/videos]
         (MediaList
          {:title (str (count videos) " videos")
           :medias (map examples/video->media-data videos)}))
       ]]

     (typo/p {:class ["max-w-3xl" "mx-auto" "text-center"]}
             "Helper functions like " [:code.text-secondary "video->media-data"]
             " are pure functions that captures the essentials of turning your
             domain data into a visual user interface without the fleeting
             details of markup - excellent targets for plain old unit tests."))

   (section {:class ["bg-gradient-to-b" "from-base-200" "to-base-100"]}
     [:div.max-w-3xl.mx-auto
      (typo/h2 {:class ["text-center"]} "Express event handlers as data")
      (typo/p {:class ["text-center"]}
        "When event handlers are data instead of functions, your UI is still
        100% data, and the desired effect of the events can be inspected (and
        tested). Replicant provides a global event handler where you can process
        event data however you want.")]
     [:div.lg:flex.items-center.bg-base-100.max-w-6xl.mx-auto.rounded-md.border.border-neutral
      [:pre.bg-base-200.text-sm.rounded-md.flex-1
       [:code.clojure "(defn render [media-list]
  [:div
   (SearchInput
    {:on {:change :action/search-videos}})

   (MediaList media-list)])

(r/set-dispatch!
 (fn [rd event-data]
   (let [e (:replicant/dom-event rd)]
     (case event-data
       :action/search-videos
       (->> videos
            (search-videos (.. e -target -value))
            videos->media-list
            render)

       (println \"Unsupported action!\" action args)))))"]]
      [:div.flex-1.p-4
       (let [videos examples/videos]
         (MediaList
          {:title (str (count videos) " videos")
           :medias (map examples/video->media-data videos)}))
       ]]

     (typo/p {:class ["max-w-3xl" "mx-auto" "text-center"]}
       "Replicant is a rendering library. It has no state management or built-in
       effects system. Use your favorite libraries or roll your own. The docs
       have guides for setting up simple and effective state management based
       on " (typo/a {:href "#"} "atoms")
       " or " (typo/a {:href "#"} "Datascript") "."))))
