(ns repliweb.elements.examples
  (:require [phosphor.icons :as icons]))

(def videos
  [{:video/url "https://www.youtube.com/watch?v=6qnNtVdf08Q"
    :video/thumbnail "/images/parens1.png"
    :episode/number 1
    :episode/title "It Lives Again"}
   {:video/url "https://www.youtube.com/watch?v=CyveUnHzc7g"
    :video/thumbnail "/images/parens2.png"
    :episode/number 2
    :episode/title "Shambling Along"}
   {:video/url "https://www.youtube.com/watch?v=_6tVIijfRzQ"
    :video/thumbnail "/images/parens3.png"
    :episode/number 3
    :episode/title "Stumbling out of the Graveyard"}])

(defn video->media-data [episode]
  {:thumbnail
   {:image (:video/thumbnail episode)
    :alt (str "Watch video: " (:episode/title episode))
    :url (:video/url episode)
    :icon (icons/icon :phosphor.regular/play)
    :icon-size :small}

   :title (str "Episode " (:episode/number episode))
   :text (:episode/title episode)
   :url (:video/url episode)
   :theme "cupcake"

   :button
   {:title "Like video"
    :icon (icons/icon :phosphor.regular/heart)}})

(comment

  (repliweb.elements.media/Media
   {:thumbnail
    {:image "/images/data-driven.png"
     :alt "Watch talk: Stateless, Data-driven UIs"
     :url "/watch"
     :icon (icons/icon :phosphor.regular/play)}

    :title "Stateless, Data-driven UIs"
    :text "How to build data-driven components and what they're good at"
    :url "/watch"
    :theme "cupcake"

    :button
    {:title "Like video"
     :icon (icons/icon :phosphor.regular/heart)
     :actions [[:action/execute-command
                [:command/like-video {:video/id "bc231"}]]]}})

  (defn render [media-list]
    [:div
     (SearchInput
      {:on {:change :action/search-videos}})

     (MediaList media-list)])

  (r/set-dispatch!
   (fn [rd event-data]
     (let [e (:replicant/dom-event rd)]
       (case event-data
         :action/search-videos
         (->> videos
              (search-videos (.. e -target -value))
              videos->media-list
              render)

         (println "Unsupported action!" action args)))))

(defn media [{:keys [url thumbnail title playing?]}]
  [:div.media {:data-theme "cupcake"}
   [:a {:href url
        :class (when playing?
                 "spinner")}
    [:img
     {:src thumbnail
      :alt title}]]])

(media {:url "https://vimeo.com/861600197"
        :thumbnail "/images/data-driven.png"
        :playing? false})

[:div.media {:data-theme "cupcake"}
 [:a {:href "https://vimeo.com/861600197"
      :class nil}
  [:img {:src "/images/data-driven.png" :alt nil}]]]

(defn render-greeting [{:keys [user]}]
  [:div
   [:h1 "Hello Clojure enthusiast!"]
   (when user
     [:p "Nice to see you, " (:user/given-name user)])
   [:p "Hope all is well today!"]])


[:h1
 {:style
  {:font-family: "FuturaPT, helvetica, sans-serif"
   :font-weight 900
   :max-width 800}}
 "Hello!"]

[:div
 {:style {:opacity 1
          :height 100
          :width 200
          :background "#6180D2"
          :transition "opacity 0.25s,
                       height 0.25s,
                       width 0.25s,
                       backgroun 0.25s"}

  :replicant/mounting
  {:style {:opacity 0}}

  :replicant/unmounting
  {:style {:height 0
           :width 0
           :background "#76AF47"}}}]

(defn render-map [{:keys [places]}]
  [:div
   {:replicant/on-mount
    (fn [{:keys [replicant/node]}]
      (mount-map node))

    :replicant/on-render
    (fn [{:keys [replicant/node]}]
      (update-map-places node places))}])

(require '[replicant.dom :as r])

(r/set-dispatch!
 (fn [e hook-data]
   (when (= :replicant.trigger/life-cycle
            (:replicant/trigger e))
     (println "Life-cycle hook triggered!")
     (println "Life-cycle" (:replicant/life-cycle e))
     (println "Node:" (:replicant/node e))
     (println "Hook data:" hook-data))))

(defn render-map [{:keys [places]}]
  [:div
   {:replicant/on-render
    [::update-map-places places]}])

[:form
 [:label {:for "name"} "Name:"]
 [:input {:type "text"
          :name "name"
          :id "name"
          :replicant/key [:input "name"]}]]

[:ul
 (for [fruit ["Banana" "Apple" "Orange"]]
   [:li fruit])])
