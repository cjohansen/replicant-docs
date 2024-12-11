(ns repliweb.core
  (:require [powerpack.highlight :as highlight]
            [repliweb.frontpage :as frontpage]))

(defn render-page [context page]
  (if-let [f (case (:page/kind page)
               :page.kind/frontpage frontpage/render-page
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

           :optimus/bundles {"/styles.css"
                             {:public-dir "public"
                              :paths ["/tailwind.css"
                                      "/code.css"]}

                             "/app.js"
                             {:public-dir "public"
                              :paths ["/js/compiled/app.js"]}}

           :optimus/assets [{:public-dir "public"
                             :paths [#"\.png$"
                                     #"\.jpg$"]}]

           :powerpack/build-dir "target/site"}

    (= :build env)
    (assoc :site/base-url "https://replicant.io")

    (= :dev env) ;; serve figwheel compiled js
    (assoc :powerpack/dev-assets-root-path "public")

    :then
    highlight/install))
