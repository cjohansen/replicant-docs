(ns repliweb.client
  (:require [repliweb.demos.main :as demos]))

(defn get-element-offset [el]
  (loop [x 0
         y 0
         el el]
    (if (nil? el)
      {:x x :y y}
      (recur (+ x (or (.-offsetLeft el) 0))
             (+ y (or (.-offsetTop el) 0))
             (.-offsetParent el)))))

(defn open-menu [menu bar]
  (let [offset (+ (:y (get-element-offset bar)) (.-offsetHeight bar))
        padding (- (.-offsetHeight (.-firstChild menu)) (.-offsetHeight menu))]
    (set! (.. menu -style -top) (str offset "px"))
    (when (< 0 padding)
      (set! (.. menu -style -paddingBottom) (str padding "px")))
    (set! js/document.body.style.overflow "hidden")))

(defn close-menu [_]
  (set! js/document.body.style.overflow "scroll"))

(defn position-menu [^js e trigger]
  (when-let [menu (.-target e)]
    (if (= "open" (.-newState e))
      (open-menu menu (.-parentNode trigger))
      (close-menu menu))))

(defn main []
  (demos/main)
  (when-let [menu (js/document.querySelector "#menu")]
    (let [trigger (js/document.querySelector "[popovertarget=menu]")]
      (.addEventListener menu "toggle" #(position-menu % trigger) #js {:capture true, :passive true}))))
