(ns repliweb.elements.layout
  (:require [repliweb.elements.logos :as logos]
            [phosphor.icons :as icons]))

(defn layout [{:keys [title]} & content]
  [:html {:data-theme "dark"}
   [:head
    [:title title]
    [:meta {:name "theme-color" :content "#202020"}]]
   [:body
    content]])

(defn header [& [{:keys [logo-url]}]]
  [:div.flex.justify-between.p-4.items-center
   [:div.w-8
    (if logo-url
      [:a {:href logo-url} (logos/replicant)]
      (logos/replicant))]
   [:div.flex.items-center.gap-4.text-sm
    [:a.hover:underline {:href "/learn/"} "Learn"]
    [:a.hover:underline {:href "https://cljdoc.org/d/no.cjohansen/replicant/"} "API Reference"]
    [:div.w-6 [:a {:href "https://clojurians.slack.com/archives/C06JZ4X334N"
                   :title "Replicant on the Clojurians Slack"}
               (icons/render :phosphor.regular/slack-logo)]]
    [:div.w-6 [:a {:href "https://github.com/cjohansen/replicant"
                   :title "Replicant on Github"}
               (logos/github)]]]])
