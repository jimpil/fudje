(ns fudje.core
  (:require [fudje
             [data :as data]
             [util :as ut]]
            [clojure.test :as test])
  (:import [java.util.concurrent.atomic AtomicLong]))

(def ^:dynamic *report-mock-state*  false)

;======================================================================
;;extend what `clojure.test/is` recognises for our own benefit :)

(defmethod test/assert-expr 'compatible
  [msg form]
  `(let [probably-expected# ~(nth form 1)
         probably-supplied# ~(nth form 2)
         [expected# supplied#] (if (fudje.util/poly-checker? probably-supplied#)
                                 [probably-supplied# probably-expected#]
                                 [probably-expected# probably-supplied#]) ;; make sure we got the order right
         clean-form# (list '~'compatible expected# supplied#)
         [only-expected# only-value# both#] (data/diff expected# supplied#)
         failure# (boolean (cond-> only-expected#
                                   (coll? only-expected#) seq))]
     (if only-expected#
       (test/do-report {:type :fail, :message ~msg :expected clean-form#, :actual (list
                                                                               '~'found only-value#
                                                                               '~'instead-of only-expected#)})
       (test/do-report {:type :pass, :message ~msg :expected clean-form#, :actual both#}))
     (not failure#)))

;================================================================================================================

(defn- apply-mock [mock args]
  (let [mock-fn (if (ut/throwable? mock)
                  (fn [& _] (throw mock))
                  mock)]
    (cond-> mock-fn
            (fn? mock-fn) (apply args))))

(defn arg-checking-wrapper [f original-mock-fn args-to-check group-no]
  (let [^AtomicLong mcounter (AtomicLong. 0)
        groups? (when (> group-no 1) true)]
    (bound-fn [& args]
      (if fudje.core/*report-mock-state*
        {:actual-calls (.get mcounter)
         :expected-calls group-no}

        (let [vargs (vec args)
              curr-i (.getAndIncrement mcounter)
              relevant-arg-group (try (cond-> args-to-check  ;;only test the relevant arg-group, if there is a state-counter
                                        groups? (nth curr-i))
                                      (catch IndexOutOfBoundsException _
                                        ;;simply cause a test failure at this point
                                        (clojure.test/is false (str "`" f "`" " was called MORE times than mocked!"))))]
          (assert (= (count vargs)
                     (count relevant-arg-group))
                  (str "Number of arguments between the mock-call and the actual function call don't match! Aborting ...\n**Mock-args: " relevant-arg-group "\n**Actual-args: " vargs))

          ;; use our new assertion-expr with `is` as it was meant to be
          (clojure.test/is (compatible relevant-arg-group vargs)
                           (if groups?
                             (str "Function `" f "` (call " (inc curr-i) ") was called with unexpected arguments!")
                             (str "Function `" f "` was called with unexpected arguments!")))

          (apply-mock (cond-> original-mock-fn ;; if there is a state-counter <mock-fn> is a list of values
                              groups? (nth curr-i)) vargs))))))

(defmacro make-mocks [mock-forms]
  `(let [triplets# (partition 3 ~mock-forms)
         [mock-outs# mock-ins#] ((juxt (partial mapv first)
                                       (partial mapv last))
                                  triplets#)
         mocks# (->> mock-outs#
                     (map-indexed (fn [index# item#]
                                    (if (list? item#)  ;;extract the function from the expression
                                      (let [fsym# (first item#)
                                            rsym# (vec (next item#))]
                                        (if (-> fsym# meta :multimock)
                                          (let [arg-groups# (vec (second item#))
                                                group-count# (count arg-groups#)]
                                            [fsym# `(arg-checking-wrapper ~fsym# ~(nth mock-ins# index#) ~arg-groups# ~group-count#)])
                                          [fsym# `(arg-checking-wrapper ~fsym# ~(nth mock-ins# index#) ~rsym# 1)]))
                                      [(with-meta item# {:novate.test/stateless true}) (nth mock-ins# index#)])))
                     (apply concat))
         resolved-syms# (->> mocks#
                             (take-nth 2)
                             (mapv (juxt identity resolve)))] ;;resolve them all before diving into syntax-quoting!
     [mocks# resolved-syms#])
  )


(defmacro mocking
  "A typical/simplistic Midje test looks like this:
   Assuming a dummy function `calculate` which calls functions `add`, `multiply`, `subtract` & `divide`:

   (defn calculate [a b c d]
   (-> 0 (add a)
         (multiply b)
         (subtract c)
         (divide d)))

  (fact \"testing `calculate`\"
    (calculate 2 -4 -2 -4) => -3
    (provided
      (add 0 2) => (+ 0 2)
      (multiply 2 -4) => (* 2 -4)
      (divide 12 -4) => -3
      (subtract -8 -2) => 12 ] ;; wrong on purpose!
      )))

  We can re-write this using vanilla `clojure.test` as so:

  (testing \"testing `calcualte`\"
    (mocking [(add 0 2) => (+ 0 2)
              (multiply 2 -4) => (* 2 -4)
              (divide 12 -4) => (/ 12 -4)
              (subtract -8 -2) => 12]  ;; wrong on purpose!
      (is (= -3 (calculate 2 -4 -2 -4))
    )))

  For mocking Exceptions Midje relies on `=throws=> (Exception. <msg>)` pattern.
  There is no need for us to do that. Any Throwable Object is a perfectly valid mock value.
  For maximum consistency, simply replace `=throws=>` with `=>`.
  For example:

    (/ 12 0) =throws=> (ArithmeticException. \"Can't divide by zero!\")
    becomes:
    (/ 12 0) => (ArithmeticException. \"Can't divide by zero!\")

  However, it's worth noting that this step is completely optional.
  The symbol you use is actually never evaluated and as such, is rather irrelevant.

  For confirming that the actual function we're testing throws Exception,
  use `(is (thrown? ...))` as usual.
  =====================================================================
  Notes:

  1)Mocking the same function more than once, is achieved by the ^:multimock metadata on the symbol, followed by a list of arg-lists,
    followed by the `=>` arrow, and finally a list of expected return values (as many as arg-lists provided and in the same order).
  For such a test to pass, any mocked fn MUST be invoked exactly N times, where N is the number of arg-lists/return-vals mocked.
  Example:

      (mocking [(^:multimock F [['anything] ['anything]]) => (repeat 2 :some-number)] ;;we've mocked 2 arg-lists AND 2 return values
      (is (= :some-number (F 6)))
      (is (= :some-number (F 7))))) ;; calling F one more/less time will cause a test failure teling you that `six-times` was called less times than mocked

  2)Mocking exeptions can ONLY be done via concrete Objects, whereas asserting that the tested function-call throws exception can be done via Class objects as well.
   "
  [mock-forms & tests]
  (assert (zero? (rem (count mock-forms) 3)) "`mocking` expects a vector of triplets [& [mock-out => mock-in]]!")
  (let [[mocks resolved-syms] (make-mocks mock-forms)]
    `(with-redefs [~@mocks]
       (try ~@tests
            (finally
              (binding [fudje.core/*report-mock-state* true]
                (doseq [[s# f#] '~resolved-syms]
                  (when-let [state# (when-not (-> s# meta :novate.test/stateless) (f#))] ;; returns `{:actual-calls x, :expected-calls y}` OR nil
                    (clojure.test/is (= (:expected-calls state#)
                                        (:actual-calls state#))
                                     (str "`" f# "`" " was NOT called the right number of times!")))))
              ))
       )))


(defmacro in-background
  "A thinner version of `mocking` which doesn't do any call-count checking.
  Intended as a replacement over midje's `against-background`."
  [mock-forms & body]
  (assert (zero? (rem (count mock-forms) 3)) "`in-background` expects a vector of triplets [& [mock-out => mock-in]]!")
  (let [[mocks _] (make-mocks mock-forms)]
    `(with-redefs [~@mocks]
       ~@body)))



