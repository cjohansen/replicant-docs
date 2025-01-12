(ns repliweb.elements.showcase
  (:require [replicant.alias :refer [defalias]]))

(def styles
  {:gradient ["bg-base-200" "lg:bg-gradient-to-r" "lg:from-base-100" "lg:to-base-300"]
   :light ["bg-base-100"]})

(defn ^{:indent 1} render-showcase [attrs children]
  [:div.lg:flex.items-stretch.rounded-md.border.border-neutral
   (-> (select-keys attrs (->> (keys attrs)
                               (filter (comp nil? namespace))))
       (update :class concat (get styles (::style attrs))))
   children])

(defn ^{:indent 1} render-code [attrs code]
  [:pre.codehilite attrs
   (when-let [title (::title attrs)]
     [:span.code-title title])
   (into [:code {:class (or (::lang attrs) "clojure")}] code)])

(defalias showcase [attrs children]
  (render-showcase attrs children))

(defalias code [attrs code]
  (render-code attrs code))
