:page/uri /guide/hiccup/
:page/title Replicant Flavored Hiccup
:page/body

In Replicant, we use Clojure data literals like vectors, keywords, maps and
strings to write HTML. When Clojure data is used to write HTML in this way we
call it "hiccup". The idea and name was coined by [James
Reeves](https://github.com/weavejester) in his [library of the same
name](https://github.com/weavejester/hiccup).

Replicant supports some additional features not available in every other library
that supports hiccup. This guide details all the features Replicant supports. If
you are missing something, please [file an
issue](https://github.com/cjohansen/replicant/issues).

## Tags

Replicant's definition of hiccup is a vector with a keyword in it:

```clj
[:br]
```

```html
<br>
```

The vector can be thought of as the brackets and the keyword represents the tag
name.

### Allowed tags

So, which tags can you use? Any one that the browser supports, including custom
elements created with `window.customElements.define`. Replicant does not
maintain a separate list of valid tags. Whatever keyword you put in the first
position of the vector will be used as the tag name -- just be aware that
[namespaced keywords are treated differently](/guide/alias/).

## Elements

Most HTML elements have child nodes -- some text and/or other elements. We can
list those after the tag name:

```clj
[:h1 "Hello Replicant"]
```

```html
<h1>Hello Replicant</h1>
```

Multiple strings are fine. They will be combined to one string with no
separators, so make sure to include spaces as required:

```clj
[:h1 "Hello " given-name ", how are ya?"]
```

```html
<h1>Hello Christian, how are ya?</h1>
```

You can nest other elements as well:

```clj
[:ul
 [:li "Data-driven"]
 [:li "Functional"]
 [:li "Unidirectional"]]
```

```html
<ul>
  <li>Data-driven</li>
  <li>Functional</li>
  <li>Unidirectional</li>
</ul>
```

### Fragments / lists

The children of an element does not need to appear one after the other as direct
descendants of the parent element. Hiccup is typically built with code, and
Replicant accounts for this fact.

The following code snippet would put the `li` elements in a list, and that's OK.

```clj
[:ul
 (for [fruit ["Banana" "Apple" "Orange"]]
  [:li fruit])]

;;=>

[:ul
 '([:li "Banana"]
   [:li "Apple"]
   [:li "Orange"])]
```

```html
<ul>
  <li>Banana</li>
  <li>Apple</li>
  <li>Orange</li>
</ul>
```

In fact, you can nest elements as deeply and irregularly as you want:

```clj
[:ul
 (map get-preferences people)]

;;=>

[:ul
 '(([:li "Apple"])
   ([:li "Orange"]
    [:li "Banana"])
   ([:li ("Chocolate" "Chips")]))]
```

```html
<ul>
  <li>Apple</li>
  <li>Orange</li>
  <li>Banana</li>
  <li>Chocolate Chips</li>
</ul>
```

Replicant doesn't support document fragments directly, but lists of elements
have the same effect in most cases.

### `nil` children

`nil`s are perfectly fine as children. This means it's safe to produce hiccup
with code that uses `when`, or calls functions that may or may not return hiccup
to be used as children. To learn how explicit `nil`s can help Replicant make
better choices, check out [explicit nils](/guide/nil/).

## Attributes

To give an element attributes, place a map next to the tag name. Attributes use
the same name as in the DOM API, and can be keywords, strings or symbols.
Typically keywords are used:

```clj
[:img {:src "/images/homer.jpg" :width 160 :height 90}]
```

```html
<img src="/images/homer.jpg" width="160" height="90">
```

You can of course have both attributes and children:

```clj
[:div {:data-theme "cupcake"}
 [:a {:href "https://vimeo.com/861600197"}
  [:img
   {:src "/images/data-driven.png"
    :alt "Watch talk: Stateless, Data-driven UIs"}]]]
```

```html
<div data-theme="cupcake">
  <a href="https://vimeo.com/861600197">
    <img src="/images/data-driven.png"
         alt="Watch talk: Stateless, Data-driven UIs">
  </a>
</div>
```

Attributes can have explicit nil values. If they do, Replicant behaves as though
the attribute wasn't there. This means it's safe to wrap an attribute's value in
a `when`, and you don't have to check every possible attribtue for `nil`:

```clj
(defn media [{:keys [url thumbnail title playing?]}]
  [:div.media {:data-theme "cupcake"}
   [:a {:href url
        :class (when playing?
                 "spinner")}
    [:img
     {:src thumbnail
      :alt title}]]])

;;=>
[:div.media {:data-theme "cupcake"}
 [:a {:href "https://vimeo.com/861600197"
      :class nil}
  [:img {:src "/images/data-driven.png" :alt nil}]]]
```

```html
<div data-theme="cupcake">
  <a href="https://vimeo.com/861600197">
    <img src="/images/data-driven.png">
  </a>
</div>
```

### Id

You can use CSS selector syntax to add an id directly to the hiccup tag name:

```clj
[:h1#heading "Hello Clojurians"]
```

```html
<h1 id="heading">Hello Clojurians</h1>
```

Ids on the tag name is practical when you can type in both at the same time. But
you can also provide an `:id` attribute in the attribute map when appropriate,
e.g. when the id is a computed value:

```clj
(let [id "heading"]
  [:h1 {:id id} "Hello Clojurians"])
```

```html
<h1 id="heading">Hello Clojurians</h1>
```

### Classes

Classes are arguably the most commonly used attributes. Because they are so
common, classes can be added directly to the tag name — just like you would
write a CSS selector:

```clj
[:img.rounded-lg.block {:src "/images/data-driven.png"}]
```

```html
<img class="rounded-lg block" src="/images/data-driven.png">
```

Multiple classes are separated by a dot. If you also use the CSS selector syntax
for ids, the id needs to preceed the classes:

```clj
[:img#image.rounded-lg.block {:src "/images/data-driven.png"}]
```

```html
<img id="image" class="rounded-lg block" src="/images/data-driven.png">
```

Like with ids, tacking classes on to the tag name is very practical when you can
type them both out ahead of time. However, conditional or computed classes are
better added to the `:class` attribute.

The `:class` attribute supports strings, keywords, and a collection of strings
and/or keywords.

```clj
[:img {:src "/images/data-driven.png"
       :class [:rounded-lg "block"]}]
```

```html
<img class="rounded-lg block" src="/images/data-driven.png">
```

Passing a string with space-separated classes, e.g. `[:img {:class "rounded-lg
block"}]` will work but is discouraged. In fact, it will produce a console error
during development. The reason is that Replicant has to parse the string to
understand it. Use a collection instead.

### Styles

The `:style` attribute can take a literal HTML style attribute value, like
`"background: #000; color: #fff"`, but who wants to write CSS in a string?
Replicant also supports passing a map to the `:style` attribute. The map keys
should be keywords, and the values should be strings or numbers:

```clj
[:h1
 {:style
  {:font-family: "helvetica, sans-serif"
   :font-weight 900
   :max-width 800}}
 "Hello!"]
```

```html
<h1 style="font-family: helvetica, sans-serif; font-weight: 900; max-width: 800px;">
  Hello!
</h1>
```

Replicant treats most numbers as pixel values, but knows which CSS properties
take numeric values that aren't pixels. Style properties are spelled just like
you would spell them in a CSS file.

### innerHTML

While not ideal, sometimes you have a string that contains some HTML (like the
output of a CMS WYSIWYG field) and you just want to insert it into the DOM tree
without any further hassle. Replicant's got you covered. When an element has the
`:innerHTML` attribute, its child nodes are completely ignored and the node's
content will be dictated by the `:innerHTML` attribute.

When rendering to a string on the server, `:innerHTML` is how you can output an
unescaped string into the generated DOM.

```clj
[:div {:innerHTML "<h1>Hello there!</h1>"}]
```

```html
<div>
  <h1>Hello there!</h1>
</div>
```

### Event handlers

### Life-cycle hooks

### Mounting and unmounting

### Keys