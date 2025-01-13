(ns repliweb.demos.oneoffs)

(defn button-click-example []
  [:button.btn
   {:on {:click (fn [_]
                  #?(:cljs (js/alert "Hello!")))}}
   "Click it"])
