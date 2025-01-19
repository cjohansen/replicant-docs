(ns repliweb.export
  (:require [powerpack.export :as export]
            [repliweb.core :as repliweb]))

(defn ^:export export [& _args]
  (set! *print-namespace-maps* false)
  (export/export! (repliweb/create-app :prod)))
