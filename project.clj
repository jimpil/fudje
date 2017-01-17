(defproject fudje "0.9.7"
  :description "A small unit-testing library heavily inspired by midje."
  :url "https://github.com/jimpil/fudje"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/math.combinatorics "0.1.1"  :exclusions [org.clojure]]] ;; avoid  pulling  clj 1.4

  :source-paths ["src/clojure"]
  :test-paths ["test/clojure"]
  :java-source-paths ["src/java"]
  :javac-options     ["-target" "1.6" "-source" "1.6"]
  )
