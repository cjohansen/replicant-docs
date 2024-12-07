(ns repliweb.elements.thumbnail
  (:require [repliweb.elements.button :refer [Button]]))

(defn Thumbnail [{:keys [image alt icon actions url icon-size]}]
  [(if url :a :div)
   (cond-> {:class ["relative"]}
     url (assoc :href url)
     actions (assoc :on {:click actions}))
   [:img.rounded-lg {:src image :alt alt}]
   [:div.absolute.inset-0.flex.items-center.justify-center
    (Button
     {:icon icon
      :theme "cupcake"
      :size icon-size
      :title alt})]])
