--------------------------------------------------------------------------------
:page/uri /tutorials/network-reads/
:page/title Data-driven queries
:page/kind :page.kind/tutorial
:page/order 50

--------------------------------------------------------------------------------
:block/markdown

In this second part of the [networking tutorial](/tutorials/network/), we will
build a data-driven system for reading data over the network.

The setup for this tutorial is based on [the state management with an atom
tutorial](/tutorials/state-atom/), including the routing extension and a small
backend. If you want to follow along, grab [the setup on
Github](https://github.com/cjohansen/replicant-networking/tree/setup), and
follow the README to get running.

--------------------------------------------------------------------------------
:block/title The design
:block/level 2
:block/id design
:block/markdown

To build a declarative, data-driven solution for performing reads over the
network our pure rendering functions should be able to answer questions like:

- Have we requested this piece of data? How long ago?
- Is the data currently loading?
- Is the data available?
- Is the available data stale? (e.g. we have requested it again, but not
  received a response)
- Did we fail to fetch the data? Why?

If we can come up with a data structure that answers these questions and that is
available to the render function, we should be able to render spinners, error
messages and the data itself whenever appropriate.

We must be able to ask the questions above separately for the request for all
users and the request for a specific document. In other words, we must be able
to address our reads.

We will call a network read a query, and represent it with a map:

```clj
{:query/kind :query/user
 :query/data {:user-id "alice"}}
```

This gives us a concrete way to talk about the various requests the frontend
makes without HTTP details in the rendering code. We will look at how this data
structure becomes an HTTP request later.

The query map also gives us a way to address queries to ask the questions above:

```clj
(query/loading? state {:query/kind :query/todo-items})
;;=> true
```

To answer the questions above, we will keep a query log keyed by the query maps:

```clj
{:query/log
 {{:query/kind :query/user
   :query/data {:user-id "alice"}}                             ;; 1
  [{:query/status :query.status/success                        ;; 2
    :query/result {:user/id "alice"                            ;; 3
                   :user/given-name "Alice"
                   :user/family-name "Johnson"
                   :user/email "alice.johnson@acme-corp.com"}
    :query/user-time #inst "2024-12-31T09:29:23.307-00:00"}    ;; 4
   {:query/status :query.status/loading                        ;; 5
    :query/user-time #inst "2024-12-31T09:29:23.142-00:00"}]}}
```

1. The entire query map is used as key in the query log. This looks a little
   weird, but will be very handy in code.
2. The log is in reverse order with the latest event first. Since the log uses
   browser memory, we can't allow it to grow infinitely. The reverse order makes
   it easy to truncate the log with `take-while` or similar.
3. When the query was successful, the log contains the resulting data.
4. Each log entry has the browser time.
5. The entry describing our initial request.

You might be wondering why we're only discussing data structures, and not HTTP
mechanics in a tutorial about networking. A good data model is the key to an
effective design. We will add the HTTP mechanics at the very end.

--------------------------------------------------------------------------------
:block/title Answering questions
:block/level 2
:block/id questions
:block/markdown

With the data model in place we can write some pure functions that updates it
and use it to answer questions. We can write some tests for this logic.

When a query has just been sent, we expect it to be loading:

```clj
(ns toil.query-test
  (:require [clojure.test :refer [deftest is testing]]
            [toil.query :as query]))

(def query {:query/kind :query/todo-items})

(deftest decisions-test
  (testing "Sends request"
    (is (true? (-> (query/send-request {} #inst "2025-01-02T06:44:13" query)
                   (query/loading? query))))))
```

We pass in the time to use as `now` so the function can remain pure. To pass
this test we will need to both update the log and investigate its status:

```clj
(ns toil.query)

(defn add-log-entry [log entry]
  (cons entry log))

(defn send-request [state now query]
  (update-in state [::log query] add-log-entry
             {:query/status :query.status/loading
              :query/user-time now}))

(defn get-latest-status [state query]
  (:query/status (first (get-in state query))))

(defn loading? [state query]
  (= :query.status/loading
     (get-latest-status state [::log query])))
```

Next we will simulate receiving a response. When we do, the status should no
longer be loading:

```clj
(def todo-items
  {:todo/items [{:todo/id "74e67"
                 :todo/title "Write project documentation"
                 :todo/done? false}]})

(deftest decisions-test
  ,,,

  (testing "Received successful response"
    (is (false? (-> (query/send-request {} #inst "2025-01-02T06:44:13" query)
                    (query/receive-response #inst "2025-01-02T06:44:14" query
                      {:success? true
                       :result todo-items})
                    (query/loading? query))))))
```

The `receive-response` function adds a log entry:

```clj
(defn receive-response [state now query response]
  (update-in state [::log query] add-log-entry
             (cond-> {:query/status (if (:success? response)
                                      :query.status/success
                                      :query.status/error)
                      :query/user-time now}
               (:success? response)
               (assoc :query/result (:result response)))))
```

When the response was a success, the query results should be marked as
available:

```clj
(testing "Successful response is available"
  (is (true? (-> (query/send-request {} #inst "2025-01-02T06:44:13" query)
                 (query/receive-response #inst "2025-01-02T06:44:14" query
                   {:success? true
                    :result todo-items})
                 (query/available? query)))))
```

Implementing this is a breeze:

```clj
(defn available? [state query]
  (= :query.status/success
     (get-latest-status state query)))
```

We're not going to handle all the details of this decision making namespace, but
there is one more particularly interesting case to handle, which is this
sequence of events:

1. Request all todo items
2. Receive all todo items
3. Request all todo items again (some time later)

Thanks to the log, we're now in the situation where todo items are both
available _and_ loading. This gives us flexibility: do we want to eagerly show
the user that we're actively refreshing their data with loading spinners? Or do
we want to downplay the loading time and just show the old data until new data
is available? The query log makes this an active choice.

```clj
(testing "Successful response is still available when refreshing"
  (is (true? (-> (query/send-request {} #inst "2025-01-02T06:44:13" query)
                 (query/receive-response #inst "2025-01-02T06:44:14" query
                   {:success? true
                    :result todo-items})
                 (query/send-request #inst "2025-01-02T06:44:15" query)
                 (query/available? query)))))
```

To pass this test, `available?` must check the entire log:

```clj
(defn available? [state query]
  (->> (get-in state [::log query])
       (some (comp #{:query.status/success} :query/status))
       boolean))
```

Finally, we'll add a function to get the currently available data, if any:

```clj
(testing "Gets available data"
  (is (= (-> (query/send-request {} #inst "2025-01-02T06:44:13" query)
             (query/receive-response #inst "2025-01-02T06:44:14" query
               {:success? true
                :result todo-items})
             (query/get-result query))
         todo-items)))
```

The implementation finds the first result in the log:

```clj
(defn get-result [state query]
  (->> (get-in state [::log query])
       (keep :query/result)
       first))
```

With this we are able to answer the most pertinent questions about our network
requests. There are some more details like error handling and log truncating to
cater to. Check out the final version of the code on Github for all the details.

--------------------------------------------------------------------------------
:block/title Making HTTP requests
:block/level 2
:block/id http-requests
:block/markdown

We now turn our attention to the HTTP request itself. We have a query data
structure that represents requests for data. How this is converted to an HTTP
request depends on your backend API(s).

The further "up the stack" we can solve problems, the better, so our backend has
a single HTTP enpoint, `/query`. It take query maps and returns data in a
unified wrapper: `{:success? true, :results ,,,}`. It is not necessary to build
your backend API this way to model network reads like we have done, an
alternative is sketched out below.

Because our backend was designed to play well with the frontend, the HTTP
mechanics can be handled pretty straight-forward with
[`fetch`](https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API/Using_Fetch):

--------------------------------------------------------------------------------
:block/lang :clj
:block/code

(ns toil.core
  (:require [cljs.reader :as reader]
            ,,,
            [toil.query :as query]
            ,,,))

,,,

(defn query-backend [store query]
  (swap! store query/send-request (js/Date.) query)
  (-> (js/fetch "/query" #js {:method "POST"
                              :body (pr-str query)})
      (.then #(.text %))
      (.then reader/read-string)
      (.then #(swap! store query/receive-response
                           (js/Date.) query %))
      (.catch #(swap! store query/receive-response
                            (js/Date.) query {:error (.-message %)}))))

--------------------------------------------------------------------------------
:block/markdown

What if your backend API can't be tailored to your client? Maybe you have a
traditional REST API of some sort. We can extend the query function and have it
dispatch on the query kind to get the HTTP details:

--------------------------------------------------------------------------------
:block/lang :clj
:block/code

(defn query->http-request [{:query/keys [kind data]}]
  (case kind
    :query/todo-items
    [:get "/api/todo/items"]

    :query/user
    [:get (str "/api/todo/users/" (:user-id data))]))

(defn query-backend [store query]
  (let [[method url body] (query->http-request query)]
    (swap! store query/send-request (js/Date.) query)
    (-> (js/fetch url #js (cond-> {:method (name method)}
                            body (assoc :body (pr-str body))))
        (.then #(.text %))
        (.then reader/read-string)
        (.then #(swap! store query/receive-response
                             (js/Date.) query %))
        (.catch #(swap! store query/receive-response
                              (js/Date.) query {:error (.-message %)})))))

--------------------------------------------------------------------------------
:block/markdown

You could use the same approach if you need to talk to different APIs, etc. If
your endpoints don't respond in a unified manner, you may also want to add a
function that can repackage the responses in a uniform manner:

```clj
(defn query->http-request [{:query/keys [kind data]}]
  (case kind
    :query/todo-items
    {:method :get
     :url "/api/todo/items"
     :get-responses (fn [res]
                      {:success? true
                       :results res})}

    :query/user
    {:method :get
     :url (str "/api/todo/users/" (:user-id data))
     :get-responses (fn [res]
                      {:success? true
                       :results (:user res)})}))
```

Adjust as necessary for your backend. The idea is to handle the HTTP mechanics
in one place, and to make all reads behave as uniformly as possible. This way
you avoid having details about your backend architecture and design choices
bleed through your entire frontend codebase.

--------------------------------------------------------------------------------
:block/title Triggering HTTP requests
:block/level 2
:block/id triggering-http-requests
:block/markdown

The final piece of the puzzle is to trigger the HTTP requests. We will do this
two ways: first we'll add an action that requires the user to ask for data, and
then we will look into requesting data as we load a route.

### Asking for data

Exposing `query-backend` as an action for DOM events is a one-liner. In
`toil.core`:

```clj
(defn execute-actions [store actions]
  (doseq [[action & args] actions]
    (case action
      :store/assoc-in (apply swap! store assoc-in args)
      :data/query (apply query-backend store args)               ;; <==
      (println "Unknown action" action "with arguments" args))))
```

With this in place, we can request data from the UI. We can also use the
functions in `toil.query` to update the UI appropriately:

```clj
(ns toil.ui
  (:require [toil.query :as query]))

(def items-query
  {:query/kind :query/todo-items})

(defn render-frontpage [state]
  [:main.p-8.max-w-screen-lg
   [:h1.text-2xl.mb-4 "Toil and trouble: Todos over the network"]
   (when-let [todos (query/get-result state items-query)]
     [:ul.mb-4
      (for [item todos]
        [:li.my-2
         [:span.pr-2
          (if (:todo/done? item)
            "✓"
            "▢")]
         (:todo/title item)])])
   (if (query/loading? state items-query)
     [:button.btn.btn-primary {:disabled true}
      [:span.loading.loading-spinner]
      "Fetching todos"]
     [:button.btn.btn-primary
      {:on {:click [[:data/query items-query]]}}
      "Fetch todos"])])
```

With this essential bit of infrastructure in place, we can add new query
capabilities to the app without writing a single line of imperative code. To do
so, you would implement a new query on the backend, and add pure rendering code
to use it, e.g.:

```clj
[:data/query
 {:query/kind :query/user
  :query/data {:user-id "alice"}}]
```

### Loading data on navigation

Having to click a button on the frontpage to get its data isn't ideal UX.
Ideally, the data would just load when the user loaded the frontpage. We can do
that by extending our mini framework.

Currently we have routes, which tie a specific URL pattern to a namespaced
keyword. This keyword is used to dispatch rendering to a page-specific function.
We could do the same for page-specific data to load.

Whenever `:location/page-id` changes, we will call a new function
`get-location-load-actions` with the location. If it returns any actions, we'll
run them through the action dispatch.

To get the actions:

```clj
(defn get-location-load-actions [location]
  (case (:location/page-id location)
    :pages/frontpage [[:data/query {:query/kind :query/todo-items}]]))
```

To trigger them when the location changes we will introduce a function to handle
navigation:

```clj
(defn navigate! [store location]
  (let [current-location (:location @store)]
    (swap! store assoc :location location)
    (when (not= current-location location)
      (execute-actions store (get-location-load-actions location)))))
```

We will use this in place of `(swap! store assoc :location location)`, which
happens in three places: bootup, body clicks, and back button clicks:

```clj
(defn route-click [e store routes]
  (let [href (find-target-href e)]
    (when-let [location (router/url->location routes href)]
      ,,,
      (navigate! store location))))

(defn main [store el]
  ,,,

  (js/window.addEventListener
   "popstate"
   (fn [_] (navigate! store (get-current-location))))

  ;; Trigger the initial render
  (navigate! store (get-current-location))
  ,,,)
```

--------------------------------------------------------------------------------
:block/title Extra credit: Reorganizing
:block/level 2
:block/id reorganizing
:block/markdown

NB! This finaly section has nothing to do with networking, but rather deals with
code organization.

Currently, routes, location load actions, and rendering are all scattered in
different places. We can gather this in a neat package, describing all aspect of
each of the pages.

Create `toil.frontpage`:

```clj
(ns toil.frontpage
  (:require [toil.query :as query]))

(def items-query
  {:query/kind :query/todo-items})

(defn render [state]
  [:main.p-8.max-w-screen-lg
   [:h1.text-2xl.mb-4 "Toil and trouble: Todos over the network"]
   (when-let [todos (query/get-result state items-query)]
     [:ul.mb-4
      (for [item todos]
        [:li.my-2
         [:span.pr-2
          (if (:todo/done? item)
            "✓"
            "▢")]
         (:todo/title item)])])
   (if (query/loading? state items-query)
     [:button.btn.btn-primary {:disabled true}
      [:span.loading.loading-spinner]
      "Fetching todos"]
     [:button.btn.btn-primary
      {:on {:click [[:data/query items-query]]}}
      "Fetch todos"])])

(def page
  {:page-id :pages/frontpage
   :route []
   :on-load (fn [location]
              [[:data/query {:query/kind :query/todo-items}]])
   :render #'render})
```

This puts the load actions in a function that receives the `location`. This
enables us to add pages that use parameters from the routing or query string to
fetch relevant data on load.

To use this data structure, we will make some changes to the main function.
First we'll gather "all" the pages in a map from page id to the page map:

```clj
(ns toil.core
  (:require ,,,
            [toil.frontpage :as frontpage]
            ,,,))

,,,

(def pages
  [frontpage/page])

(def by-page-id
  (->> pages
       (map (juxt :page-id identity))
       (into {})))
```

Then we'll define a function to return the render function for a location:

```clj
(defn get-render-f [state]
  (or (get-in by-page-id [(-> state :location :location/page-id) :render])
      ui/render-page))
```

Then we'll update the function to get the on load actions to use the pages map
instead:

```clj
(defn get-location-load-actions [location]
  (when-let [f (get-in by-page-id [(:location/page-id location) :on-load])]
    (f location)))
```

Instead of static routes in the router namespace, we will add a function to
build the routes data from the map of pages:

```clj
(defn make-routes [pages]
  (silk/routes
   (mapv
    (fn [{:keys [page-id route]}]
      [page-id route])
    pages)))
```

And finally, we can update `main` to work with the new building blocks:

```clj
(defn main [store el]
  (let [routes (router/make-routes pages)]
    (add-watch
     store ::render
     (fn [_ _ _ state]
       (let [f (get-render-f state)]
         (r/render el (f state) {:alias-data {:routes routes}}))))

    (r/set-dispatch!
     (fn [event-data actions]
       (->> actions
            (interpolate-actions
             (:replicant/dom-event event-data))
            (execute-actions store))))

    (js/document.body.addEventListener
     "click"
     #(route-click % store routes))

    (js/window.addEventListener
     "popstate"
     (fn [_] (navigate! store (get-current-location routes))))

    ;; Trigger the initial render
    (navigate! store (get-current-location routes))
    (swap! store assoc :app/started-at (js/Date.))))
```

## Loading more data

Now that we have a nice structure for pages and a mechanism for loading data on
navigation, let's add one more page. We will render who added each todo item,
and make their name clickable. Clicking the name will take us to a page with
some details about the user.

First, add a new namespace with the new page:

```clj
(ns toil.user)

(defn render [state]
  [:main.p-8.max-w-screen-lg
   [:h1.text-2xl.mb-4 "User " (-> state :location :location/params :user/id)]
   [:p
    [:ui/a.link {:ui/location {:location/page-id :pages/frontpage}}
     "Back"]]])

(def page
  {:page-id :pages/user
   :route [["users" :user/id]]
   :render #'render})
```

Add this page to the list of pages in `toil.core`:

```clj
(def pages
  [user/page
   frontpage/page])
```

NB! These need to be ordered by most to least specific route, due to how silk
processes routes.

Now add a link to the new page on the frontpage:

```clj
(defn render [state]
  [:main.p-8.max-w-screen-lg
   [:h1.text-2xl.mb-4 "Toil and trouble: Todos over the network"]
   (when-let [todos (query/get-result state items-query)]
     [:ul.mb-4
      (for [item todos]
        [:li.my-2
         [:span.pr-2
          (if (:todo/done? item)
            "✓"
            "▢")]
         (:todo/title item)
         " ("
         [:ui/a.link
          {:ui/location
           {:location/page-id :pages/user
            :location/params {:user/id (:todo/created-by item)}}}
          (:todo/created-by item)]
         ")"])])
   (if (query/loading? state items-query)
     [:button.btn.btn-primary {:disabled true}
      [:span.loading.loading-spinner]
      "Fetching todos"]
     [:button.btn.btn-primary
      {:on {:click [[:data/query items-query]]}}
      "Fetch todos"])])
```

To spice up the user page with some data about the user we will add some load
actions that uses parameters from the routing to load the right user:

```clj
(ns toil.user
  (:require [toil.query :as query]))

(defn get-query [location]
  {:query/kind :query/user
   :query/data {:user-id (-> location :location/params :user/id)}})

,,,

(def page
  {:page-id :pages/user
   :route [["users" :user/id]]
   :on-load (fn [location]
              [[:data/query (get-query location)]])
   :render #'render})
```

Then the render function can check if the user is available:

```clj
(defn render [state]
  (let [user (query/get-result state (get-query (:location state)))]
    [:main.p-8.max-w-screen-lg
     [:h1.text-2xl.mb-4
      (if user
        (str (:user/given-name user) " " (:user/family-name user))
        (str "User " (-> state :location :location/params :user/id)))]
     (when user
       [:p.mb-2 (:user/email user)])
     [:p
      [:ui/a.link {:ui/location {:location/page-id :pages/frontpage}}
       "Back"]]]))
```

--------------------------------------------------------------------------------
:block/title Conclusion
:block/level 2
:block/id conclusion
:block/markdown

In this tutorial we built a small system for dealing with network reads. You
_could_ use the same system for issuing writes over the network, but the
semantics are different enough to warrant a system of its own for those, as we
go over in [the third and final installment of this
tutorial](/tutorials/network-writes/).

The [full code for this part is on
Github](https://github.com/cjohansen/replicant-networking/tree/network-reads/).
