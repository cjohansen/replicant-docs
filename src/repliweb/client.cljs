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

(defn add-copy-button [code]
  (let [button (or (.querySelector code ".copy-code")
                   (js/document.createElement "button"))]
    (set! (.-className button) "copy-code btn btn-neutral btn-xs absolute top-2 right-2")
    (set! (.-innerHTML button) "copy")
    (.appendChild (.-parentNode code) button)))

(defn copy-code [e]
  (when (some-> e .-target .-classList (.contains "copy-code"))
    (let [button (.-target e)
          content (.-innerHTML button)
          range (js/document.createRange)
          code (.querySelector (.-parentNode button) "code")]
      (.preventDefault e)
      (if js/navigator.clipboard
        (.writeText (.-clipboard js/navigator) (.trim (str (.-textContent code))))
        (do
          (.selectNodeContents range code)
          (let [selection (js/window.getSelection)]
            (.removeAllRanges selection)
            (.addRange selection range)
            (try
              (.execCommand js/document "copy")
              (.removeRange selection code)
              (catch :default _)))))
      (set! (.-innerHTML button) "copied!")
      (js/setTimeout
       (fn []
         (set! (.-innerHTML button) content))
       2000))))

(defn main []
  (demos/main)
  (when-let [menu (js/document.querySelector "#menu")]
    (let [trigger (js/document.querySelector "[popovertarget=menu]")]
      (.addEventListener menu "toggle" #(position-menu % trigger) #js {:capture true, :passive true})))
  (let [codes (js/document.querySelectorAll "pre > code")]
    (doseq [code codes]
      (add-copy-button code))
    (when (seq codes)
      (.addEventListener js/document.body "click" copy-code))))
