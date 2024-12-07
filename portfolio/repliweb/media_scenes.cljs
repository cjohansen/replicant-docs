(ns repliweb.media-scenes
  (:require [phosphor.icons :as icons]
            [portfolio.replicant :refer-macros [defscene]]
            [repliweb.elements.media :refer [Media]]))

(defscene default-example
  (Media
   {:thumbnail
    {:image "/images/data-driven.png"
     :alt "Watch talk: Stateless, Data-driven UIs"
     :url "/watch"
     :icon (icons/icon :phosphor.regular/play)}

    :title "Stateless, Data-driven UIs"
    :text "How to build data-driven components and what they're good at"
    :url "/watch"
    :theme "cupcake"

    :button
    {:title "Like video"
     :icon (icons/icon :phosphor.regular/heart)
     :actions [[:action/execute-command
                [:command/like-video {:video/id "bc231"}]]]}}))
