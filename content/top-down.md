:page/uri /top-down/
:page/title Top-down rendering
:page/kind :page.kind/article
:page/body

Replicant has a single mode of operation: top-down rendering. In this article,
we'll discuss what that means, why it's good, and what it's drawbacks are.

## The inspiration

In 2013, React launched with the idea that your UI is just a function of your
application state:

```js
UI = f(applicationState)
```

In other words, gather all your application state in one place. Whenever it
changes, pass all of it to a single function to produce the next version of your
UI.

Replicant takes this idea and runs with it, narrowing it down even further: The
function is a _pure function_ that returns the new UI as data. Replicant uses
this representation to update the DOM accordingly.

That's it. No partial updates, no mutable objects, no network activity from UI
components. It's basically a templating system. You give it data, and it returns
a view.

## Component trees

Even though React launched the idea that your UI is just a function of your
application state, it also shipped with an escape hatch from day one:
component-local state. Once you add local state to the mix, the component tree
(your UI) is no longer a pure function of the application state. Instead, it is
a stateful mutable object graph.

In top-down rendering where a single pure function takes the application state
and returns the updated UI, there is only one possible data flow. Any observed
visual glitch can trivially be recreated from a snapshot of the application
state.

In stateful component trees, there is no single snapshot of data, and data can
flow in any number of ways. Components can receive conflicting signals, e.g.
local state indicates that a select list should be collapsed, but props from the
parent indicates it should be expanded. Component trees are inherently more
complex.

### Performance

It seems obvious that the more surgical you can make updates to the DOM, the
better performance you should be able to achieve. So a model built on stateful
components should be the better option, right? In theory yes, but not always in
practice.

When you build a tree out of stateful and mutable components, there are no
limits on what kinds of data flows you can create. Unfortunately, this includes
feedback loops and data flows that cause the same components to re-render along
multiple axes. In the worst case this can lead to excessive renders and worse
performance. To add insult to injury, these kinds of problems can be excessively
difficult to debug.

In top-down rendering data can only flow one way, and thus the variability on
the experienced performance is smaller.

Replicant's top-down model can never be the fastest, but that was never the
goal. The goal was to make it fast enough to enable the simpler programming
model of top-down rendering. And it is, for a wide range of use cases.

#### Do you actually have a performance problem?

Many people believe that pure top-down rendering isn't effective enough and that
local state and partial updates is necessary for UIs to achieve acceptable
performance. However, most have never tried top-down rendering, or encountered
performance issues firsthand. It's just one of those things that seem true
because component libraries say so in their documentation.

Over the past decade, I have built a wide range of business applications and
simple games using top-down rendering with tools like React/JavaScript, Ohm,
Quiescent, Reagent, Snabbdom, Dumdom, and Replicant. In all that time, I can
count on one hand the number of instances where I needed to optimize rendering
performance.

### Reusability

By giving components local state and a free pass to do networking and other
side-effects, you can create fully reusable stand-alone components.

An example of a feature rich reusable component is a user menu:

- Knows whether the user is logged in
- Displays a login option for unauthenticated users
- Displays the name of authenticated users
- Can expand to show more details and options for authenticated users

By putting all this in a component, you could just plop the user menu in any
application you build, and have all this functionality "for free".

This style of reusability means teams can work on separate features with less
coordination, and you can ensure consistency on a per-component level across
multiple UIs. But it comes with a cost.

<a id="system-design"></a>
#### Optimizing the system design

Reusable components like `UserMenu` optimize for creating many small fully
featured systems and piecing them together. This means that on a page with `n`
components, you have (in the worst case) `n` separate sources of networking, `n`
approaches to error handling, `n` pieces of state, etc.

Contrast this with top-down rendering: Because the rendering tree can't contain
networking, local state, or indeed any side-effects, all those things must be
solved on the outside. This enables you to solve these aspects systematically
once and once only.

When you solve for your app's side-effects in one place, you can create powerful
centralized infrastructure:

- System-wide caching for all networking
- Batch, pipeline, or debounce events and network request
- Gather all state in one place: easily serialize, save/restore, sync to the
  backend, etc

This approach optimizes for the system as a whole instead of for its building
blocks. In my experience, this is more effective at keeping code bases
manageable over time. In fact, strong centralized machinery allows you to earn
compounding interest on your investments over time -- instead of drowning in
ever increasing technical debt.

On the flip-side, in top-down rendering you cannot create a single `UserMenu`
abstraction. Networking, state management and data processing will need to be
solved separately from rendering. In this way Replicant foregoes fully reusable
components in favor of achieving better reusability at the system level.

#### Less is more

Because Replicant doesn't have components with local state or side-effects, it
can avoid several quite complicated features, like async rendering and state
management. This means a more reliable library, and fewer things that need to
change over time.

## It's all trade-offs

Ultimately, the choice between top-down rendering and stateful component trees
is a trade-off. Replicant takes the stance that optimizing for the overall
system design is more important than having drop-in components that bundle
rendering, state management and networking.

If you share this perspective, you will find Replicant to be a library that
encourages you and pushes you toward a more centralized design -- one that, in
my experience, is your best defence against technical debt.
