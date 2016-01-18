(ns fudje.checkers
  (:require [fudje
             [util :as ut]
             [data :as data]]
            [clojure.set :as set])
  (:import [java.io Writer]
           [fudje IChecker]))

(defonce shapes #{:atom :sequential :map :set})

;==============<POLYMORPHIC CHECKERS THAT INTERFACE DIRECTLY WITH `fudje.data`>========================================

(deftype ContainsChecker [content opts]
  data/Diff
  (diff-similar [this other]
    (case (data/equality-partition content)
      ; If "expected" is marked contains, the other value only needs to contain keys also in expected
      :map (data/diff-similar content (select-keys other (keys content)))
      ; Select items in value which are also in expected
      :set (data/diff-similar content (set/select content (cond-> other
                                                                  (not (set? other)) set)))
      :sequential (or (ut/diff-sequential* content other opts)
                      [this other nil])
      :atom (if (string? content)  ;; let's make the extra effort to support :atoms in `contains` and ease the situation with Strings
              (if (string? other)
                (if (re-find (re-pattern content) other)
                  [nil nil other]
                  [this other nil])
                (data/diff-similar (cond->> content
                                       (not (set? content)) (conj #{})) other))
              (data/diff-similar (cond->> content
                                     (not (set? content)) (conj #{})) other))
      ))
  data/EqualityPartition
  (equality-partition [_]
    shapes)

  Object
  (equals [this o]
    (or (identical? this o)
        (and (instance? ContainsChecker o)
             (= content (.content ^ContainsChecker o))
             (= opts (.opts ^ContainsChecker o)))))

  IChecker
  )

(defmethod print-method ContainsChecker [^ContainsChecker v ^Writer w]
  (.write w "(contains ")
  (.write w (pr-str (.content v)))
  (.write w ^String (apply str (interleave (repeat \space) (-> v .opts keys))))
  (.write w ")")
  )


(deftype JustChecker [content opts]
  data/Diff
  (diff-similar [this other]
    (if (= :sequential (data/equality-partition this))
      (let [ignore-order? (:in-any-order opts)] ;; gaps-ok has no effect here
        (if ignore-order?
          (let [[a b both :as diff] (data/diff-similar (set content) (set other))]
            (if (empty? b)
              diff
              [this b both]))
          (data/diff-similar content other)))
      (let [[a b both :as diff] (data/diff-similar content other)] ;;diff normally for maps and sets
        (if (empty? b)
          diff
          [this b both]))))
  data/EqualityPartition
  (equality-partition [_]
    (data/equality-partition content))

  Object
  (equals [this o]
    (or (identical? this o)
        (and (instance? JustChecker o)
             (= content (.content ^JustChecker o))
             (= opts (.opts ^JustChecker o)))))

  IChecker
  )

(defmethod print-method JustChecker [^JustChecker v ^Writer w]
  (.write w "(just ")
  (.write w (pr-str (.content v)))
  (.write w ^String (apply str (interleave (repeat \space) (-> v .opts keys))))
  (.write w ")")
  )

(deftype CustomChecker [testfn]
  data/Diff
  (diff-similar [this other ]
    (if (testfn other)
      [nil nil other]
      [this other nil]))
  data/EqualityPartition
  (equality-partition [_]
    shapes)

  Object
  (equals [this o]
    (or (identical? this o)
        (and (instance? CustomChecker o)
             (identical? testfn (.testfn ^CustomChecker o)))))

  IChecker
  )

(defmethod print-method CustomChecker [^CustomChecker v ^Writer w]
  (.write w (str "(checker " (.testfn v) ")")))

(deftype NofChecker [f n]
  data/Diff
  (diff-similar [this other]
    (let [c-other (count other)]
      (if (and (every? f other)
               (= n c-other))
        [nil nil other] ;; no diff
        [this other nil])))
  data/EqualityPartition
  (equality-partition [_]
    shapes)

  Object
  (equals [this o]
    (or (identical? this o)
        (and (instance? NofChecker o)
             (== n (.n ^NofChecker o))
             (identical? f (.f ^NofChecker o)))))

  IChecker
  )

(defmethod print-method NofChecker [^NofChecker v ^Writer w]
  (.write w (str "(n-of " (.f v) " " (.n v) ")")))

(deftype HasChecker [qf f]
  data/Diff
  (diff-similar [this other]
    (if (qf f other)
      [nil nil other] ;; no diff
      [this other nil]))
  data/EqualityPartition
  (equality-partition [_]
    shapes)

  Object
  (equals [this o]
    (or (identical? this o)
        (and (instance? HasChecker o)
             (identical? qf (.qf ^HasChecker o))
             (identical? f (.f ^HasChecker o)))))

  IChecker
  )

(defmethod print-method HasChecker [^HasChecker v ^Writer w]
  (.write w (str "(has " (.qf v) " " (.f v) ")")))


(deftype AnythingChecker []
  data/Diff
  (diff-similar [this other]
    [nil nil other])
  data/EqualityPartition
  (equality-partition [_]
    shapes)

  Object
  (equals [this o]
    (instance? AnythingChecker o))

  IChecker
  )

(defmethod print-method AnythingChecker [_ ^Writer w]
  (.write w "anything"))

(defn close? [tolerance a b]
  (< (Math/abs (double (- a b))) tolerance))

(deftype RoughlyChecker [n tolerance]
  data/Diff
  (diff-similar [this other]
    (let [tolerance (or tolerance 0.001)]
      (assert (number? other) "`roughly` can ONLY be used against numbers!")
      (assert (number? tolerance) "<tolerance> MUST be a number!")
      (if (close? tolerance n other)
        [nil nil other]
        [this other nil])))
  data/EqualityPartition
  (equality-partition [_]
    :atom)

  Object
  (equals [this o]
    (or (identical? this o)
        (and (instance? RoughlyChecker o)
             (== n (.n ^RoughlyChecker o))
             (== tolerance (.tolerance ^RoughlyChecker o)))))

  IChecker
  )

(defmethod print-method RoughlyChecker [^RoughlyChecker v ^Writer w]
  (.write w (str "(roughly " (.n v) " " (or (.tolerance v) 0.001) ")")))


(deftype EveryChecker [checkers]
  data/Diff
  (diff-similar [_ other]
    (let [failed-diff (some (fn [checker]
                              (let [[a b both :as diff] (data/diff-similar checker other)]
                                (when (seq a)
                                  diff)))
                            checkers)]
      (or failed-diff [nil nil other])))
  data/EqualityPartition
  (equality-partition [_]
    shapes)

  Object
  (equals [this o]
    (or (identical? this o)
        (and (instance? EveryChecker o)
             (every? true? (map = checkers (.checkers ^EveryChecker o))))))

  IChecker
  )

(defmethod print-method EveryChecker [^EveryChecker v ^Writer w]
  (.write w "(every-checker ")
  (.write w ^String (apply str (interpose " " (.checkers v))))
  (.write w ")")
  )

(deftype TruthyChecker []
  data/Diff
  (diff-similar [this other]
    (if (boolean other)
      [nil nil other]
      [this other nil])
    )
  data/EqualityPartition
  (equality-partition [_]
    shapes)

  Object
  (equals [this o]
    (or (identical? this o)
        (instance? TruthyChecker o)))

  IChecker
  )

(defmethod print-method TruthyChecker [^TruthyChecker v ^Writer w]
  (.write w "truthy"))

(deftype FalseyChecker []
  data/Diff
  (diff-similar [this other]
    (if (not (boolean other))
      [nil nil other]
      [this other nil])
    )
  data/EqualityPartition
  (equality-partition [_]
    shapes)

  Object
  (equals [this o]
    (or (identical? this o)
        (instance? FalseyChecker o)))

  IChecker
  )

(defmethod print-method FalseyChecker [^FalseyChecker v ^Writer w]
  (.write w "falsey"))

