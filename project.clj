(defproject fudje "0.8.2-SNAPSHOT"
  :description "A small unit-testing library heavily inspired by midje."
  :url "https://github.com/jimpil/fudje"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/math.combinatorics "0.1.1"  :exclusions [org.clojure]]] ;; avoid  pulling  clj 1.4

  :source-paths ["src/clojure" "test/clojure"]
  :java-source-paths ["src/java"]
  )
