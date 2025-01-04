(ns repliweb.article
  (:require [datomic-type-extensions.api :as d]
            [phosphor.icons :as icons]
            [repliweb.elements.layout :as layout]
            [repliweb.elements.typography :as typo]))

(defn pages-by-kind [db kind]
  (->> (d/q '[:find [?a ...]
              :in $ ?kind
              :where [?a :page/kind ?kind]]
            db kind)
       (map #(d/entity db %))
       (sort-by :page/order)))

(defn navlist [current-page pages]
  [:nav.mb-8
   [:ol
    (for [page pages]
      [:li
       (if (= (:page/uri current-page) (:page/uri page))
         [:span.menu-item.menu-item-selected
          (:page/title page)
          (icons/render :phosphor.bold/caret-right {:size 16})]
         [:a.menu-item
          {:href (:page/uri page)}
          (:page/title page)
          (icons/render :phosphor.bold/caret-right {:size 16})])])]])

(defn h3 [text]
  [:h3.uppercase.mb-2 {:class "ml-4"} text])

(defn ^{:indent 2} layout [{:keys [app/db]} page & body]
  (layout/layout
   {:title (:page/title page)}
   (layout/header {:logo-url "/"})
   [:div.flex.pt-8
    [:aside.basis-64
     (h3 "Guides")
     (navlist page (pages-by-kind db :page.kind/guide))
     (h3 "Tutorials")
     (navlist page (pages-by-kind db :page.kind/tutorial))
     (h3 "Articles")
     (navlist page (pages-by-kind db :page.kind/article))]
    [:main.mx-8.max-w.max-w-screen-md
     body]]))

(defn render-page [ctx page]
  (layout ctx {:title (:page/title page)}
    (typo/h1 (:page/title page))))

(defn render-index [ctx page]
  (layout ctx page
    (typo/h2 "Learn Replicant")))
