(ns fudje.sweet-test
  (:require [clojure.test :refer :all]
            [fudje.sweet :refer :all]
            [fudje.core-test :refer [plus wrand twice six-times times]]))

(deftest fact-tests

  (fact "`fact` code re-write with metaconstants" ;;this is how midje tests look like
        (six-times ...three...) => 24
        (provided
          (twice ...three...) => (* 2 4))) ;;wrong on purpose

  (fact "`fact` code re-write with metaconstants + anything'"
        (six-times ...three...) => 24
        (provided
          (twice anything) => (* 2 4)))

  (let [ten 10 thirty 30]
    (fact "`fact` code re-write with exception mock AND outer `let` bindings"
          (six-times ten) => thirty
          (provided
            (twice ten) =throws=> (Exception. "whatever"))))

  (fact "`fact` code re-write with metaconstants AND exception mock AND exception test"
        (six-times .whatever.) => (throws Exception "whatever") ;; we coud have `(Exception. "whatever")`
        (provided
          (six-times .whatever.) =throws=> (Exception. "whatever"))) ;;exceptions in mocks can ONLY be instance objects

  (fact "`fact` code re-write with `contains` assertion + nested `contains` checker"
        (six-times {:z {:zz :tt :yy :pp} :aa :bb}) => (contains {:a 1})
        (provided
          (six-times (contains {:z (contains {:zz :tt})})) => {:a 1 :b 2}))

  (fact "`fact` code re-write with `just` checker assertion + arg custom checker within a `contains`"
        (six-times {:n 10}) => (just {:a 1})
        (provided
          (six-times (contains {:n (checker [x] (pos? x))})) => {:a 1}))

  (fact "`fact` code re-write with `just` checker assertion + arg custom checker with fn experssion"
        (six-times 10) => (just {:a (checker [x] (pos? x))})
        (provided
          (six-times (checker (fn [x] (pos? x)))) => {:a 1})) ;;this looks slightly different than midje `checker` so we have to change it manually

  (fact "`fact` code re-write with  `checker` assertion + arg custom checker with compiled fn"
        (six-times 10) => (checker [x] (contains? x :a))
        (provided
          (six-times 10) => {:a 1}))

  (fact "`fact` code re-write with  `every-checker` assertion"
        (six-times 10) =>  (every-checker (contains {:a 1}) (checker [x] (contains? x :a))) ;;both checkers test for almost the same thing
        (provided
          (six-times 10) => {:a 1}))

  (let [a {:z :zz}
        b {:a 1}]
    (fact "`contains` resolves symbols"
          (six-times {:z :zz}) => (contains b)
          (provided
            (six-times (contains a)) => {:a 1 :b 2})))

  (fact "`fact` code re-write with `n-of` assertion + nested `contains` checker"
        (six-times {:z {:zz :tt :yy :pp} :aa :bb}) => (two-of map?)
        (provided
          (six-times (contains {:z (contains {:zz :tt})})) => [{:a 1 :b 2} {}]))


  (fact "`fact` code re-write with `has` assertion"
        (six-times {:z {:zz 'hi :yy :pp} :aa :bb}) => (has some set?)
        (provided
          (six-times (contains {:z (contains {:zz anything})})) => [{:a 1 :b 2} #{}]))

  (fact "`fact` code re-write with `truthy` assertion"
        (six-times {:z {:zz 'hi :yy :pp} :aa :bb}) => truthy
        (provided
          (six-times (contains {:z (contains {:zz anything})})) => [{:a 1 :b 2} []]))

  (fact "`fact` code re-write with `falsey` assertion"
        (six-times {:z {:zz 'hi :yy :pp} :aa :bb}) => falsey
        (provided
          (six-times (contains {:z (contains {:zz anything})})) => nil))

  (tabular  ;; simple tabular fact works
    (fact "adding/multiplying with self is the same for '0' and '2'" (+ ?x ?x) => (* ?x ?x))
    ?x
    0
    2)

         ;;following test was copied from  novate.test.random-test
  (tabular ;; complex tabular works
    (fact "wrand returns correct index"
      (wrand ?weights) => ?index
        (provided
          (rand ?sum) => ?rand))
    ?weights      ?sum ?rand ?index
    [3 6 7 12 15] 43   1     0
    [3 6 7 12 15] 43   3     1
    [3 6 7 12 15] 43   9     2
    [3 6 7 12 15] 43   16    3
    [3 6 7 12 15] 43   28    4
    [3 6 7 12 15] 43   42    4)


;;the following code  throws a compilation error (about NOT supporting function meta-constants)

 #_(fact "`fact` code re-write with metaconstants AND exception mock AND exception test"
         (six-times -whatever-) =throws=> Exception ;; we coud have `(Exception. "whatever")`
         (provided
           (six-times -whatever-) => (Exception. "whatever")))

 )
