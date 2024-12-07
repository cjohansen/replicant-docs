(ns repliweb.elements.typography)

(defn h1
  ([text] (h1 nil text))
  ([attrs & body]
   (into [:h1.text-5xl.mb-2.dark:text-whitish attrs] body)))

(defn h2
  ([text] (h2 nil text))
  ([attrs & body]
   (into [:h2.text-3xl.mb-2.dark:text-whitish attrs] body)))

(defn p
  ([text] (p nil text))
  ([attrs & body]
   (into [:p.my-8.dark:text-whitish attrs] body)))

(defn lead
  ([text] (lead nil text))
  ([attrs & body]
   (into [:p.my-8.text-xl.dark:text-whitish attrs] body)))
