(ns fudje.runner
  (:require [clojure.test :as ctest]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [clojure.tools.namespace.dir :as tns-dir]
            [clojure.tools.namespace.track :as tns-track])
  (:gen-class))

(def default-focus-meta :focus)
(def default-skip-meta  :skip)
(def fail-fast-types  #{:error :fail})
(def has-test-meta?   (comp :test meta))
(defn comma-split   [s] (str/split s #","))
(defn absolute-file [s] (-> s io/file .getAbsoluteFile))

(defn find-focus-nss
  [focus-meta-kw nss-syms]
  (filter
    #(-> % find-ns meta focus-meta-kw)
    nss-syms))

(defn find-skip-nss
  [skip-meta-kw nss-syms]
  (filter
    #(-> % find-ns meta skip-meta-kw)
    nss-syms))

(defn- focused-vars
  [focus-meta]
  (->> (all-ns)
       (mapcat (comp vals ns-publics))
       (filter (comp focus-meta meta))))

(defn- strip-ns!
  [ns-sym {:keys [focus-meta skip-meta]
           :or {focus-meta default-focus-meta
                skip-meta  default-skip-meta}}]
  ;(require ns-sym :reload)
  (let [publics    (ns-publics ns-sym)
        test-vars  (filter has-test-meta? (vals publics))
        focus-vars (filter (comp focus-meta meta) test-vars)
        skip-vars  (if (seq focus-vars)
                     (remove (set focus-vars) test-vars)
                     (filter (comp skip-meta meta) test-vars))]
    (doseq [sv skip-vars]
      (println "Skipping Var:" sv)
      (->> sv
           meta
           :name
           symbol
           (ns-unmap ns-sym)))))

(defn- run-tests
  "Essentially a wrapper around `clojure.test/run-tests`,
   which respects all the supported options."
  [{:keys [paths fail-fast ns-focus ns-skip focus-meta skip-meta]
    :or {focus-meta default-focus-meta
         skip-meta  default-skip-meta}
    :as opts}]
  (let [roots  (map absolute-file paths)
        ;; Scan the directories and build a dependency tracking map
        tracker (-> (tns-track/tracker)
                    (tns-dir/scan-dirs roots))
        ;; Extract the namespaces in correct topological order
        all-nss  (get tracker ::tns-track/load)
        _ (doseq [n all-nss] (require n :reload))
        ns-focus (or ns-focus
                     (seq (find-focus-nss focus-meta all-nss))
                     (map #(-> % meta :ns ns-name symbol)
                          (focused-vars focus-meta)))
        ns-skip  (when (empty? ns-focus)
                   (or ns-skip (find-skip-nss skip-meta all-nss)))
        nss  (cond->> all-nss
                      (seq ns-focus)
                      (keep (let [ns-focus (set ns-focus)]
                              #(or (ns-focus %)
                                   (println "Skipping Namespace:" %))))

                      (seq ns-skip)
                      (keep (let [ns-skip (set ns-skip)]
                              #(if (not (contains? ns-skip %))
                                 %
                                 (println "Skipping Namespace:" %)))))]
    (doseq [n nss] (strip-ns! n opts))
    (if fail-fast
      (let [orig-report clojure.test/report]
        (binding [ctest/report (fn [{:keys [type] :as rep}]
                                 (orig-report rep)
                                 (when (contains? fail-fast-types type)
                                   (System/exit -1)))]
          (apply ctest/run-tests nss)))
      (apply ctest/run-tests nss))))

(def cli-options
  [[nil "--paths PATHS" "Directories to scan (comma-separated)"
    :default ["test"]
    :parse-fn comma-split]

   [nil "--ns-focus NAMESPACES" "Namespaces to focus on (comma-separated)"
    :parse-fn (comp (partial map symbol) comma-split)]

   [nil "--focus-meta FOCUS_META" "The metadata to look for when focusing (applicable per namespace)"
    :default default-focus-meta
    :parse-fn keyword]

   [nil "--skip-meta SKIP_META" "The metadata to look for when skipping"
    :default default-skip-meta
    :parse-fn keyword]

   ;; boolean options (defaulting to nil)
   [nil "--fail-fast"]
   ["-h" "--help"]])

(defn -main [& args]
  (let [{:keys [options errors summary]} (cli/parse-opts args cli-options)]
    (cond
      errors
      (do
        (run! println errors)
        (println summary))

      (:help options)
      (println summary)

      :else
      (let [{:keys [fail error]} (time (run-tests options))]
        (when (or (pos? fail)
                  (pos? error))
          (System/exit -1))))))