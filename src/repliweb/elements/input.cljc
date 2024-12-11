(ns repliweb.elements.input
  (:require [phosphor.icons :as icons]))

(defn Input [{:keys [placeholder icon input-actions]}]
  [:label.input-field
   [:input.grow
    (cond-> {:type "text" :placeholder placeholder}
      input-actions (assoc-in [:on :input] input-actions))]
   (when icon
     (icons/render icon {:class ["h-4" "w-4" "opacity-70"]}))])
