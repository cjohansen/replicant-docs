(ns repliweb.demos.main
  (:require [repliweb.demos.searchable-media-list :as sml]))

(defn main []
  (doseq [el (seq (js/document.querySelectorAll "[data-replicant-root]"))]
    (let [id (.getAttribute el "data-replicant-root")]
      (println "Booting up example" id)
      (case id
        "searchable-media-list"
        (sml/main el)

        (println "Unknown Replicant root" id)
        (js/console.log el)))))
