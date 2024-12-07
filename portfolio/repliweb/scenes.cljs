(ns repliweb.scenes
  (:require [portfolio.ui :as ui]
            [replicant.dom :as replicant]
            [repliweb.button-scenes]
            [repliweb.media-scenes]
            [repliweb.thumbnail-scenes]))

:repliweb.button-scenes/keep
:repliweb.media-scenes/keep
:repliweb.thumbnail-scenes/keep

(replicant/set-dispatch! #(prn %3))

(defonce app
  (ui/start!
   {:config
    {:css-paths ["/tailwind.css"]
     :canvas-path "canvas.html"
     :background/options
     [{:id :dark
       :title "Dark"
       :value {:background/background-color "#202020"
               :background/document-class "dark"}}]}}))
