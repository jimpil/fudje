# fudje  
![fudge](http://halalsweeties.com/images/images_big/Vanilla-Fudge.png)

A tiny Clojure library designed to offer a helping hand when testing. For those familiar with [midje](https://github.com/marick/Midje), 
you can think of think of this as 'midje-lite'. It provides the majority of features provided my midje, at a fraction of the cost 
(code-bloat, JAR size, AOT issues etc).
In addition, fudje offers a convenient migration path away from midje, by introducing its own `fact` & `tabular` macros which
rewrite your existing midje facts into midje-free code (more on this in the docs). This allows for smooth & gradual migration, 
as you are, not only able to start writing new tests, but also automatically-(ish) migrating old ones. 

## Where

FIXME (leiningen coordinates coming soon)

## How

* For migrating an existing test ns away from midje, you want to replace `midje.sweet` => `fudje.sweet` in the ns declaration.
* For writing brand new tests, you probably want something along these lines:

```clj 
(require '[fudje 
           [core :refer [mocking in-background]]
           [sweet :refer :all]])
```

## Requirements

Clojure 1.7 (or greater) is the only requirement.

## License

Copyright © 2016 Dimitrios Piliouras

Distributed under the Eclipse Public License, the same as Clojure.
