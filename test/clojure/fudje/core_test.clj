(ns fudje.core-test
  (:require [clojure.test :refer :all]
            [fudje.core :refer :all]
            [fudje.sweet :refer :all]))

;; a couple of functions so that we have something to work with
(defn twice [x] (* x 2))
(defn six-times [y] (* (try (twice y)
                            (catch Exception _ 10)) 3))

(defn wrand  ;copied from novate.test.random
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

(defn plus [x y]
  (+ x y))
(defn times [x y]
  (reduce plus 0 (repeat x y)))


(deftest mocking-tests
  (testing "`mocking` macro simple case 1"
    (mocking [twice => (partial * 3)] ;; wrong on purpose!
             (is (= 45 (six-times 5)))))

  (testing "`mocking` macro simple case 2"
    (mocking [twice => (partial * 3)
              six-times => (fn [_] (* 2 10))] ;; wrong on purpose - renders the first redef a no-op !
             (is (= 20 (six-times 5)))))

  (testing "`mocking` macro parameter testing"
    (mocking [(six-times {:a [1 2 3] :b #{:x :y :z}}) => :some-number]
             (is (= :some-number (six-times {:a [1 2 3] :b #{:x :y :z}}))))) ;; changing the very last keyword (:z => :t) makes the test fail

  (testing "`mocking` macro anything' parameter testing and stateful multimock"
    (mocking [(^:multimock six-times [[anything] [anything]]) => (repeat 2 :some-number)]
             (is (= :some-number (six-times 6)))
             (is (= :some-number (six-times 7))))) ;; calling it with any argument is valid but only 2 times!

  (testing "`mocking` macro anything' parameter testing - stateless mock"
    (mocking [six-times => (constantly :some-number)]
             (is (= :some-number (six-times 6)))
             (is (= :some-number (six-times 7))))) ;; calling it with any argument is valid AND any amount of times

  (testing "`mocking` macro mock Exception case"
    (mocking [(twice 2) => (Exception. "SOME EXCEPTION")]
             (is (= 30 (six-times 2))))) ;; Exception did fire and we got (* 10 3)

  (testing "`mocking` macro final Exception thrown case"
    (mocking [(six-times 5) => (Exception. "SOME EXCEPTION")]
             (is (thrown? Exception (six-times 5)))))

  (testing "`mocking` with multi-mocks"
    (mocking [(six-times anything) => 25
              (^:multimock plus ([0 3] [3 3] [6 3])) => [3 6 10] ;;wrong on purpose
              ]
             (is (= 25 (six-times 5)))
             (is (= 10 (times 3 3)))))

  (testing "`mocking` with nested checkers works"
    (mocking [(six-times (just {:a (contains [2 3]) :b (has every? keyword?)})) => :some-number] ;; could have used `checker` instead of `has`
             (is (compatible :some-number (six-times {:a [1 2 3 4] :b #{:x :y :z}}))))
    )
  (testing "`mocking` with checkers inside the multi-mock AND in the final assertion"
    (mocking [(six-times anything) => 25
              (^:multimock plus ([0 3] [3 3] [(checker (partial = 6)) 3])) => [3 6 {:result 10}] ;;wrong on purpose
              ]
             (is (= 25 (six-times 5)))
             (is (compatible (contains {:result (checker (partial = 10))}) (times 3 3)))))

  (tabular
    (testing  "wrand returns correct index"
      (mocking [(rand ?sum) => ?rand]
        (is (= ?index (wrand ?weights)))))
    ?weights      ?sum ?rand ?index
    [3 6 7 12 15] 43   1     0
    [3 6 7 12 15] 43   3     1
    [3 6 7 12 15] 43   9     2
    [3 6 7 12 15] 43   16    3
    [3 6 7 12 15] 43   28    4
    [3 6 7 12 15] 43   42    4)

  )

(deftest background-tests
  (testing "in-background"
    (in-background [(plus anything anything) => (six-times 2)]
                   (is (= 12  (times 2 2))))))

