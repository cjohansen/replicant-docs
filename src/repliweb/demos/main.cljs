(ns repliweb.demos.main
  (:require [replicant.dom :as r]
            [repliweb.demos.data-driven-transitions :as ddt]
            [repliweb.demos.searchable-media-list :as sml]))

(defn main []
  (let [ns->store {"repliweb.demos.data-driven-transitions" (atom {})
                   "repliweb.demos.searchable-media-list" (atom {})}]
    (r/set-dispatch!
     (fn [replicant-data [action & args]]
       (let [ns (namespace action)
             f (case ns
                 "repliweb.demos.data-driven-transitions" ddt/handle-action
                 "repliweb.demos.searchable-media-list" sml/handle-action)]
         (f (get ns->store ns) replicant-data action args))))

    (doseq [el (seq (js/document.querySelectorAll "[data-example-ns]"))]
      (let [ns (.getAttribute el "data-example-ns")]
        (println "Booting up example" ns)
        (case ns
          "repliweb.demos.searchable-media-list"
          (sml/main el (ns->store ns))

          "repliweb.demos.data-driven-transitions"
          (ddt/main el (ns->store ns))

          (println "Unknown example ns" ns)
          (js/console.log el))))))
