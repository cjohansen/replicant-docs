(ns repliweb.core
  (:require [clojure.string :as str]
            [powerpack.highlight :as highlight]
            [repliweb.article :as article]
            [repliweb.assets :as assets]
            [repliweb.frontpage :as frontpage]
            [repliweb.videos :as videos]))

(defn create-txes [file-name content]
  (prn file-name)
  (if (str/ends-with? file-name ".md")
    [(assoc (first content) :page/blocks
            (->> (next content)
                 (map-indexed (fn [idx block]
                                (-> block
                                    (dissoc :page/uri)
                                    (assoc :order/idx idx))))))]
    content))

(defn render-page [context page]
  (if-let [f (case (:page/kind page)
               :page.kind/article article/render-page
               :page.kind/frontpage frontpage/render-page
               :page.kind/guide article/render-page
               :page.kind/tutorial article/render-page
               :page.kind/videos videos/render-page
               nil)]
    (f context page)
    [:h1 "Page not found 🤷‍♂️"]))

(defn create-app [env]
  (cond-> {:site/title "Replicant"
           :powerpack/port 4444
           :powerpack/render-page #'render-page
           :powerpack/create-ingest-tx #'create-txes
           :powerpack/log-level :debug
           :powerpack/content-file-suffixes ["md" "edn"]
           :powerpack/dev-assets-root-path "public"
           :powerpack/build-dir "target/site"}

    (= :build env)
    (assoc :site/base-url "https://replicant.io")

    (= :dev env) ;; serve figwheel compiled js
    (assoc :powerpack/dev-assets-root-path "public")

    :then
    (merge assets/config)

    :then
    highlight/install))
