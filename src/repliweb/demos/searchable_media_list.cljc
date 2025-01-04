(ns repliweb.demos.searchable-media-list
  (:require [clojure.string :as str]
            [phosphor.icons :as icons]
            [replicant.dom :as r]
            [repliweb.elements.examples :as examples]
            [repliweb.elements.input :refer [Input]]
            [repliweb.elements.media :refer [MediaList]]
            [repliweb.elements.typography :as typo]))

(defn search-videos [q videos]
  (if-let [q-re (some-> q re-pattern)]
    (filter #(or (re-find q-re (str/lower-case (:episode/title %)))
                 (re-find q-re (str "episode " (:episode/number %)))) videos)
    videos))

(defn render [{:keys [videos]}]
  [:div {:data-example-ns "repliweb.demos.searchable-media-list"}
   (typo/h2 "Parens of the dead episodes")
   [:div.my-4
    (Input
     {:placeholder "Search"
      :icon (icons/icon :phosphor.bold/magnifying-glass)
      :input-actions [::search :event/target.value]})]
   (MediaList
    {:medias (map examples/video->media-data videos)})])

(defn handle-action [store replicant-data action _args]
  (case action
    ::search
    (let [q (some-> replicant-data :replicant/dom-event .-target .-value not-empty)]
      (swap! store assoc :videos (search-videos q examples/videos)))))

(defn main [el store]
  (add-watch store ::render (fn [_ _ _ state]
                              (r/render el (render state))))
  (swap! store assoc :videos examples/videos))
