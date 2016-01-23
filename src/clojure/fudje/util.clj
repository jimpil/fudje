(ns fudje.util
  (:require [fudje.data :as data]
            [clojure.math.combinatorics :as combi])
  (:import [fudje IChecker]))

(defn throwable? [x]
  (instance? Throwable x))

(defn poly-checker? [x]
  (instance? IChecker x))

(defn metaconstant? [s]
  (let [ss (str s)]
    (cond
      (and (.startsWith ss ".")
           (.endsWith ss ".")) [true "\\."] ;;for function meta-constants we'd use [true "\\-"]
      (and (.startsWith ss "-")
           (.endsWith ss "-")) [false (IllegalStateException. "Converting function-metaconstants is NOT supported as these tests will have to be re-written from scratch anyway!")]
      :else
      [false nil])))

(defn metaconstant->kw
  "Converts a midje metaconstant to a keyword.
   Doesn't work for function-metaconstants."
  [mtc ^String sep]
  (keyword (str "..." (.replaceAll (str mtc) sep "") "...")))


(let [fix "?"] ;; predefined prefix/suffix
  (defn qmark?
    "Returns true if the symbol <x> starts/ends with '?'."
    [x]
    (let [sx (str x)]
      (or (.startsWith sx fix)
          (.endsWith sx fix))))
  )

(defn find-subset
  "Given the already constructed partitions of <other> (via `(partition (count content) 1 other)`),
  walk their diffs against <seq-content>, looking for a nil first item (i.e. a successful diff).
  If one is found, return that partition as the solution."
  [seq-other-partitions seq-content]
  (some (fn [p]
          (when (->> p
                     (data/diff seq-content)
                     first
                     nil?) ;; walk their diffs looking for an empty first element (succesful diff)
            p))
        seq-other-partitions))


(defn find-subset-without-gaps
  "Given 2 sequential things, find the biggest matching subset of the first into the second,
  while ignoring intermediate elements."
  [seq-content seq-other]
  (loop [content seq-content
         potentials seq-other
         solution []]
      (if (or (empty? potentials)
              (empty? content))
        solution
        (let [fcontent (first content)
              matches? (= fcontent
                          (first potentials))]
          (if matches?
            (recur (rest content) (rest potentials) (conj solution fcontent))
            (recur content (rest potentials) solution))))))

(defn find-subset-in-any-order [other-partitions content]
  (->> content
       combi/permutations
       (keep (partial find-subset other-partitions)) ;; this is hard! try out all possible permutations of <content> against all the all possible partitions of <other> - this code branch can be MUCH slower
       first))

(defn diff-sequential*
  "Helper for uncluttering the ContainsChecker code.
   It does a sensible attempt to diff sequential things a bit more relaxed than what clojure.data/diff does by default,
   so we can support certain useful modifiers like :in-any-order & :gaps-ok."
  [content other opts]
  (let [other-partitions (delay (partition (count content) 1 other)) ;; wrap it in `delay` - we may not have to use it after all
        [ignore-order? ignore-gaps?] ((juxt :in-any-order :gaps-ok) opts)]
    (if ignore-order?
      (if ignore-gaps?
        (data/diff-similar (set content) (set other)) ;; this is easy - just use sets
        (when (find-subset-in-any-order @other-partitions content)
          [nil nil content]))
      (if ignore-gaps?
        (when (= content (find-subset-without-gaps content other)) ;;this is easy as well
          [nil nil content])
        (when (find-subset @other-partitions content) ;; this is hard! try out all possible partitions of <other> - this code branch will be SLIGHTLY slower
          [nil nil content])
        ))))


