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

(defn qmark?
  "Returns true if the symbol <x> starts with '?'."
  [x]
  (.startsWith (str x) "?"))

(defn find-subset
  [seq-other-partitions seq-content]
  (->> seq-other-partitions
       (map (partial data/diff seq-content)) ;; diff them all
       (some #(when (-> % first empty?) %)))) ;; walk their diffs looking for an empty first element (succesful diff)


(defn find-subset-without-gaps
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
          (recur
            (cond-> content matches? rest)
            (rest potentials)
            (cond-> solution matches? (conj fcontent)))))))

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
        (->> content
             combi/permutations
             (keep (partial find-subset @other-partitions)) ;; this is hard! try out all possible permutations of <content> against all the all possible partitions of <other> - this code branch can be MUCH slower
             first))
      (if ignore-gaps?
        (when (= content (find-subset-without-gaps content other)) ;;this is easy as well
          [nil nil content])
        (find-subset @other-partitions content) ;; this is hard! try out all possible partitions of <other> - this code branch will be SLIGHTLY slower
        ))))


