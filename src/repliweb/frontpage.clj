(ns repliweb.frontpage
  (:require [phosphor.icons :as icons]
            [repliweb.elements.layout :as layout]
            [repliweb.elements.media :refer [Media]]
            [repliweb.elements.typography :as typo]))

(defn logo [& [{:keys [size]}]]
  [:pre {:class (or size "text-xl")}
   [:span.font-bold.text-cljblue "["]
   [:span.text-cljlightgreen.-mr-1 ":"]
   [:span.text-cljlightgreen "replicant"]
   [:span.font-bold.text-cljblue "]"]])

(defn ^{:indent 1} section [attrs & body]
  (into [:div.py-20.px-4 attrs] body))

(defn render-page [_ctx _page]
  (layout/layout
   {:title "Replicant - Data-driven UIs for Clojure"}
   [:div.m-4 (logo)]
   (section {:class ["bg-base-300" "flex" "flex-col" "items-center"]}
     (typo/h1 "Replicant")
     (typo/lead "A functional Clojure(Script) library for data-driven user-interfaces on the web.")
     [:div.flex.gap-2
      [:a.btn.btn-primary {:href ""} "Learn Replicant"]
      [:a.btn.btn-primary.btn-outline {:href ""} "API Reference"]])

   (section {:class ["mx-auto"]}
     [:div.max-w-3xl.mx-auto
      (typo/h2 {:class ["text-center"]} "Create user interfaces with data")
      (typo/p {:class ["text-center"]}
              "Replicant lets you build user interfaces with " [:em "hiccup"]
              " - plain old Clojure data literals like vectors, keywords,
      maps and strings. Structure your UI in reusable bits and pieces with
      regular Clojure functions. Keep these functions in cljc files and use them
      on the server or on the client.")]
     [:div.lg:flex.items-center.bg-base-200.lg:bg-gradient-to-r.lg:from-base-100.lg:to-base-300.max-w-6xl.mx-auto
      [:pre.bg-base-300.text-sm.rounded-md.flex-1
       [:code.clojure "(defn Media [{:keys [theme thumbnail url
                     title text button]}]
  [:div.rounded-2xl.p-4 {:data-theme theme}
   [:div.flex.flex-row.items-center.gap-3
    [:div.w-40
     (Thumbnail thumbnail)]
    [:a.grow {:href url}
     [:h3.font-bold title]
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
     component abstraction."))))
