(ns repliweb.button-scenes
  (:require [phosphor.icons :as icons]
            [portfolio.replicant :refer-macros [defscene]]
            [repliweb.elements.button :refer [Button]]))

(defscene buttons
  [:div.flex.gap-4
   (Button {:text "Standard"})
   (Button
    {:text "Neutral"
     :style :neutral})
   (Button
    {:text "Primary"
     :style :primary})
   (Button
    {:text "Secondary"
     :style :secondary})
   (Button
    {:text "Accent"
     :style :accent})
   (Button
    {:text "Ghost"
     :style :ghost})
   (Button
    {:text "Link"
     :style :link})])

(defscene active-buttons
  [:div.flex.gap-4
   (Button {:text "Standard" :active? true})
   (Button
    {:text "Neutral"
     :style :neutral
     :active? true})
   (Button
    {:text "Primary"
     :style :primary
     :active? true})
   (Button
    {:text "Secondary"
     :style :secondary
     :active? true})
   (Button
    {:text "Accent"
     :style :accent
     :active? true})
   (Button
    {:text "Ghost"
     :style :ghost
     :active? true})
   (Button
    {:text "Link"
     :style :link
     :active? true})])

(defscene state-colors
  [:div.flex.gap-4
   (Button
    {:text "Info"
     :style :info})
   (Button
    {:text "Success"
     :style :success})
   (Button
    {:text "Warning"
     :style :warning})
   (Button
    {:text "Error"
     :style :error})])

(defscene outlined
  [:div.flex.gap-4
   (Button
    {:text "Info"
     :style :info
     :outline? true})
   (Button
    {:text "Success"
     :style :success
     :outline? true})
   (Button
    {:text "Warning"
     :style :warning
     :outline? true})
   (Button
    {:text "Error"
     :style :error
     :outline? true})])

(defscene icon-buttons
  [:div.flex.gap-4
   (Button
    {:text "Like"
     :icon (icons/icon :phosphor.regular/heart)})
   (Button
    {:text "Like"
     :icon-left (icons/icon :phosphor.regular/heart)})
   (Button
    {:text "Like"
     :icon-right (icons/icon :phosphor.regular/heart)})])

(defscene icon-only-button
  [:div.flex.gap-4
   (Button
    {:icon (icons/icon :phosphor.regular/heart)})
   (Button
    {:icon (icons/icon :phosphor.regular/heart)
     :style :ghost})])

(defscene links-as-buttons
  [:div.flex.gap-4
   (Button
    {:icon (icons/icon :phosphor.regular/heart)
     :href "/somewhere"
     :text "My likes"})])
