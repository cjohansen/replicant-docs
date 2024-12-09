(ns repliweb.elements.media
  (:require [repliweb.elements.button :refer [Button]]
            [repliweb.elements.thumbnail :refer [Thumbnail]]
            [repliweb.elements.typography :as typo]))

(defn Media [{:keys [thumbnail title text url button theme]}]
  [:div.rounded-2xl.p-2.text-sm.xl:text-base.flex.flex-row.items-center.gap-3
   (when theme
     {:data-theme theme})
   [:div.w-40 {:class ["basis-1/4"]}
    (Thumbnail thumbnail)]
   [:a.grow.hover:underline {:href url}
    [:h2.font-bold title]
    [:p text]]
   (Button
    (assoc button :style :ghost))])

(defn MediaList [{:keys [title medias]}]
  [:div.flex.flex-col.gap-2
   (typo/h2 title)
   (map Media medias)])
