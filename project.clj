(defproject fudje "0.7.9-SNAPSHOT"
  :description "A unit-testing library which provides features very similar to midje for a fraction of the code.
Fudje was designed to be significantly less 'magical' than midje, and to be AOT friendly (as opposed to midje)."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/math.combinatorics "0.1.1"  :exclusions [org.clojure]]] ;; avoid  pulling  clj 1.4
  :aot :all)
