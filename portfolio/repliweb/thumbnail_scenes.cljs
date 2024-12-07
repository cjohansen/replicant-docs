(ns repliweb.thumbnail-scenes
  (:require [phosphor.icons :as icons]
            [portfolio.replicant :refer-macros [defscene]]
            [repliweb.elements.thumbnail :refer [Thumbnail]]))

(defscene large-button
  (Thumbnail
   {:image "/images/data-driven.png"
    :alt "Watch talk: Stateless, Data-driven UIs"
    :icon (icons/icon :phosphor.regular/play)
    :icon-size :large}))

(defscene small-button
  (Thumbnail
   {:image "/images/data-driven.png"
    :alt "Watch talk: Stateless, Data-driven UIs"
    :icon (icons/icon :phosphor.regular/play)
    :icon-size :small}))
