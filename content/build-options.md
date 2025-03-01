--------------------------------------------------------------------------------
:page/uri /build-options/
:page/title Build options
:page/kind :page.kind/guide
:page/order 70

--------------------------------------------------------------------------------
:block/markdown

Replicant supports a few build options that toggles certain features on or off.
By default, Replicant tries to set these automatically appropriately for
development and production so hopefully you will not need to manually tinker
with these.

The easiest way to set Replicant build options is to add them alongside other
compiler options:

--------------------------------------------------------------------------------
:block/lang clj
:block/code

{:main "myapp.dev"
 :optimizations :none
 :pretty-print true
 :source-map true
 :asset-path "/js/dev"
 :output-to "public/js/app.js"
 :output-dir "public/js/dev"
 :replicant/asserts? false} ;; <==

--------------------------------------------------------------------------------
:block/markdown

You can also set Replicant build options with JVM properties. When doing so, use
the option name with a dot in place of the slash and without any question marks
-- `:replicant/asserts?` can be set with the `replicant.asserts` property.

--------------------------------------------------------------------------------
:block/id options
:block/title Options
:block/level 2
:block/markdown

### `:replicant/dev?`

Controls whether Replicant is in development mode or not. Defaults to `false`
when the ClojureScript compiler options use `:optimizations :simple` or
`:advanced`, and `true` otherwise. Development mode is used to default other
build options.

When Replicant is in development mode, it also forces a full re-render when any
alias function is changed.

### `:replicant/asserts?`

When this option is enabled, Replicant performs a bunch of sanity checks during
rendering and warns you about inefficiencies and potential mistakes in your
hiccup. These asserts are meant to help you catch mistakes early during
development, and avoid running into actual problems in production.

<img src="/images/assert.png" alt="Example assertion error log">

This setting defaults to `true` in development mode. If you have a lot of
problems, the printing mechanism can slow down rendering. The recommended way to
deal with this problem is to fix the problems, but if that's not feasible, you
can also disable asserts.

### `:replicant/catch-exceptions?`

When set to `true`, Replicant will catch exceptions in a few critical places to
avoid small errors resulting in nothing being rendered. The exceptions Replicant
currently guards against are:

- Trying to add an illegally named attribute
- An alias function that throws

When Replicant catch these errors, it will log an error and print the full
exception.

This setting defaults to `false` during development and `true` in production.
When it is `false`, Replicant inserts no `try/catch` anywhere, and you are free
to use the debugger to inspect problems, etc.
