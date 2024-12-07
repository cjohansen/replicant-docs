(ns repliweb.elements.button
  (:require [phosphor.icons :as icons]))

(def btn-classes
  {:accent "btn-accent"
   :error "btn-error"
   :ghost "btn-ghost"
   :info "btn-info"
   :link "btn-link"
   :neutral "btn-neutral"
   :primary "btn-primary"
   :secondary "btn-secondary"
   :success "btn-success"
   :warning "btn-warning"
   })

(defn Button [{:keys [text style active? outline? block? glass?
                      icon icon-left icon-right actions url theme
                      size title]}]
  (let [left-icon (or icon icon-left)
        icon-size (case size
                    :large ["h-10" "w-10"]
                    :small ["h-4" "w-4"]
                    ["h-6" "w-6"])]
    [(if url :a.btn :div.btn)
     (cond-> {:class (cond-> []
                       style (conj (btn-classes style))
                       active? (conj "btn-active")
                       outline? (conj "btn-outline")
                       block? (conj "btn-block")
                       (nil? text) (conj "btn-circle")
                       glass? (conj "glass")
                       (= :large size) (conj "btn-lg")
                       (= :small size) (conj "btn-sm"))}
       actions (assoc :on {:click actions})
       url (assoc :href url)
       theme (assoc :data-theme theme)
       title (assoc :title title))
     (when left-icon
       (icons/render left-icon {:class icon-size}))
     text
     (when (and (nil? left-icon) icon-right)
       (icons/render icon-right {:class icon-size}))]))
