(ns repliweb.export
  (:require [clojure.string :as str]
            [powerpack.export :as export]
            [repliweb.core :as repliweb]))

(defn check-link [_ _ {:keys [href]}]
  ;; Problem: The TOC makes ids for markdown headings and links to them. But the
  ;; rendered Markdown doesn't have ids in them. This _should_ be fixed by
  ;; adding ids to the headings, e.g. with a markup
  ;; processor (https://github.com/cjohansen/powerpack/?tab=readme-ov-file#post-processing),
  ;; but this cheat will do for now.
  (str/starts-with? href "#"))

(defn ^:export export [& _args]
  (set! *print-namespace-maps* false)
  (export/export! (repliweb/create-app :prod) {:link-ok? check-link}))
