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
  (set! (.. menu -style -top) (str (+ (:y (get-element-offset bar)) (.-offsetHeight bar)) "px"))
  (set! js/document.body.style.overflow "hidden"))

(defn close-menu [menu]
  (set! js/document.body.style.overflow "scroll"))

(defn position-menu [e trigger]
  (when-let [menu (.-target e)]
    (if (= "open" (.-newState e))
      (open-menu menu (.-parentNode trigger))
      (close-menu menu))))

(defn main []
  (demos/main)
  (when-let [menu (js/document.querySelector "#menu")]
    (let [trigger (js/document.querySelector "[popovertarget=menu]")]
      (.addEventListener menu "toggle" #(position-menu % trigger) #js {:capture true, :passive true}))))
