(ns repliweb.prod
  (:require [repliweb.client :as main]))

(def ^:export kicking-out-the-jams
  (main/main))
