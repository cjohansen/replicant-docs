--------------------------------------------------------------------------------
:page/uri /learn/
:page/title Learn Replicant
:page/kind :page.kind/article
:open-graph/description

Learn to develop UIs in Clojure/ClojureScript with Replicant

--------------------------------------------------------------------------------
:block/markdown

So you decided to learn Replicant, thanks! Replicant is pretty straight forward:
compile some hiccup, and pass it to Replicant. When you need to update the UI,
do it all over again, and Replicant will do just what's needed to update the
DOM.

--------------------------------------------------------------------------------
:block/lang :clj
:block/alignment :mx-0
:block/code

(require '[replicant.dom :as r])

(r/render js/document.body
  [:div.media
   [:aside.media-thumb
    [:img.rounded-lg {:src "/images/christian.jpg"}]]
   [:main.grow
    [:h2.font-bold "Christian Johansen"]
    [:p "Just wrote some documentation for Replicant."]
    [:p.opacity-50
     "Posted February 26th 2025"]]])

;; Or render to a string, on the client or on the server
(require '[replicant.string :as s])

(s/render [:h1 "Hello world!"])

--------------------------------------------------------------------------------
:block/markdown

If you're new to Replicant, the [Tic-Tac-Toe tutorial](/tutorials/tic-tac-toe/)
takes you from an empty directory to a working implementation of the game. It
will teach you most of what you need to know.

The [Hiccup](/hiccup/) guide details every nook and cranny of "Replicant
flavored hiccup". It is recommended reading for everyone, including if you have
previously worked with other hiccup libraries.

Learning to use Replicant is mostly learning to create stateless, data-driven
user interfaces -- which you can also do in any number of other rendering
libraries. In fact, you can follow many of the provided tutorials using several
other rendering libraries.

The two state management tutorials ([with atoms](/tutorials/state-atom/) and
[with Datascript](/tutorials/state-datascript/)) detail how to do
state-management in a [top-down rendered](/top-down/) stateless UI. Similarly,
[the routing tutorial](/tutorials/routing/) demonstrate routing in a stateless
UI.
