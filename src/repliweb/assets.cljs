(ns repliweb.assets
  (:require-macros [repliweb.assets :refer [get-asset-paths]]))

(def asset-paths (get-asset-paths))

(defn get-asset-path [asset]
  (get asset-paths asset asset))
