(ns repliweb.input-scenes
  (:require [phosphor.icons :as icons]
            [portfolio.replicant :refer-macros [defscene]]
            [repliweb.elements.input :refer [Input]]))

(defscene input-with-placeholder
  (Input {:placeholder "Name"}))

(defscene input-with-icon
  [:div.text-base-content
   (Input
    {:placeholder "Search"
     :icon (icons/icon :phosphor.bold/magnifying-glass)})])
