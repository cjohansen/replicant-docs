(ns repliweb.elements.media
  (:require [repliweb.elements.button :refer [Button]]
            [repliweb.elements.thumbnail :refer [Thumbnail]]
            [repliweb.elements.typography :as typo]
            [phosphor.icons :as icons]))

(def sizes
  {12 "basis-12"
   16 "basis-16"
   20 "basis-20"})

(defn Media [{:keys [thumbnail title text url button theme]}]
  [:div.media
   (when theme
     {:data-theme theme})
   [:aside.media-thumb
    {:class (when (number? (:size thumbnail))
              (sizes (:size thumbnail)))}
    (Thumbnail thumbnail)]
   [:main.grow
    [(if url :a.hover:underline :div)
     (cond-> {}
       url (assoc :href url))
     [:h2.font-bold title]
     (if (coll? text)
       (for [t text]
         [:p t])
       [:p text])]]
   (when button
     (Button
      (assoc button :style :ghost)))])

(defn MediaList [{:keys [title medias]}]
  [:div.media-list
   (when title (typo/h2 title))
   (map Media medias)])

(comment

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


  [:div.media-thumb
   [:a.relative {:href "https://vimeo.com/861600197"}
    [:img.rounded-lg
     {:src "/images/data-driven.png"
      :alt "Watch talk: Stateless, Data-driven UIs"}]
    [:div.overlay
     [:a.btn.btn-circle.btn-sm
      {:href "https://vimeo.com/861600197"}
      [:svg.h-4.w-4
       {:xmlns "http://www.w3.org/2000/svg"
        :viewBox "0 0 256 256"
        :style {:display "inline-block", :line-height "1"}}
       [:path {:d "M72,39.88V216.12a8,8,0,0,0,12.15,6.69l144..."
               :fill "none"
               :stroke "currentColor"
               :stroke-linecap "round"
               :stroke-linejoin "round"
               :stroke-width "16"}]]]]]]

)
