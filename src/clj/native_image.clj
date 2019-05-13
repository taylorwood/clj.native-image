(ns clj.native-image
  "Builds GraalVM native images from deps.edn projects."
  (:require [clojure.java.io :as io]
            [clojure.string :as cs]
            [clojure.tools.deps.alpha :as deps]
            [clojure.tools.deps.alpha.reader :as deps.reader]
            [clojure.tools.namespace.find :refer [find-namespaces-in-dir]])
  (:import (java.io BufferedReader)))

(def windows?
  (-> (System/getProperty "os.name")
      cs/trim
      cs/lower-case
      (cs/starts-with? "windows")))

(defn deps->classpath
  "Returns the classpath according to deps.edn, adds *compile-path*."
  [deps-map]
  (deps/make-classpath
    (deps/resolve-deps deps-map nil)
    (:paths deps-map)
    {:extra-paths (conj (:extra-paths deps-map) *compile-path*)}))

(defn merged-deps []
  "Merges install, user, local deps.edn maps left-to-right."
  (-> (if windows? {:config-files []}
          (deps.reader/clojure-env))
      (:config-files)
      (concat ["deps.edn"])
      (deps.reader/read-deps)))

(defn sh
  "Launches a process with optional args, returning exit code.
  Prints stdout & stderr."
  [bin & args]
  (let [arg-array ^"[Ljava.lang.String;" (into-array String (cons bin args))
        process (-> (ProcessBuilder. arg-array)
                    (.redirectErrorStream true) ;; TODO stream stderr to stderr
                    (.start))]
    (with-open [out (io/reader (.getInputStream process))]
      (loop []
        (when-let [line (.readLine ^BufferedReader out)]
          (println line)
          (recur))))
    (.waitFor process)))

(defn exec-native-image
  "Executes native-image (bin) with opts, specifying a classpath,
   main/entrypoint class, and destination path."
  [bin opts cp main]
  (let [cli-args (cond-> []
                   (seq opts) (into opts)
                   cp         (into ["-cp" cp])
                   main       (conj main)
                   (not windows?) (conj "--no-server"))]
    (apply sh bin cli-args)))

(defn prep-compile-path []
  (let [compile-path (io/file *compile-path*)]
    (doseq [file (-> compile-path (file-seq) (rest) (reverse))]
      (io/delete-file file))
    (.mkdir compile-path)))

(defn native-image-bin-path []
  (-> (io/file (System/getenv "GRAALVM_HOME")
               (if windows?
                 "bin/native-image.cmd"
                 "bin/native-image"))
      (.getAbsolutePath)))

(defn- munge-class-name [class-name]
  (cs/replace class-name "-" "_"))

(defn build [main-ns opts]
  (let [[nat-img-path & nat-img-opts]
        (if (some-> (first opts) (io/file) (.exists)) ;; check first arg is file path
          opts
          (cons (native-image-bin-path) opts))]
    (when-not (string? main-ns)
      (binding [*out* *err*] (println "Main namespace required e.g. \"script\" if main file is ./script.clj"))
      (System/exit 1))

    (let [deps-map (merged-deps)
          namespaces (mapcat (comp find-namespaces-in-dir io/file) (:paths deps-map))]
      (prep-compile-path)
      (doseq [ns namespaces]
        (println "Compiling" ns)
        (compile (symbol ns)))

      (let [classpath (deps->classpath deps-map)
            class-name (munge-class-name main-ns)]
        (println (format "Building native image with classpath '%s'" classpath))
        (System/exit (exec-native-image nat-img-path nat-img-opts classpath class-name))))))

(defn -main [main-ns & args]
  (try
    (build main-ns args)
    (finally
      (shutdown-agents))))
