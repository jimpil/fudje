# fudje
![fudge](https://www.bbcgoodfood.com/sites/default/files/styles/bbcgf_recipe/public/member-recipes/vanilla_fudge.jpg)

## What
A tiny (~20kb jar) Clojure library designed to offer a helping hand when testing. For those familiar with [midje](https://github.com/marick/Midje), 
you can think of think of this as 'midje-lite'. It provides the majority of features provided my midje, at a fraction of the cost 
(code-bloat, number of transitive dependencies, AOT issues etc).
In addition, fudje offers a convenient migration path away from midje, by introducing its own `fact` & `tabular` macros which
rewrite your existing midje facts into midje-free code (more on this in the intro.md). This allows for smooth & gradual migration, 
as you are, not only able to start writing new tests, but also automatically-(ish) migrating old ones. 

## For whom

This library can (potentially) be of interest to you if:

* You are looking for simple unit-testing library which supports mocking and allows you to write concise tests in a familiar syntax.
* You generally like the features offered by midje (mocking, nesting-checkers etc), but you are not overly thrilled about the syntax/complexity it introduces, and therefore wouldn't mind something less *magical*.
* You are looking for something like midje which can be AOT compiled. 
* You have a large codebase full of midje tests, and for whatever reason, you'd like to migrate away from it (without a full manual rewrite).

This library will NOT be of any interest to you if:

* You are perfectly content with vanilla clojure.test, or some other testing library.
* You never liked/appreciated midje, and even the **slightest** resemblance will put you off.


## Where
[![Clojars Project](https://clojars.org/fudje/latest-version.svg)](https://clojars.org/fudje) 

## How

* For migrating an existing test ns away from midje, you want to replace `midje.sweet` => `fudje.sweet` in the ns declaration.
* For writing brand new tests, you probably want something along these lines:

```clj 
(require '[fudje 
           [core :refer [mocking in-background]]
           [sweet :refer :all]])
```

## Example:

```clj

(defn increment [x] 
  (inc x))
  
(defn decrement [x] 
  (dec x))


(mocking [(increment 1) => (dec 1)
          (decrement 2) => 3]
          
  (is (= 0 (increment 1))) 
  (is (= 3 (decrement 2))))

=> true
  
(mocking [(increment 1) => :whatever]        
  (is (= :whatever (increment 2))))  ;; 1 failure (wrong argument passed) 

Fail in blah-blah-blah
Function `user$increment@58d6dddc` was called with unexpected arguments!"
expected: [1]
  actual: [2]

=> true
```

Please consult the [intro](https://github.com/jimpil/fudje/blob/master/doc/intro.md) for a more comprehensive demo.

## Requirements

Fudje has been tested against Clojure versions 1.5.1, 1.6, 1.7 and 1.8, but should work with earlier versions as well (possibly down to 1.2). The oldest JDK you can use is 1.6. 

## License

Copyright © 2016 Dimitrios Piliouras

Distributed under the Eclipse Public License, the same as Clojure.
