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
       (map #(d/entity db %))))

(defn navlist [attrs current-page pages]
  [:nav attrs
   [:ol
    (for [page (sort-by :page/order pages)]
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
  [:h3.text-whitish.mb-2.ml-4 text])

(defn h4 [text]
  [:h4.text-xs.text-whitish.mb-2.ml-4 text])

(def tutorial-category->text
  {:tutorial.category/getting-started "Getting started"
   :tutorial.category/basics "Basics"
   :tutorial.category/networking "Networking"
   :tutorial.category/forms "Forms"
   :tutorial.category/interop "JavaScript interop"
   :tutorial.category/aliases "Aliases"})

(defn menu [db page]
  [:aside.basis-60.shrink-0
   (h3 "Guides")
   (navlist {:class "mb-8"} page (pages-by-kind db :page.kind/guide))
   (h3 "Tutorials")
   (let [by-category (->> (pages-by-kind db :page.kind/tutorial)
                          (group-by :page/category)
                          (sort-by (comp :page/order first second)))]
     (for [[category tutorials] by-category]
       (list
        (h4 (tutorial-category->text category))
        (navlist (if (= category (first (last by-category)))
                   {:class "mb-8"}
                   {:class "mb-2"}) page tutorials))))
   (h3 "Articles")
   (navlist {:class "mb-8"} page (pages-by-kind db :page.kind/article))])

(def page-kind->text
  {:page.kind/guide "Guide"
   :page.kind/tutorial "Tutorial"
   :page.kind/article "Article"})

(defn blocks->toc
  "Extract table of contents from blocks structure"
  [blocks]
  (when (seq blocks)
    (->> blocks
      (filter :block/level)
      (filter #(#{2 3} (:block/level %)))
      (sort-by :order/idx <)
      (map (fn [{:block/keys [id title level]}]
             {:id id
              :text title
              :depth (case level 2 0 3 1)})))))

(defn md->toc [md]
  (when (not-empty md)
    (let [lines (str/split-lines md)
          heading-pattern #"^(#{2,3})\s+(.+)$"]
      (->> lines
           (map (fn
                  [line]
                  (when-let [match (re-find heading-pattern line)]
                    (let [hashes (nth match 1)
                          text (str/trim (nth match 2))
                          depth (case (count hashes)
                                  2 0 ; h2 -> depth 0
                                  3 1 ; h3 -> depth 1
                                  nil)
                          ;; Create id from text (similar to how markdown processors do it)
                          id (-> text
                                 str/lower-case
                                 (str/replace #"[^a-zA-Z0-9\s-]" "")
                                 (str/replace #"\s+" "-")
                                 (str/replace #"-+" "-")
                                 (str/replace #"^-|-$" ""))]
                      (when depth
                        {:id id :text text :depth depth})))))
           (filter some?)))))

(defn page->toc [page]
  (let [blocks-toc (blocks->toc (:page/blocks page))
        markdown-toc (md->toc (:page/body page))]
    (->> (concat blocks-toc markdown-toc)
         (filter some?))))

;; Couldn't find this icon as part of phosphor and I think it fits better than a
;; normal menu icon.
(defn render-toc-icon []
  [:svg.h-3.w-3 {:width "16" :height "16" :viewBox "0 0 16 16" :fill "none"
                             :stroke "currentColor" :stroke-width "2" :xmlns "http://www.w3.org/2000/svg"}
   [:path {:d "M2.44434 12.6665H13.5554" :stroke-linecap "round" :stroke-linejoin "round"}]
   [:path {:d "M2.44434 3.3335H13.5554" :stroke-linecap "round" :stroke-linejoin "round"}]
   [:path {:d "M2.44434 8H7.33323" :stroke-linecap "round" :stroke-linejoin "round"}]])

(defn render-toc
  "Render table of contents component"
  [toc-entries]
  (when (seq toc-entries)
    [:div.text-sm.leading-6.overflow-y-auto.space-y-2.pb-4.-mt-10.pt-10.text-base-content#table-of-contents {:class ["w-[19rem]"]}
     [:div.font-medium.flex.items-center.space-x-2
      (render-toc-icon)
      [:span "On this page"]]
     [:ul#table-of-contents-content
      (for [{:keys [id text depth]} toc-entries]
        [:li {:data-depth depth}
         [:a {:href (str "#" id)
              :style {:margin-left (str (* depth 1) "rem")}
              :class (into ["hover:text-primary" :transition-300] (if (= depth 0)
                                                                    ["py-1" "block" "font-medium"]
                                                                    ["group" "flex" "items-start" "py-1" "whitespace-pre-wrap"]))}
          text]])]]))

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
      [:div.md:flex.relative
       [:main.mt-8.mb-16.mx-4.md:mx-0.fullscreen.flex-1
        body]
       (when-let [toc-entries (page->toc page)]
         [:aside.top-12.self-start.sticky.hidden.xl:block.pt-8
          (render-toc toc-entries)])]))))

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

(defn render-blocks [blocks]
  (->> blocks
       (sort-by :order/idx <)
       (map render-block)))

(defn render-page [ctx page]
  (layout ctx page
    (typo/h1 {:class #{"section-md"}} (:page/title page))
    (render-blocks (:page/blocks page))
    (render-markdown (:page/body page))))
