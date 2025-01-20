(ns repliweb.demos.data-driven-transitions
  (:require [repliweb.elements.media :refer [Media]]
            [replicant.dom :as r]
            [repliweb.assets :as assets]))

(defn render [{:keys [loaded?]}]
  [:div {:data-example-ns "repliweb.demos.data-driven-transitions"}
   (when loaded?
     [:div {:style {:transition "opacity 0.25s"
                    :opacity 1}
            :replicant/mounting {:style {:opacity 0}}
            :replicant/unmounting {:style {:opacity 0}}}
      (Media
       {:thumbnail {:image (assets/get-asset-path "/images/christian.jpg")
                    :size 20}
        :title "Christian Johansen"
        :text (list "Just wrote some documentation for Replicant"
                    [:span.opacity-50 "Posted December 11th 2024"])})])
   [:p [:a.underline.hover:no-underline.cursor-pointer
        {:on {:click [::toggle]}}
        (if loaded? "Hide post" "Show post")]]])

(defn main [el store]
  (add-watch store ::render (fn [_ _ _ state]
                              (r/render el (render state))))
  (swap! store assoc :loaded? false))

(defn handle-action [store _ action _]
  (case action
    ::toggle
    (swap! store update :loaded? not)))
