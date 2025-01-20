(ns repliweb.assets
  (:require [optimus.optimizations :as optimizations]
            [powerpack.assets :as assets]))

(def asset-config
  {:optimize? (not= "" (System/getenv "optimize_assets"))})

(defn optimize-assets [assets & [options]]
  (if (:optimize? asset-config)
    (optimizations/all assets options)
    (optimizations/none assets options)))

(def config
  {:optimus/bundles {"/styles.css"
                     {:public-dir "public"
                      :paths ["/tailwind.css"
                              "/code.css"]}

                     "/app.js"
                     {:public-dir "public"
                      :paths ["/js/compiled/app.js"]}
                     }

   :optimus/assets [{:public-dir "public"
                     :paths [#"\.png$"
                             #"\.jpg$"
                             #"\.svg$"
                             #"\.gif$"
                             #"\.ico$"]}]

   :optimus/options {}})

(def assets (optimize-assets (assets/get-assets config)))

(defmacro get-asset-paths []
  (->> assets
       (map (juxt :original-path :path))
       (into {})))

(defn get-asset-path [path]
  (->> assets
       (filter (comp #{path} :original-path))
       first
       :path))
