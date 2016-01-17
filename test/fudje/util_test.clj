(ns fudje.util-test
  (:require [clojure.test :refer :all]
            [fudje.util :refer :all]))


(deftest find-subset-without-gaps-tests
  (is (= [1 2 3] (find-subset-without-gaps [1 2 3] [1 'hi 2 :hi 3 nil])))
  (is (= [1 2] (find-subset-without-gaps [1 2 3] [3 1 'hi 2 :hi])))
  (is (= [1] (find-subset-without-gaps [1 2 3] [3 2 'hi  1 :hi])))
  )

(deftest find-subset-tests
  (is (= [nil nil [:a :b :c]] (find-subset (partition 3 1 [1 2 :a :b :c 3 4 5 6]) [:a :b :c])))
  (is (= nil (find-subset (partition 3 1 [1 2 :a :b  3 :c 4 5 6]) [:a :b :c])))
  )
