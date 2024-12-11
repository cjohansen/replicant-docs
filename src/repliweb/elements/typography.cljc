(ns repliweb.elements.typography)

(defn ^{:indent 1} h1
  ([text] (h1 nil text))
  ([attrs & body]
   (into [:h1.text-xl.lg:text-5xl.mb-2.dark:text-whitish attrs] body)))

(defn ^{:indent 1} h2
  ([text] (h2 nil text))
  ([attrs & body]
   (into [:h2.text-xl.xl:text-3xl.lg:text-2xl.mb-2.dark:text-whitish attrs] body)))

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
