(ns repliweb.elements.layout)

(defn layout [{:keys [title]} & content]
  [:html {:data-theme "dark"}
   [:head
    [:title title]
    [:meta {:name "theme-color" :content "#202020"}]]
   [:body
    content]])
