(ns repliweb.article
  (:require [clojure.string :as str]
            [datomic-type-extensions.api :as d]
            [phosphor.icons :as icons]
            [powerpack.markdown :as md]
            [repliweb.elements.layout :as layout]
            [repliweb.elements.showcase :as showcase]
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
          (icons/render :phosphor.bold/caret-right {:size "16px"})]
         [:a.menu-item
          {:href (:page/uri page)}
          (:page/title page)
          (icons/render :phosphor.bold/caret-right {:size "16px"})])])]])

(defn h3 [text]
  [:h3.text-whitish.mb-2 {:class "ml-4"} text])

(defn menu [db page]
  [:aside.basis-60.shrink-0
   (h3 "Guides")
   (navlist page (pages-by-kind db :page.kind/guide))
   (h3 "Tutorials")
   (navlist page (pages-by-kind db :page.kind/tutorial))
   (h3 "Articles")
   (navlist page (pages-by-kind db :page.kind/article))])

(def page-kind->text
  {:page.kind/guide "Guide"
   :page.kind/tutorial "Tutorial"
   :page.kind/article "Article"})

(defn ^{:indent 2} layout [{:keys [app/db]} page & body]
  (layout/layout
   {:title (:page/title page)}
   (layout/header {:logo-url "/"})
   (if (= (:page/uri page) "/learn/")
     [:div.flex.pt-8.flex-col-reverse.md:flex-row.mb-16
      [:div.mx-4.md:m-0 (menu db page)]
      [:main.mx-8.grow
       body]]
     (list
      [:div.p-4.bg-base-200.items-center.flex.flex-row.gap-4.sticky.top-0.z-10
       [:button {:popovertarget "menu"}
        (icons/render :phosphor.regular/list {:size 24})]
       [:div.bg-base-200.m-0.px-0.absolute.h-full {:popover "auto" :id "menu"}
        (menu db page)]
       (page-kind->text (:page/kind page)) ": " (:page/title page)]
      [:main.mt-8.mb-16.mx-4.md:mx-0.fullscreen body]))))

(defn render-heading [block]
  (when-let [title (:block/title block)]
    [(case (:block/level block)
       1 :h1.h1
       2 :h2.h2
       3 :h3.h3
       :h4.h4) {:class #{"section-md"}
                :id (:block/id block)}
     [:a.group.relative {:href (str "#" (:block/id block))}
      [:span.absolute.-left-4.group-hover:visible.invisible "§ "]
      title]]))

(defn render-markdown
  ([md] (render-markdown nil md))
  ([block md]
   (when (not-empty md)
     [:div.prose.mt-4.section-md
      (cond-> {}
        (and (:block/id block) (nil? (:block/title block)))
        (assoc :id (:block/id block)))
      (render-heading block)
      (md/render-html md)])))

(def sizes
  {:small "section-sm"
   :medium "section-md"
   :large "section-lg"})

(defn render-ab [{:keys [lang title code example class]}]
  (cond
    code
    (showcase/render-code {::showcase/lang lang
                           ::showcase/title title
                           :class class}
      [code])

    example
    (let [[ns fn] (str/split example #"/")]
      [:div.flex.flex-col.flex-1
       (when title
         [:span.code-title title])
       [:div.p-4.flex.items-center.grow
        {:data-example-ns ns
         :data-example-fn fn}]])))

(defn render-block [block]
  (list
   (if (not-empty (:block/markdown block))
     (render-markdown block (:block/markdown block))
     (render-heading block))
   (when (:block/a-lang block)
     (showcase/render-showcase {::showcase/style :light
                                :class #{(or (sizes (:block/size block))
                                             "section-md")
                                         "my-6"}}
       [(render-ab {:lang (:block/a-lang block)
                    :title (:block/a-title block)
                    :code (:block/a-code block)
                    :example (:block/a-example block)})
        (render-ab {:lang (:block/b-lang block)
                    :title (:block/b-title block)
                    :code (:block/b-code block)
                    :example (:block/b-example block)
                    :class ["bg-base-100"]})]))
   (when (:block/code block)
     [:div.my-6
      (cond-> {:class (cond-> [(or (sizes (:block/size block))
                                   "section-md")]
                        (:block/alignment block) (conj (:block/alignment block))
                        (nil? (:block/alignment block)) (conj "mx-auto"))}
        (and (nil? (:block/markdown block))
             (:block/id block)) (assoc :id (:block/id block)))
      (showcase/render-code {::showcase/lang (:block/lang block)
                             ::showcase/title (:block/code-title block)
                             :class ["bg-base-200"]}
        [(:block/code block)])])))

(defn render-page [ctx page]
  (layout ctx page
    (typo/h1 {:class #{"section-md"}} (:page/title page))
    (->> (:page/blocks page)
         (sort-by :block/idx <)
         (map render-block))
    (render-markdown (:page/body page))))
