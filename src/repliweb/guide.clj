(ns repliweb.guide
  (:require [repliweb.article :as article]
            [repliweb.elements.typography :as typo]))

(defn render-page [ctx page]
  (article/layout ctx page
    (typo/h1 (:page/title page))))
