(ns repliweb.dev
  (:require [datomic-type-extensions.api :as d]
            [powerpack.dev :as dev]
            [powerpack.export :as export]
            [repliweb.core :as repliweb]))

(defmethod dev/configure! :default []
  (repliweb/create-app :dev))

(comment
  (set! *print-namespace-maps* false)
  (export/export (repliweb/create-app :prod))
  )

(comment ;; s-:
  (dev/start)
  (dev/stop)
  (dev/reset)

  ;; (def db (d/db (:datomic/conn (dev/get-app))))

  ;; (d/q '[:find ?uri
  ;;        :where
  ;;        [_ :page/uri ?uri]]
  ;;      db)
  )
