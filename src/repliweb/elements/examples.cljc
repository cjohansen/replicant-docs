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

  )
