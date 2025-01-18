(ns repliweb.core
  (:require [clojure.string :as str]
            [powerpack.highlight :as highlight]
            [repliweb.article :as article]
            [repliweb.frontpage :as frontpage]))

(defn create-txes [file-name content]
  (if (str/ends-with? file-name ".md")
    [(assoc (first content) :page/blocks
            (->> (next content)
                 (map-indexed (fn [idx block]
                                (-> block
                                    (dissoc :page/uri)
                                    (assoc :block/idx idx))))))]
    content))

(defn render-page [context page]
  (if-let [f (case (:page/kind page)
               :page.kind/article article/render-page
               :page.kind/frontpage frontpage/render-page
               :page.kind/guide article/render-page
               :page.kind/tutorial article/render-page
               nil)]
    (f context page)
    [:h1 "Page not found 🤷‍♂️"]))

(defn create-app [env]
  (cond-> {:site/title "Replicant"
           :powerpack/render-page #'render-page
           :powerpack/port 4444
           :powerpack/log-level :debug
           :powerpack/content-file-suffixes ["md" "edn"]
           :powerpack/dev-assets-root-path "public"
           :powerpack/create-ingest-tx #'create-txes

           :optimus/bundles {"/styles.css"
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

           :powerpack/build-dir "target/site"}

    (= :build env)
    (assoc :site/base-url "https://replicant.io")

    (= :dev env) ;; serve figwheel compiled js
    (assoc :powerpack/dev-assets-root-path "public")

    :then
    highlight/install))
