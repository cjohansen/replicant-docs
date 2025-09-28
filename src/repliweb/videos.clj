(ns repliweb.videos
  (:require [datomic-type-extensions.api :as d]
            [phosphor.icons :as icons]
            [powerpack.markdown :as md]
            [repliweb.article :as article]
            [repliweb.elements.layout :as layout]
            [repliweb.elements.media :refer [Media]]
            [repliweb.elements.typography :as typo]))

(defn get-video-series [db]
  (->> (d/q '[:find [?v ...]
              :where [?v :video-series/id]]
            db)
       (map #(d/entity db %))
       (sort-by :order/idx)))

(defn render-page [{:app/keys [db]} page]
  (layout/layout
   {:title (:page/title page)}
   (layout/header {:logo-url "/"})
   [:main.mt-8.mb-16.mx-4.md:mx-0.fullscreen.flex-1
    (typo/h1 {:class #{"section-md"}} (:page/title page))
    (article/render-blocks (:page/blocks page))
    (for [{:video-series/keys [title description videos]} (get-video-series db)]
      [:div.section-md.mb-8
       (typo/h2 title)
       [:div.prose (some-> description md/render-html)]
       [:div.mt-4
        (for [{:video/keys [title description url thumbnail]} (sort-by :order/idx videos)]
          (Media
           {:thumbnail
            {:image thumbnail
             :alt (str "Watch " title)
             :url url
             :icon (icons/icon :phosphor.regular/play)
             :icon-size :small}
            :title title
            :text description
            :url url}))]])]))

(comment
  (set! *print-namespace-maps* false)
  (def db (d/db (:datomic/conn (powerpack.dev/get-app))))
  (map :video-series/title (get-video-series db))
)
