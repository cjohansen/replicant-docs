(ns repliweb.elements.showcase
  (:require [replicant.alias :refer [defalias]]))

(def styles
  {:gradient ["bg-base-200" "lg:bg-gradient-to-r" "lg:from-base-100" "lg:to-base-300"]
   :light ["bg-base-100"]})

(defn ^{:indent 1} render-showcase [attrs children]
  [:div.lg:flex.items-center.max-w-6xl.mx-auto.rounded-md.border.border-neutral
   {:class (get styles (::style attrs))}
   children])

(defn ^{:indent 1} render-code [attrs code]
  [:pre.text-sm.rounded-md.flex-1 attrs
   (into [:code.clojure code])])

(defalias showcase [attrs children]
  (render-showcase attrs children))

(defalias code [attrs code]
  (render-code attrs code))
