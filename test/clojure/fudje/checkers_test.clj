(ns fudje.checkers-test
  (:require [clojure.test :refer :all]
            [fudje.sweet :refer :all]
            [fudje.data :refer [diff]]))


(deftest checker-tests

  (is (compatible (contains {:a 1}) {:a 1 :b 2})
      "Value contains the same keys & values as expected and an additional key")

  (is (= [{:a 1} nil nil] (diff (contains {:a 1}) {:b 2}))
      "Value is missing a key in expected.")

  (is (= [{:a 1} {:a 2} nil] (diff (contains {:a 1}) {:a 2}))
      "Value has different value for a key in expected.")

  (is (compatible (contains {:a (contains {:b 2})}) {:a {:b 2 :z 1} :x 1})
      "Works recursively")

  (is (= [{:a {:z 2}} {:a {:z 1}} {:a {:b 2}}]
         (diff (contains {:a (contains {:b 2 :z 2})}) {:a {:b 2 :z 1} :x 1}))
      "A difference in sub-map")

  (is (compatible (contains #{1 2}) #{1 2 3}))

  (is (compatible [{:a 1} {:b 1}]
                  [{:a 1} {:b 1}])
      "Sequential collections can contain maps")
  (is (compatible [(contains {:a 1}) (contains {:b 1})]
                  [{:a 1 :z 1} {:b 1 :z 1}])
      "Sequential collections can contain maps with rules")

  (is (compatible (contains [3 1 2] :in-any-order)
                  [1 2 3])
      "Sequential collections can be marked with in-any-order")

  (is (= [(contains [3 1 2 4] :in-any-order) [1 2 3] nil] (diff (contains [3 1 2 4] :in-any-order) [1 2 3]))
      "if In-any-order fails then diffing falls back to vanilla sequential diffing")

  (is (compatible (contains [1 2 3])
                  [4 1 2 3 6])
      "contains works with sequential things much like midje")

  (is (compatible (contains [4 5 700] :in-any-order) [:ignored 700 5 4 :ignored])

      "contains supports :in-any-order together much like midje")
  (is (compatible (contains [700 4 5] :gaps-ok) [700 'hi 4 5 'hi])
      "contains supports :gaps-ok much like midje")

  (is (compatible (contains [4 5 700] :in-any-order :gaps-ok) [700 'hi 4 5 'hi])
      "contains supports gaps-ok + :in-any-order together much like midje")

  (is (compatible (contains {:a (contains {:z (contains [3 6 1] :in-any-order)})})
                  {:b 999 :a {:h 444 :z [1 3 6]}})
      "`contains works` recursively")

  (is (compatible (just {:a 1}) {:a 1})
      "useless but works - just like midje")

  (is (compatible (just [3 1 2] :in-any-order) [1 2 3])
      "`just`  can be marked with :in-any-order")

  (is (compatible (just {:a (checker pos?)}) {:a 1}) ;;custom checkers require the target shape
      "just supports inner checkers (as long as we're diffing maps)")

  (is (= [(just [3 1 2] :in-any-order) #{4} #{1 3 2}] (diff (just [3 1 2] :in-any-order) [1 2 3 4]))
      "just complains on extra elements")

  (is (= [(just {:a (n-of number? 2)}) {:b 2} {:a #{10 11}}] (diff (just {:a (n-of number? 2)}) {:a #{10 11} :b 2}))
      "just complains on extra elements #2")


  (is (= [(just {:a (checker vector?)}) {:b 2} {:a [1]}] (diff (just {:a (checker vector?)}) {:a [1] :b 2}))
      "just complains on extra elements #3")


  (is (compatible (just {:a (contains [1 2 3] :gaps-ok)})
                  {:a [:whatever 1 555 2 666 3 777]}))

  (is (compatible [{:a 1} {}]
                  (n-of map? 2))
      "top-level `n-of` works")

  (is (compatible (has some vector?)
                  [{} []])
      "top-level `has` works with sequentials")

  (is (compatible (has some (comp zero? second)) ;; have to use `second` instead of `:a` as the map will be seq-ed (per `some`)
                  {:a 0 :b 2})
      "top-level `has` implicitely seqs maps")

  (is (compatible (checker (comp zero? :a)) ;; semantically equivalent with above but using `:a` rather than `second`
                  {:a 0 :b 2})
      "...but we can also write it like this")

  (is (compatible (contains {:a (has some neg?)})
                  {:a [10 -11] :b 2})
      "`has` nested checker works")

  (is (compatible (n-of (partial = \R) 3) "RRR"))
  (is (compatible (has some  (partial = \R)) "VBR"))
  (is (compatible (contains {:a (n-of (partial = \R) 3)}) {:a "RRR"}))
  (is (compatible (contains "RR") "RRR")
      "contains works with string against string")
  (is (compatible (contains "R*") "RRR")
      "contains works with regex-string against string")
  (is (compatible (contains "RRR") ["VVV" "RRR"])
      "contains works with string against coll")
  (is (compatible (contains \R) "RRR")
      "contains works with char against string")
  (is (compatible (contains \R) [:a :b \R])
      "contains works with char against coll")
  (let [x 3.5 t 1]
    (is (compatible (roughly x t) 3.8)
      "roughly checker works"))





                  )
