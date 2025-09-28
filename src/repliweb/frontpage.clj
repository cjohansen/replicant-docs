(ns repliweb.frontpage
  (:require [phosphor.icons :as icons]
            [repliweb.demos.data-driven-transitions :as ddt]
            [repliweb.demos.searchable-media-list :as sml]
            [repliweb.elements.examples :as examples]
            [repliweb.elements.layout :as layout]
            [repliweb.elements.logos :as logos]
            [repliweb.elements.media :refer [Media]]
            [repliweb.elements.showcase :as showcase]
            [repliweb.elements.typography :as typo]))

(defn render-page [_ctx _page]
  (layout/layout
   {:title "Replicant — Simpler, more testable UIs with pure functions and data"}
   (layout/header)
   (layout/section {:class (into ["flex" "flex-col" "items-center"]
                                 (layout/section-styles :dark))}
     [:div.w-32 (logos/replicant)]
     (typo/h1 "Replicant")
     (typo/lead {:class ["text-center" "max-w-2xl" "mx-auto"]}
       "Build simpler, more testable UIs with pure functions and data.
       Separate rendering from domain logic and state, and finally enjoy true
       functional programming when building user interfaces.")
     [:div.flex.gap-2
      [:a.btn.btn-primary {:href "/learn/"} "Learn Replicant"]
      [:a.btn.btn-primary.btn-outline {:href "https://cljdoc.org/d/no.cjohansen/replicant/"}
       "API Reference"]])

   (layout/section {}
     [:div.max-w-3xl.mx-auto.text-center
      (typo/h2 "Create user interfaces with data")
      (typo/p {:class ["text-center"]}
        "Build user interfaces with " [:em "hiccup"]
        " — plain old Clojure data literals like vectors, keywords,
      maps and strings. Render to strings on the server or render (and
      re-render) the live DOM in browsers, just like you would with React and
      its peers.")]
     (showcase/render-showcase {::showcase/style :gradient
                                :class #{"mx-auto" "max-w-6xl"}}
       [(showcase/render-code {:class ["bg-base-300"]}
          ["[:div.media-thumb
 [:a {:href \"https://www.youtube.com/watch?v=AGTDfXKGvNI\"}
  [:img.rounded-lg
   {:src \"/images/ui-pure-simple.png\"
    :alt \"Watch UI, Pure and Simple\"}]
  [:div.overlay
   [:a.btn.btn-circle
    {:data-theme \"cupcake\"
     :href \"https://www.youtube.com/watch?v=AGTDfXKGvNI\"}
    [:svg.h-4.w-4
     {:xmlns \"http://www.w3.org/2000/svg\"
      :viewBox \"0 0 256 256\"
      :style {:display \"inline-block\"
              :line-height \"1\"}}
     [:path {:d \"M72,39.88V216.12a8,8,0,0...\"
             :fill \"none\"
             :stroke \"currentColor\"}]]]]]]"])
        [:div.flex-1
         [:div.media-thumb.relative.max-w-96.mx-auto
          [:a {:href "https://www.youtube.com/watch?v=AGTDfXKGvNI"}
           [:img.rounded-lg
            {:src "/images/ui-pure-simple.png"
             :alt "Watch talk: UI, Pure and Simple"}]
           [:div.overlay
            [:a.btn.btn-circle
             {:data-theme "cupcake"
              :href "https://www.youtube.com/watch?v=AGTDfXKGvNI"}
             [:svg.h-4.w-4
              {:xmlns "http://www.w3.org/2000/svg"
               :viewBox "0 0 256 256"
               :style {:display "inline-block", :line-height "1"}}
              [:path {:d "M72,39.88V216.12a8,8,0,0,0,12.15,6.69l144.08-88.12a7.82,7.82,0,0,0,0-13.38L84.15,33.19A8,8,0,0,0,72,39.88Z"
                      :fill "none"
                      :stroke "currentColor"
                      :stroke-linecap "round"
                      :stroke-linejoin "round"
                      :stroke-width "16"}]]]]]]]])

     (typo/p {:class ["max-w-3xl" "mx-auto" "text-center"]}
       "Hiccup is highly expressive and unlike JSX does not require any
       additional build step — it's just Clojure. Replicant's dialect supports
       some features not found in other libraries, learn more in " (typo/a
                                                                    {:href "/hiccup/"} "the hiccup reference") "."))

   (layout/section {:class (layout/section-styles :medium)}
     [:div.max-w-3xl.mx-auto
      (typo/h2 {:class ["text-center"]} "Reusable UI elements with pure functions")
      (typo/p {:class ["text-center"]}
        "Structure your UI in reusable bits and pieces with regular Clojure
      functions. Keep these pure functions in cljc files and use them on the
      server or on the client. No framework specific component abstractions
      required.")]
     (showcase/render-showcase {::showcase/style :light
                                :class #{"mx-auto" "max-w-6xl"}}
       [(showcase/render-code {:class ["bg-base-300"]}
          ["(defn Media [{:keys [theme thumbnail url
                     title text button]}]
  [:div.media {:data-theme theme}
   [:aside.media-thumb
    (Thumbnail thumbnail)]
   [:main.grow
    [:a.hover:underline {:href url}
     [:h2.font-bold title]
     [:p text]]]
   (Button (assoc button :style :ghost))])"])
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
         ]])

     (typo/p {:class ["max-w-3xl" "mx-auto" "text-center"]}
       "While domain-aware UI components like "
       (typo/code "Video") " and " (typo/code "LikeButton") " can look neat in
       quick demos and conference talks, coupling the business domain with
       rendering logic scales poorly and leads to duplicated UI code. Replicant
       encourages generic data-driven UI elements by not providing a stateful
       component abstraction. Learn " (typo/a {:href "https://vimeo.com/861600197"} "why generic elements
       improve frontend code-bases") "."))

   (layout/section {:class (layout/section-styles :dark)}
     [:div.max-w-3xl.mx-auto
      (typo/h2 {:class ["text-center"]} "Data-driven interactivity")
      (typo/p {:class ["text-center"]}
        "Even event handlers can be data, giving you the option of handling them
        all in a single global handler function. Your UI remains pure data,
        event handlers declare their intended effects, and you can trivially
        test the UI even when it supports user interactivity.")]
     (showcase/render-showcase {::showcase/style :light
                                :class #{"mx-auto" "max-w-6xl"}}
       [(showcase/render-code {:class ["bg-base-300"]}
          ["(replicant.dom/set-dispatch!
 (fn handle-dom-event [rd [action]]
   (case action
     ::search-videos
     (let [q (-> rd :replicant/dom-event
                 .-target .-value)]
       (swap! store search-videos q)))))

(defn render [state]
  [:div
   [:h2 \"Parens of the dead episodes\"]
   [:label.input-field
    [:input.grow
     {:type \"text\"
      :placeholder \"Search\"
      :on {:input [::search-videos]}}]
    (icons/render icon)]
   (MediaList
    {:medias (->> (:results state)
                  (map video->media-data))})])"])
        [:div.flex-1.p-4.self-start
         (sml/render {:videos examples/videos})]])

     (typo/p {:class ["max-w-3xl" "mx-auto" "text-center"]}
       "Replicant imposes no structure on your event data — it's just passed to
       the global event handler. You're free to make your own declarative
       interactivity DSL. Oh, and event handlers can be regular functions as
       well." [:br] "Learn more about " (typo/a
                                         {:href "/event-handlers/"} "event handlers") "."))

   (layout/section {}
     [:div.max-w-3xl.mx-auto
      (typo/h2 {:class ["text-center"]} "Easy data-driven transitions")
      (typo/p {:class ["text-center"]}
        "Replicant allows you to specify overrides for any attributes during
        mounting and unmounting. This allows you to declaratively transition
        elements on mount and unmount, like fading in an element as it's
        mounted:")]
     (showcase/render-showcase {::showcase/style :gradient
                                :class #{"mx-auto" "max-w-6xl"}}
       [(showcase/render-code {:class ["bg-base-300"]}
          ["(when (:visible? props)
  [:div {:style {:transition \"opacity 0.25s\"
                 :opacity 1}
         :replicant/mounting {:style {:opacity 0}}
         :replicant/unmounting {:style {:opacity 0}}}
   (Media
    {:thumbnail {:image \"/images/christian.jpg\"
                 :size 12}
     :title \"Christian Johansen\"
     :text (list \"Just wrote some documentation for Replicant.\"
                 [:span.opacity-50 \"Posted December 11th 2024\"])})])"])
        [:div.flex-1.p-4
         (ddt/render {:loaded? true})]])

     (typo/p {:class ["max-w-3xl" "mx-auto" "text-center"]}
       "This is not limited to inline styles, you can stick any attribute in "
       (typo/code ":replicant/mounting") " and " (typo/code ":replicant/unmounting")
       ", like " (typo/code ":class") ". Learn more tidbits like this in "
       (typo/a {:href "/hiccup/"} "the hiccup reference") "."))

   (layout/section {:class (layout/section-styles :medium)}
     [:div.max-w-3xl.mx-auto
      (typo/h2 {:class ["text-center"]} "Extend Replicant with custom element aliases")
      (typo/p {:class ["text-center"]}
        "Custom element aliases can extend Replicant's hiccup vocabulary.
        Aliases are stateless wrappers that can expand to arbitrary hiccup.
        Alias functions are only called when their arguments change, and they
        can receive side-chained data, removing the need to pass certain data
        everywhere. Perfect for data that is static (e.g. i18n dictionaries), or
        change very infrequently (e.g. locales).")]
     (showcase/render-showcase {::showcase/style :gradient
                                :class #{"mx-auto" "max-w-6xl"}}
       [(showcase/render-code {:class ["bg-base-300"]}
          ["[:div.media
 [:aside.media-thumb
  [:img.rounded-lg {:src (:person/profile-pic author)}]]
 [:main.grow
  [:h2.font-bold (:person/full-name author)]
  [:p (:post/text post)]
  [:p.opacity-50
   [:i18n/k ::posted {:date (:post/created-at post)}]]]]"])
        [:div.flex-1.p-4
         (Media
          {:thumbnail {:image "/images/christian.jpg"
                       :size 20}
           :title "Christian Johansen"
           :text (list "Just wrote some documentation for Replicant"
                       [:span.opacity-50 "Posted December 11th 2024"])
           })
         ]])

     (typo/p {:class ["max-w-3xl" "mx-auto" "text-center"]}
       "In this example, " (typo/code ":i18n/k") " is a user-provided extension
       that uses a library to seemingly give Replicant built-in i18n
       capabilities. And it's all still data. Learn more about "
       (typo/a {:href "/alias/"} "aliases") ", and check out the "
       (typo/a {:href "/tutorials/i18n-alias/"} "i18n tutorial") "."))

   (layout/section {:class (layout/section-styles :dark)}
     [:div.max-w-3xl.mx-auto
      (typo/h2 {:class ["text-center"]} "You can test your UI")
      (typo/p {:class ["text-center"]}
        "When the entire UI is represented by data created by a pure function,
        testing is trivial: Pass in some data, assert that it appears in the UI
        somehow. Rinse and repeat. You can even verify that event handlers will
        \"do\" what you expect, as long as they're expressed as data.")]
     (showcase/render-showcase {::showcase/style :light
                                :class #{"mx-auto" "max-w-6xl"}}
       [(showcase/render-code {}
          ["(defn MediaList [{:keys [title medias]}]
  [:div.media-list
   (when title (typo/h2 title))
   (map Media medias)])

(defn prepare-ui-data [{:keys [user video]}]
  {:title (str (count videos) \" videos\")
   :medias (map #(video->media-data user %) videos)})

;; Take some domain data
(->> @store
     ;; Convert it to UI data
     prepare-ui-data
     ;; Convert it to hiccup
     MediaList
     ;; ...and render it
     (r/render el))"])
        (showcase/render-code {:class ["bg-base-300"]}
          ["(deftest prepare-ui-data-test
  (testing \"Uses episode number for title\"
    (is (= (->> {:videos [{:episode/number 1}]}
                prepare-ui-data :medias first
                :title)
           \"Episode 1\")))

  (testing \"Includes a like button\"
    (is (= (->> {:videos [{:video/id \"v898900\"
                           :episode/number 1}]
                 :user {:user/id \"u09b\"}}
                prepare-ui-data :medias first
                :button)
           {:title \"Like video\"
            :icon :phosphor.regular/heart
            :on {:click [[:command/like-video
                          {:user/id \"u09b\"
                           :video/id \"v898900\"}]]}}))))"])
        ])

     (typo/p {:class ["max-w-3xl" "mx-auto" "text-center"]}
       "Pure functions like " (typo/code "video->media-data")
       " capture the essentials of turning your domain data into a visual user
       interface without the volatile details of markup — excellent targets for
       plain old unit tests. No elaborate browser automation required. Check out
       the "
       (typo/a {:href "/tutorials/tic-tac-toe/"} "getting started tutorial") "
       for a practical demonstration."))

   (layout/section {}
     [:div.max-w-3xl.mx-auto
      (typo/h2 {:class ["text-center"]} "What are developers saying?")
      [:blockquote.blockquote.mt-8
       [:p
        "Functional programming in the UI? It always felt like a pipe dream, and
React was the closest we would ever get. Then I stumbled across Replicant and
the game changed completely. I hadn’t realized how much I had longed for
building fully data oriented UIs!"]
       [:p.text-right
        [:cite
         [:a {:href "https://github.com/pez"} "Peter Strömberg"]
         ", frontend developer, creator of "
         [:a {:href "https://calva.io/"} "Calva"]]]]])

   (layout/section {:class (layout/section-styles :dark)}
     [:div.max-w-3xl.mx-auto
      (typo/h2 {:class ["text-center"]} "Unidirectional data flow")
      (typo/p {:class ["text-center"]}
        "With Replicant you always render the entire UI, starting at the root node.
        There are no atoms, subscriptions, sub-tree rendering, component-local
        state, or other moving parts. Just a pure function that takes in your
        domain data and returns the entire UI. Boring in a good way. Simple AND
        easy.")]

     (typo/p {:class ["max-w-3xl" "mx-auto" "text-center"]}
       "Learn more about how Replicant makes this possible, and why you
       shouldn't worry about performance in " [:br] (typo/a
                                                     {:href "/top-down/"} "Why top-down rendering is the best frontend
       programming model") "."))

   (layout/section {}
     [:div.max-w-3xl.mx-auto.md:flex.gap-2
      [:div.flex.gap-2
       [:div.w-6 (logos/replicant)]
       "Replicant logo by"
       [:span (typo/a {:href "https://github.com/jaidetree"} "jaide") "/"
        (typo/a {:href "https://eccentric-j.com/"} "eccentric-j") ". "]]
      [:span (typo/a {:href "https://github.com/cjohansen/replicant-docs"} "This website source on Github") "."]])))
