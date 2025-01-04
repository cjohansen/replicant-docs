:page/uri /tutorials/tic-tac-toe-alias/
:page/title Tic-Tac-Toe with aliases
:page/kind :page.kind/tutorial
:page/order 70
:page/body

In this tutorial, we will add [aliases](/aliases/) to the app we built in the
[Tic Tac Toe tutorial](/tutorials/tic-tac-toe/), and hopefully learn a thing or
two about how aliases work, and what they are good at.

## Reusable UI elements with aliases

We will start by converting the Tic Tac Toe UI elements to aliases. First up is
the cell element, which currently looks like this:

```clj
(defn render-cell [{:keys [content on-click dim? highlight? clickable?]}]
  [:button.cell
   {:on {:click on-click}
    :class (cond-> []
             dim? (conj "cell-dim")
             highlight? (conj "cell-highlight")
             clickable? (conj "clickable"))}
   (when content
     [:div.cell-content
      {:replicant/mounting {:class "transparent"}
       :replicant/unmounting {:class "transparent"}}
      content])])
```

The simplest possible way to convert this to an alias is to simply provide it as
an alias when rendering:

```clj
(require '[replicant.dom :as r])

(r/render el hiccup {:aliases {:ui/cell render-cell}})
```

This would work, since aliases will receive a map of attributes as their first
argument. However, we can create a better abstraction with a few tweaks.

The `content` could now just be the alias children. If we namespace the
remaining option keys, we can pass the attributes map as the attributes of
`:button`, and thus have a much more malleable building block, since users can
set arbitrary attributes on the element without the need to explicitly map
everything:

```clj
(defalias cell [{::keys [on-click dim? highlight? clickable?] :as attrs} content]
  [:button.cell
   (cond-> attrs
     on-click (assoc-in [:on :click] on-click)
     dim? (update :class conj "cell-dim")
     highlight? (update :class conj "cell-highlight")
     clickable? (update :class conj "clickable"))
   (when (seq content)
     (into
      [:div.cell-content
       {:replicant/mounting {:class "transparent"}
        :replicant/unmounting {:class "transparent"}}]
      content))])
```

This alias already is more flexible than the original function, as you can
easily add classes and custom attributes to it:

```clj
[:tic-tac-toe.ui/cell.myclass
  {:data-cell-id "f6c"}
  ui/marker-x]
```

The alias parameters aren't really being used for any sort of intelligent
logic -- they all map directly to one attribute. So we don't need that
indirection, there's nothing gained from using `:dim? true` to mean `:class
"cell-dim"`. Doing so will clean up the implementation as well:

```clj
(defalias cell [attrs content]
  [:button.cell attrs
   (when (seq content)
     (into
      [:div.cell-content
       {:replicant/mounting {:class "transparent"}
        :replicant/unmounting {:class "transparent"}}]
      content))])
```

This is even more flexible, but it is now harder to see what options are
available -- what classes are we supposed to use with this component? You can
answer that question with both a docstring, and Portfolio scenes that
demonstrate the various states.

The updated Portfolio scenes look like this:

```clj
(defscene empty-cell
  [ui/cell {:class :clickable}])

(defscene cell-with-x
  [ui/cell ui/mark-x])

(defscene cell-with-o
  [ui/cell ui/mark-o])

(defscene interactive-cell
  "Click the cell to toggle the tic on/off"
  :params (atom nil)
  [store]
  [ui/cell
   {:class "clickable"
    :on {:click (fn [_]
                  (swap! store #(if % nil ui/mark-x)))}}
   @store])

(defscene dimmed-cell
  [::ui/cell.cell-dim
   ui/mark-o])

(defscene highlighted-cell
  [::ui/cell.cell-highlight
   ui/mark-o])
```

Note the mixed use of vars and keywords to refer to the alias. This is a matter
of taste.

### The board

Here's the current board implementation:

```clj
(defn render-board [{:keys [rows]}]
  [:div.board
   (for [row rows]
     [:div.row
      (for [cell row]
        (render-cell cell))])])
```

We can't really convert this to an alias the same way we just did the cell.
Since the cell now takes a map of attributes _and_ content as two different
arguments, we can't as easily map over the maps in each row. But here's the
kicker: there is little to be gained from making an alias out of the board -- it
doesn't really do anything. Let's leave it for now.

## Converting business domain data to UI data

Let's have a look at the `game->ui-data` function that is responsible for
translating the business domain (e.g. our game state) to generic UI data and the
render function:

```clj
(defn game->ui-data [{:keys [size tics victory over?]}]
  (let [highlight? (set (:path victory))]
    {:button (when over?
               {:text "Start over"
                :on-click [:reset]})
     :board
     {:rows
      (for [y (range size)]
        (for [x (range size)]
          (if-let [player (get tics [y x])]
            (let [victorious? (highlight? [y x])]
              (cond-> {:content (player->mark player)}
                victorious? (assoc :highlight? true)
                (and over? (not victorious?)) (assoc :dim? true)))
            (if over?
              {:dim? true}
              {:clickable? true
               :on-click [:tic y x]}))))}}))

(defn render-game [{:keys [board button]}]
  [:div
   (render-board board)
   (when button
     [:button {:on {:click (:on-click button)}
               :style {:margin-top 20
                       :font-size 20}}
      (:text button)])])
```

Since the `cell` alias dropped the boolean indirection for the various class
names, this function can now output hiccup directly. In other words: with
aliases, we can combine the prepping function and the top-level render function
into one:

```clj
(defalias board [{:keys [size tics victory over?]}]
  (let [highlight? (set (:path victory))]
    [:div.board
     (for [y (range size)]
       [:div.row
        (for [x (range size)]
          (if-let [player (get tics [y x])]
            (let [victorious? (highlight? [y x])]
              [cell {:class (cond-> []
                              victorious? (conj :cell-highlight)
                              (and over? (not victorious?)) (conj :cell-dim))}
               (player->mark player)])
            (if over?
              [cell {:class :cell-dim}]
              [cell {:class :clickable
                     :on {:click [:tic y x]}}])))])]))

(defalias button [attrs children]
  (into [:button
         (assoc attrs :style {:margin-top 20
                              :font-size 20})]
        children))

(defn render-game [game]
  [:div
   [board game]
   (when (:over? game)
     [button {:on {:click [:reset]}}
      "Start over"])])
```

## What's the point?

It seems we have now undone an abstraction (`game->ui-data`) that was presented
as essential in keeping the business domain and the generic UI elements
separate, and to be able to test the UI without tripping on visual details.

The clue here is that the cell alias has increased the abstraction level of the
hiccup. Thus, we can write tests against the hiccup now and still not be
bothered by irrelevant rendering details. So let's have a look at the tests.

### Testing with aliases

The first test for `game->ui-data` tested that the game was properly converted
to something that could be rendered with the `board` element. We'll change this
to simply look at the rendered board.

Here's the adjusted call:

```clj
(ui/render-game
 {:size 3
  :tics {[0 0] :x
         [0 1] :o}
  :next-player :x})
```

And here's the output:

```clj
[:div [::ui/board
       {:size 3
        :tics {[0 0] :x
               [0 1] :o}
        :next-player :x}]
 nil]
```

Well, that doesn't really say much. The only thing we can really test with this
structure is whether or not there is a board, and whether or not there is a
button. We'll get back to that.

To test the layout of the board, we really need to expand the board alias. But
if we expand all the aliases, we end up with hiccup that has too many visual
details. Replicant provides `replicant.alias/expand-1` for this exact use case:
expand one level of aliases:

```clj
(require '[replicant.alias :as alias])

(->> (ui/render-game
      {:size 3
       :tics {[0 0] :x
              [0 1] :o}
       :next-player :x})
     alias/expand-1)

;;=>
'[:div
  [:div {:class #{"board"}}
   [:div.row
    ([::ui/cell {:class []} ui/mark-x]
     [::ui/cell {:class []} ui/mark-o]
     [::ui/cell {:class :clickable, :on {:click [:tic 0 2]}}])]
   [:div.row
    ([::ui/cell {:class :clickable, :on {:click [:tic 1 0]}}]
     [::ui/cell {:class :clickable, :on {:click [:tic 1 1]}}]
     [::ui/cell {:class :clickable, :on {:click [:tic 1 2]}}])]
   [:div.row
    ([::ui/cell {:class :clickable, :on {:click [:tic 2 0]}}]
     [::ui/cell {:class :clickable, :on {:click [:tic 2 1]}}]
     [::ui/cell {:class :clickable, :on {:click [:tic 2 2]}}])]]
  nil]
```

Now this is exactly the level of detail we want! If we could only focus the test
on the board itself, it'd be perfect.

We can use a library called [lookup](https://github.com/cjohansen/lookup) to
extract information from hiccup with CSS selectors:

```clj
(->> (ui/render-game
      {:size 3
       :tics {[0 0] :x
              [0 1] :o}
       :next-player :x})
     alias/expand-1
     (lookup/select-one :div.board))

;;=>
[:div {:class #{"board"}}
 ,,,]
```

And the final test:

```clj
(testing "Renders board"
  (is (= (->> (ui/render-game
               {:size 3
                :tics {[0 0] :x
                       [0 1] :o}
                :next-player :x})
              alias/expand-1
              (lookup/select-one :div.board))
         [:div {:class #{"board"}}
          [:div {:class #{"row"}}
           [::ui/cell ui/mark-x]
           [::ui/cell ui/mark-o]
           [::ui/cell {:on {:click [:tic 0 2]}, :class #{"clickable"}}]]
          [:div {:class #{"row"}}
           [::ui/cell {:on {:click [:tic 1 0]}, :class #{"clickable"}}]
           [::ui/cell {:on {:click [:tic 1 1]}, :class #{"clickable"}}]
           [::ui/cell {:on {:click [:tic 1 2]}, :class #{"clickable"}}]]
          [:div {:class #{"row"}}
           [::ui/cell {:on {:click [:tic 2 0]}, :class #{"clickable"}}]
           [::ui/cell {:on {:click [:tic 2 1]}, :class #{"clickable"}}]
           [::ui/cell {:on {:click [:tic 2 2]}, :class #{"clickable"}}]]])))
```

Now, this test covers a lot. We really don't want many tests at this level, but
one is fine.

For the next test, we can make a more surgical extraction. The test "plays" a
game until victory, and expects to find the winning path highlighted in the UI
data:

```clj
(testing "Highlights winning path"
  (is (= (-> (game/create-game {:size 3})
             (game/tic 0 0) ;; x
             (game/tic 1 0) ;; o
             (game/tic 0 1) ;; x
             (game/tic 1 1) ;; o
             (game/tic 0 2) ;; x
             ui/render-game
             alias/expand-1
             (->> (lookup/select '.cell-highlight)))
         [[:tic-tac-toe.ui/cell {:class #{"cell-highlight"}} ui/mark-x]
          [:tic-tac-toe.ui/cell {:class #{"cell-highlight"}} ui/mark-x]
          [:tic-tac-toe.ui/cell {:class #{"cell-highlight"}} ui/mark-x]])))
```

You may worry that the asserted data doesn't say anything about the coordinates,
but don't. The previous test already demonstrated that the marks are placed in
the correct positions, so the fact that this sequence of operations gave us
three cells, all marked with x, is all we needed to know.

The original test also tested that everything else was dimmed. Let's do that in
a separate test instead, this time a simple count is enough:

```clj
(testing "Dims everything besides the winning path"
  (is (= (-> (game/create-game {:size 3})
             (game/tic 0 0) ;; x
             (game/tic 1 0) ;; o
             (game/tic 0 1) ;; x
             (game/tic 1 1) ;; o
             (game/tic 0 2) ;; x
             ui/render-game
             alias/expand-1
             (->> (lookup/select '.cell-dim))
             count)
         6)))
```

The final test verifies that the entire game is dimmed down in case of a tie.
This is very similar to the test we just wrote, just with a different count:

```clj
(testing "Dims tied game"
  (is (= (-> (game/create-game {:size 3})
             (game/tic 0 0) ;; x
             (game/tic 0 1) ;; o
             (game/tic 0 2) ;; x
             (game/tic 1 0) ;; o
             (game/tic 1 1) ;; x
             (game/tic 2 2) ;; o
             (game/tic 2 1) ;; x
             (game/tic 2 0) ;; o
             (game/tic 1 2) ;; x
             ui/render-game
             alias/expand-1
             (->> (lookup/select '.cell-dim))
             count)
         9)))
```

## Conclusion

The main difference between the original implementation and the new one is the
merging of `game->ui-data` and `render-data`. This change really pokes at the
benefit and difficulty of using aliases well, because it both improved the
implementation and blurred the lines a little.

### The benefits

The code is more direct: where there used to be two steps (convert domains, then
render), there is now only one (render). Because of how aliases work, we gained
a performance boost: the conversion between domains is now done during
rendering, and only when data has actually changed.

With aliases, we were able to express the UI in hiccup that contained much less
fleeting details such as specific classes, data attributes and inline styles.
This allowed us to write tests directly for rendering, technically increasing
coverage while reducing the number of representations -- without sacrificing
generic UI elements.

### Drawbacks

Aliases are used for two types of tasks: abstracting the UI elements and
converting the business domain (game data) to generic UI data. This distinction
is not very clear, as both kinds use the same mechanism (aliases) and live
side-by-side.

Since there is no clear distinction between the two, we run a risk of data
conversion leaking into the UI elements, or too much rendering details leaking
into the data conversion. There is no hard limit: a class here and there
(`.board`, `.row`) is probably fine in "conversion code", many of them are not.

In other words: you will need some level of discipline to keep a clean
separation, which is always challenging, espcially on larger teams.

To succeed with this approach, you could keep the different kinds of code in
separate namespaces, and use naming to try to enforce the separation. This way
you could have a hard and fast rule that UI elements should never use business
domain terms, and conversion/prep code should not contain so much visual detail
that you would be tempted to display it in Portfolio.

It's worth noting that this exact problem also exists in component-based
libraries and frameworks. In fact, these libraries often advertise mixing the
two aspects. It's up to you, but in my experience, keeping them separate gives
you better control.

### Source code

The full code listing is available on [the aliases branch in the tutorial
repo](https://github.com/cjohansen/replicant-tic-tac-toe/tree/aliases).
