(ns repliweb.dev
  (:require [repliweb.client :as main]))

(def ^:export kicking-out-the-jams
  (main/main))

(comment
  (set! *print-namespace-maps* false)
)
