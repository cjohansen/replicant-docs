(ns repliweb.example-scenes
  (:require [clojure.string :as str]
            [phosphor.icons :as icons]
            [portfolio.replicant :refer-macros [defscene]]
            [replicant.dom :as r]
            [repliweb.elements.examples :as examples]
            [repliweb.elements.input :refer [Input]]
            [repliweb.elements.media :refer [Media MediaList]]
            [repliweb.elements.typography :as typo]))

(defn search-videos [q videos]
  (if-let [q-re (some-> q re-pattern)]
    (filter #(or (re-find q-re (str/lower-case (:episode/title %)))
                 (re-find q-re (str "episode " (:episode/number %)))) videos)
    videos))

(defscene searchable-media-list-example
  :params (atom {:videos examples/videos})
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [replicant-data [action]]
                 (case action
                   ::search
                   (let [q (some-> replicant-data :replicant/dom-event .-target .-value not-empty)]
                     (swap! store assoc :videos (search-videos q examples/videos)))))))
  [store]
  [:div
   (typo/h2 "Parens of the dead episodes")
   [:div.my-4
    (Input
     {:placeholder "Search"
      :icon (icons/icon :phosphor.bold/magnifying-glass)
      :input-actions [::search :event/target.value]})]
   (MediaList
    {:medias (map examples/video->media-data (:videos @store))})])

(defscene user-post-media-example
  (Media
   {:thumbnail {:image "/images/christian.jpg"
                :size 12}
    :title "Christian Johansen"
    :text (list "Just wrote some documentation for Replicant, hope people find it useful."
                [:span.opacity-50 "Posted December 11th 2024"])
    }))

(defscene fade-example
  :params (atom {:visible? false})
  [props]
  [:div {:on {:click #(swap! props update :visible? not)}
         :style {:min-width 100
                 :min-height 100}}
(when (:visible? @props)
  [:div {:style {:transition "opacity 0.25s"
                 :opacity 1}
         :replicant/mounting {:style {:opacity 0}}
         :replicant/unmounting {:style {:opacity 0}}}
   (Media
    {:thumbnail {:image "/images/christian.jpg"
                 :size 12}
     :title "Christian Johansen"
     :text (list "Just wrote some documentation for Replicant, hope people find it useful."
                 [:span.opacity-50 "Posted December 11th 2024"])})])])
