(ns repliweb.elements.media
  (:require [repliweb.elements.button :refer [Button]]
            [repliweb.elements.thumbnail :refer [Thumbnail]]))

(defn Media [{:keys [thumbnail title text url button theme]}]
  [:div.rounded-2xl.p-4.text-sm.xl:text-base.flex.flex-row.items-center.gap-3
   (when theme
     {:data-theme theme})
   [:div.w-40
    (Thumbnail thumbnail)]
   [:a.grow.hover:underline {:href url}
    [:h3.font-bold title]
    [:p text]]
   (Button
    (assoc button :style :ghost))])
