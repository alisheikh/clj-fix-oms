# clj-fix-oms

## What is it?
clj-fix-oms is a light-weight order management system intended for use with [clj-fix](https://github.com/nitinpunjabi/clj-fix). It accepts translated execution reports from clj-fix and updates orders as required.

## Why does it exist?
clj-fix-oms was created solely to assist in testing other development and, for now, is *not* recommended for production use. It is released here under the MIT license.

## Related repos
- [clj-fix](https://github.com/nitinpunjabi/clj-fix)
- [fix-translator](https://github.com/nitinpunjabi/fix-translator)

## Installing (Leiningen)
```Clojure

;Include this in your project.clj:
[clj-fix-oms "0.4"]

; Example:
(defproject my-project "1.0"
  :dependencies [[clj-fix-oms "0.4"]])
```

## Usage
```Clojure

; In your ns statement:
(ns my-project.core
  (:use clj-fix-oms.core)
```

## How to use clj-fix-oms
__1__. [Create a FIX client](https://github.com/nitinpunjabi/clj-fix#steps-to-create-a-fix-client-using-clj-fix) using clj-fix. To use clj-fix-oms, your client must set clj-fix to translate incoming FIX messages.

__2__.Upon receiving a translated Execution Report, pass it to clj-fix-oms:
```Clojure
(update-oms new-msg)
```

__3__. Two functions are available to retrieve orders:
```Clojure
; find-order, the params for which are [order-id]
(find-order order-id)

; get-best-priced-order, the params for which are [symbol side]
(get-best-priced-order "NESNz" :buy]

```

## To-Dos
- Move order storage to a priority queue.
- Have clj-fix-oms track positions