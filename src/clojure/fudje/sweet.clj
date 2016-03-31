(ns fudje.sweet
  (:require [fudje.checkers :as checkers]
            [fudje.core :refer [mocking]]))

(defonce anything
  (fudje.checkers/->AnythingChecker))

(defonce irrelevant anything) ;; just a synonym

(defonce truthy
  (checkers/->TruthyChecker))

(defonce falsey
  (checkers/->FalseyChecker))

(defmacro contains
  "A macro to help us simulate the `contains` checker."
  [x & modifiers]
  `(fudje.checkers/->ContainsChecker ~x ~(zipmap modifiers (repeat true))))

(defmacro just
  "A macro to help us simulate the `just` argument-checker."
  [x & modifiers]
  `(fudje.checkers/->JustChecker ~x ~(zipmap modifiers (repeat true))))

(defmacro checker
  "A macro to help us simulate the `checker` checker."
  [& x]
  (if (vector? (first x))  ;;catch this syntactic quirk of midje
    `(fudje.checkers/->CustomChecker (fn ~@x))
    `(fudje.checkers/->CustomChecker ~(first x))))

(defmacro n-of
  "A macro to help us simulate the `n-of` checker."
  [x y]
  `(fudje.checkers/->NofChecker ~x ~y))

(defmacro one-of
  "A macro to help us simulate the `one-of` checker."
  [x]
  `(n-of ~x 1))

(defmacro two-of
  "A macro to help us simulate the `two-of` checker."
  [x]
  `(n-of ~x 2))

(defmacro three-of
  "A macro to help us simulate the `three-of` checker."
  [x]
  `(n-of ~x 3))

(defmacro four-of
  "A macro to help us simulate the `four-of` checker."
  [x]
  `(n-of ~x 4))

(defmacro five-of
  "A macro to help us simulate the `five-of` checker."
  [x]
  `(n-of ~x 5))

(defmacro six-of
  "A macro to help us simulate the `six-of` checker."
  [x]
  `(n-of ~x 6))

(defmacro seven-of
  "A macro to help us simulate the `seven-of` checker."
  [x]
  `(n-of ~x 7))

(defmacro eight-of
  "A macro to help us simulate the `eight-of` checker."
  [x]
  `(n-of ~x 8))

(defmacro nine-of
  "A macro to help us simulate the `nine-of` checker."
  [x]
  `(n-of ~x 9))

(defmacro ten-of
  "A macro to help us simulate the `ten-of` checker."
  [x]
  `(n-of ~x 10))

(defmacro has
  "A macro to help us simulate the `has` checker."
  [x y]
  `(fudje.checkers/->HasChecker ~x ~y))

(defmacro roughly
  "A macro to help us simulate the `roughly` checker."
  [x & y]
  (do
    (assert (number? x) "First argument to `roughly` MUST be a number!")
    `(fudje.checkers/->RoughlyChecker ~x ~(first y))))

(defmacro every-checker
  "A macro to help us simulate the `every-checker` checker."
  [& checkers]
  `(let [cs# ~(vec checkers)]
     (assert (every? fudje.util/poly-checker? cs#) "`every-checker` expects checkers!")
     (fudje.checkers/->EveryChecker cs#)))


(defmacro split-in-provided-without-metaconstants
  "Given a some typical Midje code where some `provided` clauses (mocks) follow the actual test assertions,
  separate the assertions from the mocks. Also replaces all meta-constants with keywords.
  Returns a vector of [<forms-before-provided (assertions)> <forms-after-provided (mocks)>]."
  [forms]
  `(->> ~forms
        (split-with #(if (list? %)
                      (not= :provided (keyword (symbol (first %))))
                      true))
        (map (partial clojure.walk/postwalk (fn [form#]
                                              (let [[mc?# to-replace#] (fudje.util/metaconstant? form#)]
                                                (if mc?#
                                                  (fudje.util/metaconstant->kw form# to-replace#)
                                                  (if (fudje.util/throwable? to-replace#) ;;we do not support replacing funciton-metaconstants, check for the Exception object here
                                                    (throw to-replace#)
                                                    form#))))))))


(defn- expand-tests
  "Map midje symbols `=>` & `=throws=>` to `is` & `(is (thrown? ...` respectively."
  [tests]
  (for [[t symb res] tests]
    (condp = symb
      '=> `(let [expected# ~res] ;; this is the common case
             (if (fudje.util/poly-checker? expected#)
               (clojure.test/is (~'compatible expected# ~t))
               (clojure.test/is (= ~res ~t))))
      '=throws=> (let [[xxx msg] res     ;; <xxx> will either be a Class or an Exception object
                       xxx-klass (cond-> xxx
                                         (fudje.util/throwable? xxx) class)]
                   (if (nil? msg)
                     `(clojure.test/is (~'thrown? ~xxx-klass ~t))
                     `(clojure.test/is (~'thrown-with-msg? ~xxx-klass ~(cond-> msg
                                                                               (string? msg) re-pattern) ~t))))
      (throw (IllegalArgumentException. ^String (format "arrow %s not recognised!" (str symb)))))))



(defmacro fact
  "An (almost) drop-in replacement for `midje.sweet/fact` which rewrites the code to not use Midje.
  Rudimentary support for converting top-level midje checkers exists (`contains` & `just`).
  ATTENTION: `let` bindings right after a `midje.sweet.fact` should be manually pulled one level up, before switching to `novate.sweet.fact`."
  [description & forms]
  (assert (string? description) "`novate.sweet/fact` requires a String description as the first arg (per `clojure.test/testing`)...")
  ;; MAKE SURE THERE IS NO EXPRESSION SHADOWING THE MOCKS:
  ;; we want to avoid a situation where the `provided` clauses are not visible when splitting the entire `fact` into assertions & mocks.
  ;; For example the following is bad because there are 2 nesting levels separating the `fact` & `provided` symbols, whereas there should only be 1.
  ;;(fact ""
  ;;  (let [a 1]
  ;;    (x a) => 5
  ;; (provided
  ;;   (y 2) => 6)))
  ;;
  ;; Instead of bypassing this silently (which renders the test useless), we want to make this a compile-time error.
  ;; However, we need to take extra care to allow for similar looking expressions that don't violate the rule.
  ;; For example the following should be allowed:
  ;;
  ;; (fact ""
  ;;  (let [a 1]
  ;;    (x a)) => 5 ; notice how the `let` closes here instead of at the end
  ;; (provided
  ;;   (y 2) => 6))

  ;; first thing we need to know is whether the original expression we're rewriting comtains any mocks
  (let [mocks-present? (some true? (map #(with-local-vars [mocks? false]
                                          (clojure.walk/prewalk (fn mock-flag? [x]
                                                                  (do (when (= 'provided x)
                                                                        (var-set mocks? true))
                                                                      x))
                                                                %)
                                          (var-get mocks?))
                                        forms))
        [pre-provided post-provided] (fudje.sweet/split-in-provided-without-metaconstants forms)

        ;; following assertion covers the case where the mock expressions have been shadowed by some expression (e.g `let`),
        ;; and as such the whole expression cannot be reliably split into assertions VS mocks. There is just no way to get well-formed assertions,
        ;; if there is a `let` right under the `fact` and is consuming the whole body of the `fact`. The only way to get well-formed assertions while mixing e.g. `let` expressions,
        ;; is to limit the `let`'s scope to either the left, or the right side of the assertion, in which case it is perfectly legal to do so and should be allowed.
        _ (assert (zero? (rem (count pre-provided) 3))
                  "Well-formed assertions should have 3 identifiable parts (e.g. `x => y`)...\nIt is likely that some expression (e.g `let`) is shadowing your assertion(s), and potentially the mocks as well! If so, manually lift that expression outside the `fact`, and try again.")

        mocks (-> post-provided first rest)

        ;; following assertion will fire if the mocks are empty, or if they are not well-formed (as triplets similar to the assertions).
        ;; In absence of spelling errors, and since the assertion before it covers the 'shadowing' issue, the mocks should never be empty at this point
        ;; (given that the original expression did contain a 'provided symbol). So here, we're mainly checking that the mocks are indeed triplets.
        _ (when mocks-present?
            (assert (and (not-empty post-provided)
                         (zero? (rem (count mocks) 3))) "Well-formed mocks should have 3 identifiable parts (e.g. `x => y`)... "))

        tests (->> pre-provided
                   (partition 3)
                   (map (fn [[t _ r :as x]]
                          (if (and (list? r)
                                   (= 'throws (symbol (first r))))
                            [t '=throws=> (rest r)]   ;; this requires special treatment (see `expand-tests`)
                            x)))
                   )
        mocks (-> post-provided first rest)]
    `(clojure.test/testing ~description
       (fudje.core/mocking ~mocks
         ~@(fudje.sweet/expand-tests tests)))
    ))



(defmacro are* ;;
  "Slightly patched version of `clojure.test/are` that assumes that <expr> is a `fact` or `mocking`.
   Only difference is that <expr> is NOT evaluated within the context of a `clojure.test/is` as the `fact` takes care of that.
   Not really intended for public use but it has to stay a public Var."
  {:added "1.1"}
  [argv expr & args]
  (if (or
        (and (empty? argv) (empty? args))
        (and (pos? (count argv))
             (pos? (count args))
             (zero? (mod (count args) (count argv)))))
    `(clojure.template/do-template ~argv ~expr ~@args)
    (throw (IllegalArgumentException. "The number of args doesn't match are's argv."))))


(defmacro tabular [fact-expr & body]
  (with-local-vars [arga #{}]
    (clojure.walk/prewalk (fn [x]
                            (if (fudje.util/qmark? x)
                              (do (var-set arga (conj @arga x)) x)
                              x))
                          fact-expr)
    (let [[args assertions] (split-at (count @arga) body)]
      `(fudje.sweet/are* ~(vec args) ~fact-expr ~@assertions))
    )
  )


