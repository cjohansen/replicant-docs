(ns repliweb.elements.typography)

(defn ^{:indent 1} h1
  ([text] (h1 nil text))
  ([attrs & body]
   (into [:h1.h1.dark:text-whitish attrs] body)))

(defn ^{:indent 1} h2
  ([text] (h2 nil text))
  ([attrs & body]
   (into [:h2.h2.lg:text-2xl.mb-2.dark:text-whitish attrs] body)))

(defn ^{:indent 1} h3
  ([text] (h3 nil text))
  ([attrs & body]
   (into [:h3.h3.lg:text-1xl.mb-2.dark:text-whitish attrs] body)))

(defn ^{:indent 1} p
  ([text] (p nil text))
  ([attrs & body]
   (into [:p.my-8.dark:text-whitish attrs] body)))

(defn ^{:indent 1} lead
  ([text] (lead nil text))
  ([attrs & body]
   (into [:p.my-8.text-xl.dark:text-whitish attrs] body)))

(defn a [attrs & children]
  (into [:a.text-primary.underline.hover:no-underline attrs] children))

(defn code
  ([text] (code nil text))
  ([attrs text]
   (into [:code.text-secondary attrs text])))
