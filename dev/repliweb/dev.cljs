(ns repliweb.dev
  (:require [repliweb.demos.main :as main]))

(defonce ^:export kicking-out-the-jams
  (main/main))

(comment
  (set! *print-namespace-maps* false)
)
