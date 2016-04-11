# Introduction to fudje

The following intro is divided in 2 major sections. The first one will assume you've never heard of midje before, and will simply explain what the `mocking` macro does and how to use it (which is the whole API really). 
It will also cover how to use the checkers provided. The second section will be about automatically rewriting existing midje tests, and as such we will assume, at least some, familiarity with midje.


## Writing new tests

fudje.core basically exposes a single macro called `mocking` (it actually, exposes `in-background` as well, but that's irrelevant for now).
It looks like this:

```clj
(mocking [& mocks]
  & assertions)
```

At first glance, it sort of syntactically resembles `clojure.core/binding` & `clojure.core/with-redefs`. As you probably guessed, there can be any number of mocks in mocking vector, and any number of assertions following it. But what exactly is a mock or an assertion? If you've ever seen or written tests using vanilla clojure.test, then you're already familiar with what assertions look like.
In other words, fudje has no strong opinions regarding how you express your assertions. Your existing clojure.test assertions (based on `is` will work).

Let's look at a simplistic case...
Assume for a minute that you don't need to do any mocking (which is perfectly valid btw). You could write something like this:

```clj
(mocking [] ;; don't do that
  (is (= 4 (* 2 2))))
```
Obviously, using `mocking` with an empty vector makes little to no sense, but this will work regardless. So as said, you already know what an assertion looks like. It is an expression which somehow/somewhere is going to eventually call `is` and register either a success or a failure. You can have as many of those as you want following the mocking-vector. Let's now make things interesting and look at mocks...

### stubbing & mocking

Clojure already provides excellent facilities for stubbing (i.e. `with-redefs`). However, for *mocking* a specific function call with specific arguments, you're left on your own. This is where `mocking` comes to save the day. You can use it for either stubbing or mocking or a combination thereof. Using it for stubbing, is basically a convenience, since it allows you to skip surrounding your `mocking` expression with a `with-redefs` one.
 
 So we've already seen the syntax for `mocking`. A mock is basically a triplet like the following:
 
```
 mock-out => mock-in
``` 

Generally speaking, the `mock-in` replaces the `mock-out`. If `mock-out` is a single symbol, then you will get stubbing semantics for that 'mock'. In other words, you can mix & match stubs with mocks within `mocking`. Mind you, the symbol in between is never evaluated and therefore can be anything. 

Let's make things more concrete though...First let's define some functions that are not inlined (so we can mock them).

```clj

(defn increment [x] 
  (inc x))
  
(defn decrement [x] 
  (dec x))


(mocking [(increment 1) => (dec 1)
          (decrement 2) => 3]
          
  (is (= 0 (increment 1)))
  (is (= 3 (decrement 2))))

```


Ok, now you've seen the basic syntax. For the most part this is what you will be dealing with. However, since `mocking` basically boils down to `with-redefs`, there is a catch. You cannot have, the same function/function-invocation mocked more than once. The syntax for what fudje calls a *multimock* is slightly different... 

A **multimock** must have the following form:

```clj
(^:multimock f [& arg-lists]) => [& return-lists]
```

Let's look at one example:

```clj
(mocking [(^:multimock increment [[1] [2]]) => [(decrement 1) -1]] ;; must provide as many return values as arg-lists
          
  (is (= 0 (increment 1)))
  (is (= -1 (increment 2))))

```

If you can't possibly enumerate the invocations, then you're probably looking for a stub, rather than a mock.

That is it! You now know how to use `mocking`...

`in-background` is simply a thinner version of `mocking`, which is something between mocking and stubbing. `in-background` will check the arguments provided but will not do any call-count checking at the end (as opposed to `mocking` which will). In other words, you can think of it as a slightly more capable version of `with-redefs`. As such, the intention is for it to surround multiple `mocking` expressions in order to provide mocking for expressions which we don't care how many times they are called. Midje does this via `against-background` and a couple of other variants.  



### Checkers 

There is not a lot to say here...
All the checkers have been inspired by *midje* and therefore offer similar functionality. 

* `contains` (mainly for collections, but also works with String/RegexPattern against String)
* `just` (for collections)
* `roughly` (for numbers)
* `checker` (for custom checking - expects a fn)
* `n-of` (mainly for collections - `one-of`, `two-of`, `three-of` etc, up to 10, are provided)
* `has` (mainly for collections but not limited)
* `every-checker` (`and` for checkers)
* `anything` or `irrelevant` (self explanatory)

Notes:
 
* `(contains <something-sequential> :in-any-order)` has the potential of being very slow when used with large-ish inputs (e.g. more than 7-8 elements). This is because, fudje will try to diff all permutations of the expected coll against all partitions of the supplied one (`(partition (count expected) 1 supplied)`)! Therefore, I'd recommend that you don't use this with an expected value counting more than 5-6 elements. Obviously, if the supplied value is also not too big either, that's great, but the most of the cost would come from generating the permutations, rather than the partitions.
  
* `checker` can be used either with an fntail (a-la midje: `(checker [x] (== 5 x))`) or an actual fn object (a-la fudje: `(checker (fn [x] (== 5 x)))`). I recommend the latter but fudje has to support the former too, in order to be able to translate the midje version without manual intervention. 
   

* `contains-in` a version of `contains` which removes the boilerplate when asserting membership of some value in nested structures. Only works against maps and vectors. For example, instead of writing the following:

```clj
(fact "..."
  {:a {:b {:c "1"}}} => (contains {:a (contains {:b (contains {:c (checker string?)})})}))

```

you can write this instead:

```clj
(fact "..."
  {:a {:b {:c "1"}}} => (contains-in {:a {:b {:c (checker string?)}}})

```


* `just-in` a version of `just` which removes the boilerplate when asserting precise membership of values in nested structures. Only works against maps and vectors. 
  
Let's see some short examples:

```clj

(is (compatible (contains {:a (contains {:b (checker pos?)})})
                {:a {:b 2}
                 :c 3}))
                 
(is (compatible (contains {:a (has every? neg?)})
                {:a [-1 -2 -3 -4]})) 
                                
(is (compatible (contains [4 5 6] :gaps-ok)  ;; order matters, intermediate elements don't
                [1 2 3 4 'hi 5 :yes 6 7 8 9]))
                                                 
(is (compatible (contains [4 5 6] :in-any-order :gaps-ok)  ;; order doesn't matter, nor do intermediate elements 
                [1 2 3 4 'hi 7 :yes 6 5 8 9]))                                                 

(is (compatible [{:a 1} {}] ;; order of arguments passed to `compatible` doesn't matter
                (two-of map?)))

;; Bare functions are NOT supported as checkers.                
(is (compatible {:a pos?} {:a 1}))           ;; will NOT work in fudje 
(is (compatible {:a (checker pos?)} {:a 1})) ;; need to use a `checker`                
```

Similarly to midje, checkers can be used both as result-checkers in the assertion (as shown above) and as arg-checkers in the mocking-vector (as shown below).

```clj
(testing "arg-checker in mocking vector"
  (mocking [(f (just {:a (contains [2 3]) 
                      :b (has every? keyword?)})) => :some-number] 
    (is (= :some-number (f {:a [1 2 3 4] :b #{:x :y :z}})))))
```

Please have a look at the test checkers-test.clj namespace for more demo usage.

We're done! You now know how to use fudje!

### Migrating existing midje facts

Fudje is able to automatically rewrite a **well-formed** midje fact, via its own `fact` macro, so, in theory, you should be able to get rid of the midje dependency of a project, by simply replacing `midje.sweet` => `fudje.sweet` in all its test ns declarations. Now, why 'well-formed' and why 'in-theory'? Let's first look at what constitutes a 'well-formed' midje fact (according to fudje). Here is one:


```clj
(let [...]
  (fact "some-description"
    & assertions
    (provided 
      & mocks)))
```

The difference of the above snippet and the one below, is subtle. Midje doesn't mind either, but fudje will not be able to split the latter into its various parts correctly. In particular, it won't be able to separate the assertions from the mocks. The crucial symbol here is `provided`. That's where we want to make the split. However, the `provided` expression below has been swallowed by the `let`. As a result, it is 2 levels below `fact`, and fudje won't be able to split it correctly. 

```clj
(fact "some-description"
  (let [...] ;; don't do that
    & assertions
    (provided 
      & mocks)))
```

So, to sum up, fudje considers a `fact` well-formed, when the `provided` symbol is only 1 level below the `fact` symbol. If it is not, then you have to start by manually pulling that `let` (or `binding` or whatever) outside the `fact` (as shown 2 snippets above). You can then let `fudje.sweet/fact` do its thing. 

Here are some short examples:

```clj

(defn twice [x] 
  (* x 2))
  
(defn six-times [y] 
  (* (try (twice y)
       (catch Exception _ 10)) 
      3))
      
      
(fact "" 
  (six-times ...three...) => 24
  (provided
    (twice ...three...) => (* 2 4))) 

(fact ""
  (six-times ...three...) => 24
  (provided
    (twice anything) => (* 2 4)))

(let [ten 10 thirty 30]
  (fact ""
    (six-times ten) => thirty
    (provided
    (twice ten) =throws=> (Exception. "whatever"))))
```

All the above tests are valid in both midje AND fudje. It all depends which `fact` you're using. The same goes for `tabular`. The following 3 snippets are all equivalent:


```clj
(defn wrand ;;need a fn to test
  "given a vector of slice sizes, returns the index of a slice given a
  random spin of a roulette wheel with compartments proportional to
  slices."
  [slices]
  (let [total (reduce + slices)
        r (rand total)]
    (loop [i 0 sum 0]
      (if (< r (+ (slices i) sum))
        i
        (recur (inc i) (+ (long (slices i)) sum))))))
;--------------------------------------------------------
;; 1) midje style (in theory you can carry on writing this)
(tabular
  (fact "wrand returns correct index"
    (wrand ?weights) => ?index
      (provided
        (rand ?sum) => ?rand))
        
    ?weights      ?sum ?rand ?index
    [3 6 7 12 15] 43   1     0
    [3 6 7 12 15] 43   3     1
    [3 6 7 12 15] 43   9     2)

;; 2) half-and-half style (a bit more evident IMO as it doesn't use `fact`)
(tabular
  (testing  "wrand returns correct index"
    (mocking [(rand ?sum) => ?rand]
      (is (= ?index (wrand ?weights)))))
      
    ?weights      ?sum ?rand ?index
    [3 6 7 12 15] 43   1     0
    [3 6 7 12 15] 43   3     1
    [3 6 7 12 15] 43   9     2)    

;; 3) recommended traditional clojure.test style (MUST use `fudje.sweet/are*`)
(are* [weights sum rando index] 
  (mocking [(rand sum) => rando]
    (is (= index (wrand weights))))
    
  [3 6 7 12 15] 43   1     0
  [3 6 7 12 15] 43   3     1
  [3 6 7 12 15] 43   9     2 )
    
```

BTW, the 3 snippets above also demonstrate, the most outer macro-expansion needed in order to achieve code-rewrite. In other words, you can see how an expression which is written entirely in terms of midje (1st one), ends up being rewritten to, essentially, vanilla `clojure.test` code (3rd one). First we convert, `fact` to `mocking`, and then `tabular` to `are*`. I'd personally vote for variant #3 as it requires the least pre-processing, and also because it will probably look a lot more familiar to others.  
 
 
## Final words
 
As you may have gathered by now, *fudje* is based on `clojure.test`, `clojure.data`, `with-redefs` & a bunch of `deftype`s (the checkers). As a result, the implementation is pretty straight forward. Macros are used solely for code-rewrite, and there are no occurrences of `eval` or `*ns*`. This basically means that *fudje* is 100% AOT friendly. 

I sincerely hope you find it as useful as we have!

### Keep Calm and Carry On Testing :)
