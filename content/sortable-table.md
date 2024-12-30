:page/uri /tutorials/sortable-table/
:page/title Building a sortable table with Replicant
:page/body

In this tutorial, we will build a sortable table, and eventually extract a
reusable data-driven abstraction as a Replicant [alias](/aliases/). The goal is
to create an abstraction that deals with the details of getting a dataset into a
table and sorting it, without restricting your options for formatting and visual
presentation.

The basis for this tutorial is a stripped-down version of the code from [the
routing tutorial](/tutorials/routing/) that only manages query and hash/fragment
parameters. The setup also uses [tailwindcss](https://tailwindcss.com).

If you want to follow along, grab [the setup on
Github](https://github.com/cjohansen/replicant-sortable-table/tree/setup), then
run `make tailwind` in a terminal, fire up your editor and start a REPL.

## A basic table

We'll start by listing out our dataset in a basic table. For this tutorial,
we're working with the [40 top ranking boardgames on
boardgamegeek.com](https://boardgamegeek.com/browse/boardgame). The list is
available in `boardgames.data`:

```clj
(ns boardgames.data)

(def data
  {:boardgames
   [{:bgg/ranking 1
     :bgg/thumbnail "https://cf.geekdo-images.com/x3zxjr-Vw5iU4yDPg70Jgw__micro/img/4Od3GYCiqptga0VbmyumPbJlBsU=/fit-in/64x64/filters:strip_icc()/pic3490053.jpg"
     :boardgame/title "Brass: Birmingham"
     :boardgame/release-year 2018
     :bgg/geek-rating 8.409
     :bgg/average-rating 8.59
     :bgg/num-voters 49313}

    ,,,]})
```

Like in the routing tutorial, this data is fed to the UI render functions as
`:boardgames` in the `state` argument. This little indirection means our UI
doesn't know or care where data comes from, which gives us a lot of flexibility.

Here's how we'll render a basic table with some Tailwind flair:

```clj
(ns boardgames.ui)

(defn render-page [{:keys [boardgames]} location]
  [:div.p-8.max-w-screen-lg
   [:h1.text-2xl.font-serif.mb-4 "Boardgames ranked by Boardgamegeek"]
   [:table.w-full
    [:thead
     [:tr.border-b.bg-base-200
      [:th.py-2.text-left.px-4 "Title"]
      [:th.py-2.text-left.pr-4 "Released"]
      [:th.py-2.text-left.pr-4 "Ranking"]
      [:th.py-2.whitespace-nowrap.text-left.pr-4 "Geek rating"]
      [:th.py-2.whitespace-nowrap.text-left.pr-4 "Avg. rating"]
      [:th.py-2.text-right.px-4 "Voters"]]]
    [:tbody
     (for [game boardgames]
       [:tr.border-b.border-1 {:replicant/key (:bgg/ranking game)}
        [:th.py-2.px-4.text-left (:boardgame/title game)]
        [:td.py-2.pr-4.text-left (:boardgame/release-year game)]
        [:td.py-2.pr-4.text-center (:bgg/ranking game)]
        [:td.py-2.pr-4.text-left (:bgg/geek-rating game)]
        [:td.py-2.pr-4.text-left (:bgg/average-rating game)]
        [:td.py-2.px-4.text-right (:bgg/num-voters game)]])]]])
```

Because this function will primarily reorder rows renders, we add a
[key](/keys/) to each one so Replicant will know to reorder DOM elements instead
of recreating them.

## Indicating sorting column

We will now add visual indicators to the active sort column header:

```clj
[:th.py-2.text-left.pr-4 "▼ Ranking"]
```

The data happens to be sorted by ranking by default, but we can make that
explicit:

```clj
(for [game (sort-by :bgg/ranking < boardgames)]
  [:tr.border-b.border-1
   ,,,])
```

We want clicks on the currently sorted column header to reverse the order. We
can set a hash parameter `sort-order` to make this happen. That means we also
have to check this parameter for the chosen order before sorting.

Here's a function that finds the current sorting order. It is written such that
it provides a default if the `sort-order` hash parameter isn't set -- or if it
has any value except `desc` or `asc`:

```clj
(defn get-sort-order [location]
  (if (= "desc" (-> location :location/hash-params :sort-order))
    "desc"
    "asc"))
```

To make it easy to control the sort order, we'll introduce a mapping from the
chosen order to comparator function:

```clj
(def comparators
  {"desc" >
   "asc" <})
```

Now we can update the rendering function:

```clj
(defn render-page [{:keys [boardgames]} location]
  (let [sort-order (get-sort-order location)]
    [:div.p-8.max-w-screen-lg
     ,,,
     [:table.w-full
      [:thead
       [:tr.border-b.bg-base-200
        ,,,
        [:th.py-2.text-left.pr-4
         (if (= "desc" sort-order) "▼" "▲") " Ranking"]
        ,,,]]
      [:tbody
       (for [game (sort-by :bgg/ranking (comparators sort-order) boardgames)]
         [:tr.border-b.border-1
          ,,,])]]]))
```

If all goes well, everything should stay the same. What a win, eh? Let's make
the header clickable to reverse the order. First we'll add another handy map:

```clj
(def reverse-order
  {"desc" "asc"
   "asc" "desc"})
```

Then we'll use the routing alias to change the URL's hash params:

```clj
[:ui/a.hover:underline.cursor-pointer
 {:ui/location (assoc-in location [:location/hash-params :sort-order]
                         (reverse-order sort-order))}
 (if (= "desc" sort-order) "▼" "▲") " Ranking"]
```

## Changing sort columns

The next task is to change the sorting column. This is similar to what we just
did, so we'll start by deciding on the current sorting column. This is a little
bit trickier than finding the order, for two reasons.

1. There are more valid options, so selecting a valid one is a bit more work.
2. We need to map the selected parameter value to a namespaced keyword.

To help solve both these, we'll introduce a map of possible param values to
appropriate sorting key:

```clj
(def sort-columns
  {"ranking" :bgg/ranking
   "title" :boardgame/title
   "year" :boardgame/release-year
   "rating" :bgg/geek-rating
   "average" :bgg/average-rating
   "voters" :bgg/num-voters})

(defn get-sort-column [location]
  (or (get sort-columns (-> location :location/hash-params :sort-column))
      (get sort-columns "ranking")))
```

We then use this new function to sort the dataset:

```clj
(defn render-page [{:keys [boardgames]} location]
  (let [sort-order (get-sort-order location)
        sort-column (get-sort-column location)]
    [:div.p-8.max-w-screen-lg
     ,,,
     [:table.w-full
      [:thead
       ,,,]
      [:tbody
       (for [game (sort-by sort-column (comparators sort-order) boardgames)]
         [:tr.border-b.border-1 {:replicant/key (:bgg/ranking game)}
          ,,,])]]]))
```

Next up, we will make it possible to change the sorting column to the average
rating by clicking the header:

```clj
[:th.py-2.whitespace-nowrap.text-left.pr-4
 [:ui/a {:ui/location (assoc-in location [:location/hash-params :sort-column]
                                "average")}
  "Avg. rating"]]
```

Clicking this header will sort the table by average rating, but now the headers
are out of sync. We have to also move the marker, and make the link _either_
change sort order or sort column (and possibly reset the sort order). Oh my.

Here's the updated header:

```clj
[:th.py-2.whitespace-nowrap.text-left.pr-4
 [:ui/a
  {:ui/location
   (if (= :bgg/average-rating sort-column)
     (assoc-in location [:location/hash-params :sort-order]
               (reverse-order sort-order))
     (assoc-in location [:location/hash-params :sort-column]
               "average"))}
  (when (= :bgg/average-rating sort-column)
    (if (= "desc" sort-order) "▼ " "▲ "))
  "Avg. rating"]]
```

This now works, but the ranking header is still stuck. Basically we need this
piece of logic on the header for every sortable column. Time to extract a
function:

```clj
(defn render-header-link [location k param-v label]
  (let [sort-order (get-sort-order location)
        sort-column (get-sort-column location)]
    [:ui/a
     {:ui/location
      (if (= k sort-column)
        (assoc-in location [:location/hash-params :sort-order]
                  (reverse-order sort-order))
        (assoc-in location [:location/hash-params :sort-column]
                  param-v))}
     (when (= k sort-column)
       (if (= "desc" sort-order) "▼ " "▲ "))
     label]))
```

In order to avoid a "everything but the kitchen sink" style signature, this
helper finds the sort order and column from the location. This isn't ideal, but
let's not worry about performance until we have to.

We can use this helper on all the headers, and end up with a neatly sortable
table:

```clj
(defn render-page [{:keys [boardgames]} location]
  (let [sort-order (get-sort-order location)
        sort-column (get-sort-column location)]
    [:div.p-8.max-w-screen-lg
     [:h1.text-2xl.font-serif.mb-4 "Boardgames ranked by Boardgamegeek"]
     [:table.w-full
      [:thead
       [:tr.border-b.bg-base-200
        [:th.py-2.text-left.px-4
         (render-header-link location :boardgame/title "title" "Title")]
        [:th.py-2.text-left.pr-4
         (render-header-link location :boardgame/release-year "year" "Released")]
        [:th.py-2.text-left.pr-4
         (render-header-link location :bgg/ranking "ranking" "Ranking")]
        [:th.py-2.whitespace-nowrap.text-left.pr-4
         (render-header-link location :bgg/geek-rating "rating" "Geek rating")]
        [:th.py-2.whitespace-nowrap.text-left.pr-4
         (render-header-link location :bgg/average-rating "average" "Avg. rating")]
        [:th.py-2.text-right.px-4
         (render-header-link location :bgg/num-voters "voters" "Voters")]]]
      [:tbody
       (for [game (sort-by sort-column (comparators sort-order) boardgames)]
         [:tr.border-b.border-1 {:replicant/key (:bgg/ranking game)}
          [:th.py-2.px-4.text-left (:boardgame/title game)]
          [:td.py-2.pr-4.text-left (:boardgame/release-year game)]
          [:td.py-2.pr-4.text-center (:bgg/ranking game)]
          [:td.py-2.pr-4.text-left (:bgg/geek-rating game)]
          [:td.py-2.pr-4.text-left (:bgg/average-rating game)]
          [:td.py-2.px-4.text-right (:bgg/num-voters game)]])]]]))
```

Both `thead` and `tbody` loop through the same attributes in order to build a
row. Currently we're relying on these two pieces of code matching up. It would
be neat if we could make both the headers and the body rows be generated from
the same data to guarantee that they correspond.

Let's gather all the column information in a data structure:

```clj
(def columns
  [{:f :boardgame/title, :id "title", :label "Title"}
   {:f :boardgame/release-year, :id "year", :label "Released"}
   {:f :bgg/ranking, :id "ranking", :label "Ranking"}
   {:f :bgg/geek-rating, :id "rating", :label "Geek rating"}
   {:f :bgg/average-rating, :id "average", :label "Avg. rating"}
   {:f :bgg/num-voters, :id "voters", :label "Voters"}])
```

I renamed `k` to `:f` because if we use it as a function, it isn't limited to
being a keyword. If you wanted to display the year with the title, you could add
a column like so:

```clj
{:f #(str (:boardgame/title %) " (" (:boardgame/release-year) ")")
 :k "title-year"
 :label "Title"}
```

To use this new data structure, we must update `get-sort-column` to look in it
instead of the map we made before:

```clj
(defn get-sort-column [location]
  (let [param (-> location :location/hash-params :sort-column)]
    (or (first (filter (comp #{param} :id) columns))
        (first (filter (comp #{"ranking"} :id) columns)))))
```

The updated render function is looking a little more regular, and at the very
least the headers are now guaranteed to correspond to the body rows:

```clj
(defn render-page [{:keys [boardgames]} location]
  (let [sort-order (get-sort-order location)
        sort-column (get-sort-column location)]
    [:div.p-8.max-w-screen-lg
     [:h1.text-2xl.font-serif.mb-4 "Boardgames ranked by Boardgamegeek"]
     [:table.w-full
      [:thead
       [:tr.border-b.bg-base-200
        [:th.py-2.text-left.px-4
         (render-header-link location (nth columns 0))]
        [:th.py-2.text-left.pr-4
         (render-header-link location (nth columns 1))]
        [:th.py-2.text-left.pr-4
         (render-header-link location (nth columns 2))]
        [:th.py-2.whitespace-nowrap.text-left.pr-4
         (render-header-link location (nth columns 3))]
        [:th.py-2.whitespace-nowrap.text-left.pr-4
         (render-header-link location (nth columns 4))]
        [:th.py-2.text-right.px-4
         (render-header-link location (nth columns 5))]]]
      [:tbody
       (for [game (sort-by (:f sort-column) (comparators sort-order) boardgames)]
         [:tr.border-b.border-1 {:replicant/key (:bgg/ranking game)}
          [:th.py-2.px-4.text-left ((:f (nth columns 0)) game)]
          [:td.py-2.pr-4.text-left ((:f (nth columns 1)) game)]
          [:td.py-2.pr-4.text-center ((:f (nth columns 2)) game)]
          [:td.py-2.pr-4.text-left ((:f (nth columns 3)) game)]
          [:td.py-2.pr-4.text-left ((:f (nth columns 4)) game)]
          [:td.py-2.px-4.text-right ((:f (nth columns 5)) game)]])]]]))
```

It would be nice if we could loop the columns to create rows. Unfortunately,
custom formatting is in our way. But what if we could separate the formatting
wrappers from the content?

## Adding aliases

To increase the abstraction level, we will create aliases for the table,
thead/tbody and cells. Since aliases evaluate top-down, parent aliases can
manipulate their content, which we can use to pass along data.

Let's start by defining a table alias. It will receive the columns and the
location. Create `src/boardgames/ui/sortable_table.cljc` with the following:

```clj
(ns boardgames.ui.sortable-table
  (:require [replicant.alias :refer [defalias]]
            [replicant.hiccup :as hiccup]))

(defn get-sort-order [location]
  (if (= "desc" (-> location :location/hash-params :sort-order))
    "desc"
    "asc"))

(defalias table [attrs children]
  (into                                                      ;; 1
   [:table attrs]                                            ;; 2
   (mapv #(hiccup/update-attrs                               ;; 3
           % assoc
           ::location (::location attrs)                     ;; 4
           ::columns (::columns attrs)
           ::sort-order (get-sort-order (::location attrs))) ;; 5
         children)))
```

1. Avoid introducing more nesting around the children.
2. The sortable table can take arbitrary attributes for use with the table
   element.
3. `update-attrs` works like `update` for the attribute map of a hiccup node.
   Using this function means you don't have to worry about whether the node has
   explicit attributes (e.g. `[:tbody {:class "tbody"} [:tr ,,,]]`) or not (e.g.
   `[:tbody [:tr ,,,]]`).
4. Since Replicant will not try to render namespaced keys in the attributes map
   as DOM attributes, they are used to pass all the "technical" parameters
   (location, columns).
5. We find the current sort order once and pass it to all the children.

But what about the sort column? `get-sort-column` currently uses a named column
as the default one, this won't fly in a generic element. Let's instead look for
either the column marked as default, or just use the first one:

```clj
(defn get-sort-column [location columns]
  (let [param (-> location :location/hash-params :sort-column)]
    (or (first (filter (comp #{param} :id) columns))
        (first (filter :default? columns))
        (first columns))))

(defalias table [attrs children]
  (into
   [:table attrs]
   (mapv #(hiccup/update-attrs
           % assoc
           ::location (::location attrs)
           ::columns (::columns attrs)
           ::sort-order (get-sort-order (::location attrs))
           ::sort-column (get-sort-column (::location attrs) (::columns attrs)))
         children)))
```

Using this new alias won't make much of a difference yet:

```clj
(ns boardgames.ui
  (:require [boardgames.ui.sortable-table :as st]))

,,,

(defn render-page [{:keys [boardgames]} location]
  (let [sort-order (get-sort-order location)
        sort-column (get-sort-column location)]
    [:div.p-8.max-w-screen-lg
     ,,,
     [::st/table.w-full
      {::st/location location
       ::st/columns columns}
      ,,,]]))
```

The custom table element passes the location, column and sorting criteria to the
`thead` and `tbody`. In order to get them to the column headers, we'll need a
custom `thead` as well.

We will make a shortcut for the header: we'll assume that there is only one row
of headers, so our custom `thead` will take `th` elements as direct children.
This isn't necessary, but makes things more compact.

```clj
(defalias thead [attrs children]
  [:thead
   (into
    [:tr attrs]
    (map-indexed
     (fn [idx child]
       (hiccup/update-attrs
        child assoc
        ::location (::location attrs)
        ::column (nth (::columns attrs) idx)
        ::sort-order (get-sort-order (::location attrs))
        ::sort-column (get-sort-column (::location attrs) (::columns attrs))))
     children))])
```

Notice that we used the index to only pass the relevant column to each child.
This will be real handy inside each header. Using this element also makes little
difference yet:

```clj
(defn render-page [{:keys [boardgames]} location]
  (let [sort-order (get-sort-order location)
        sort-column (get-sort-column location)]
    [:div.p-8.max-w-screen-lg
     ,,,
     [::st/table.w-full
      {::st/location location
       ::st/columns columns}
      [::st/thead.border-b.bg-base-200
       [:th.py-2.text-left.px-4
        (render-header-link location (nth columns 0))]
       ,,,]
      ,,,]]))
```

The custom header element can now rely on being passed all the information it
needs. This means that it has all the necessary information to create the
content of the header, while we control the attributes of the header element.
Very much like a templating system.

```clj
(def reverse-order
  {"desc" "asc"
   "asc" "desc"})

(defalias th [{::keys [column sort-column sort-order location] :as attrs}]
  [:th attrs
   [:ui/a
    {:ui/location
     (if (= (:id column) (:id sort-column))
       (assoc-in location [:location/hash-params :sort-order]
                 (reverse-order sort-order))
       (assoc-in location [:location/hash-params :sort-column]
                 (:id column)))}
    (when (= (:id column) (:id sort-column))
      (if (= "desc" sort-order) "▼ " "▲ "))
    (:label column)]])
```

This custom element will help us quite a bit. We can now remove the header link
function, and update the render function to this:

```clj
(defn render-page [{:keys [boardgames]} location]
  (let [sort-order (get-sort-order location)
        sort-column (get-sort-column location)]
    [:div.p-8.max-w-screen-lg
     [:h1.text-2xl.font-serif.mb-4 "Boardgames ranked by Boardgamegeek"]
     [::st/table.w-full
      {::st/location location
       ::st/columns columns}
      [::st/thead.border-b.bg-base-200
       [::st/th.py-2.text-left.px-4]
       [::st/th.py-2.text-left.pr-4]
       [::st/th.py-2.text-left.pr-4]
       [::st/th.py-2.whitespace-nowrap.text-left.pr-4]
       [::st/th.py-2.whitespace-nowrap.text-left.pr-4]
       [::st/th.py-2.text-right.px-4]]
      [:tbody
       (for [game (sort-by (:f sort-column) (comparators sort-order) boardgames)]
         [:tr.border-b.border-1 {:replicant/key (:bgg/ranking game)}
          [:th.py-2.px-4.text-left ((:f (nth columns 0)) game)]
          [:td.py-2.pr-4.text-left ((:f (nth columns 1)) game)]
          [:td.py-2.pr-4.text-center ((:f (nth columns 2)) game)]
          [:td.py-2.pr-4.text-left ((:f (nth columns 3)) game)]
          [:td.py-2.pr-4.text-left ((:f (nth columns 4)) game)]
          [:td.py-2.px-4.text-right ((:f (nth columns 5)) game)]])]]]))
```

The final piece of the puzzle is to wrap the `tbody` and each cell in the body
rows. Once again we will allow nesting cells directly in it without the `tr`.
This time, we will loop the dataset to generate the rows.

Here's the tbody:

```clj
(def comparators
  {"desc" >
   "asc" <})

(defalias tbody [{::keys [columns sort-column sort-order data] :as attrs} children]
  (into
   [:tbody]
   (->> data
        (sort-by (:f sort-column) (comparators sort-order))
        (mapv
         (fn [cell-data]
           (into [:tr (assoc attrs :replicant/key cell-data)]
                 (map-indexed
                  (fn [col-idx cell]
                    (hiccup/update-attrs
                     cell assoc
                     ::column (nth columns col-idx)
                     ::data cell-data))
                  children)))))))
```

Notice that we set the `:replicant/key` on each row. Since we can't make
assumptions about which keys to select we just set the entire map as key, the
effect will be exactly the same.

Here's the `td`:

```clj
(defalias td [{::keys [column data] :as attrs}]
  [:td attrs ((:f column) data)])
```

With these final pieces, the original usage code is now down to the bare
essentials, only expressing the visual aspects and the column layouts:

```clj
(ns boardgames.ui
  (:require [boardgames.ui.sortable-table :as st]))

(def columns
  [{:f :boardgame/title, :id "title", :label "Title"}
   {:f :boardgame/release-year, :id "year", :label "Released"}
   {:f :bgg/ranking, :id "ranking", :label "Ranking", :default? true}
   {:f :bgg/geek-rating, :id "rating", :label "Geek rating"}
   {:f :bgg/average-rating, :id "average", :label "Avg. rating"}
   {:f :bgg/num-voters, :id "voters", :label "Voters"}])

(defn render-page [{:keys [boardgames]} location]
  [:div.p-8.max-w-screen-lg
   [:h1.text-2xl.font-serif.mb-4 "Boardgames ranked by Boardgamegeek"]
   [::st/table.w-full
    {::st/location location
     ::st/columns columns}
    [::st/thead.border-b.bg-base-200
     [::st/th.py-2.text-left.px-4]
     [::st/th.py-2.text-left.pr-4]
     [::st/th.py-2.text-left.pr-4]
     [::st/th.py-2.whitespace-nowrap.text-left.pr-4]
     [::st/th.py-2.whitespace-nowrap.text-left.pr-4]
     [::st/th.py-2.text-right.px-4]]
    [::st/tbody.border-b.border-1
     {::st/data boardgames}
     [::st/td.py-2.px-4.text-left]
     [::st/td.py-2.pr-4.text-left]
     [::st/td.py-2.pr-4.text-center]
     [::st/td.py-2.pr-4.text-left]
     [::st/td.py-2.pr-4.text-left]
     [::st/td.py-2.px-4.text-right]]]])
```

## td vs th

You may have noticed the small cheat in the last example. In the original code,
the title was in a `th` in `tbody`. Since `::st/th` and `::st/td` behave
differently, this isn't immediately possible. However, cells in `thead` and
`tbody` receive different data, so fixing it is quite straight forward:

```clj
(defalias th [{::keys [column sort-column sort-order location] :as attrs}]
  (if (::data attrs)
    [:th attrs ((:f column) (::data attrs))]
    [:th attrs
     [:ui/a
      {:ui/location
       (if (= (:id column) (:id sort-column))
         (assoc-in location [:location/hash-params :sort-order]
                   (reverse-order sort-order))
         (assoc-in location [:location/hash-params :sort-column]
                   (:id column)))}
      (when (= (:id column) (:id sort-column))
        (if (= "desc" sort-order) "▼ " "▲ "))
      (:label column)]]))
```

With that, we can achieve the exact same layout we started with:

```clj
(defn render-page [{:keys [boardgames]} location]
  [:div.p-8.max-w-screen-lg
   [:h1.text-2xl.font-serif.mb-4 "Boardgames ranked by Boardgamegeek"]
   [::st/table.w-full
    {::st/location location
     ::st/columns columns}
    [::st/thead.border-b.bg-base-200
     [::st/th.py-2.text-left.px-4]
     [::st/th.py-2.text-left.pr-4]
     [::st/th.py-2.text-left.pr-4]
     [::st/th.py-2.whitespace-nowrap.text-left.pr-4]
     [::st/th.py-2.whitespace-nowrap.text-left.pr-4]
     [::st/th.py-2.text-right.px-4]]
    [::st/tbody.border-b.border-1
     {::st/data boardgames}
     [::st/th.py-2.px-4.text-left]     ;; <=====
     [::st/td.py-2.pr-4.text-left]
     [::st/td.py-2.pr-4.text-center]
     [::st/td.py-2.pr-4.text-left]
     [::st/td.py-2.pr-4.text-left]
     [::st/td.py-2.px-4.text-right]]]])
```

## Conclusion

So there you have it, a completely data-driven sortable table. Using aliases, we
were able to clean it up to the point where it only expresses the necessary
layout details. It can now be used to sort any dataset with any visual
expression.

Some things are still somewhat hard-coded: the query parameter names, and the
details about the orders. These could be trivially parameterized, and doing so
is left as an exercise for the reader.

As always, [code is available on
Github](https://github.com/cjohansen/replicant-sortable-table).
