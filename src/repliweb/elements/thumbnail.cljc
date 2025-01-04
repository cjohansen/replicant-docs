(ns repliweb.elements.thumbnail
  (:require [repliweb.elements.button :refer [Button]]))

(defn Thumbnail [{:keys [class image alt icon actions url icon-size]}]
  [(if url :a :div)
   (cond-> {:class ["relative"]}
     url (assoc :href url)
     actions (assoc :on {:click actions})
     (coll? class) (update :class concat class)
     (or (string? class) (keyword? class)) (update :class conj class))
   [:img.rounded-lg {:src image :alt alt :replicant/key image}]
   (when icon
     [:div.overlay
      (Button
       {:icon icon
        :theme "cupcake"
        :size icon-size
        :title alt})])])
