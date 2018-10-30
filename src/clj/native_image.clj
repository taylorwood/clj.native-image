(ns clj.native-image
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as cs]
            [clojure.tools.deps.alpha :as deps]
            [clojure.tools.deps.alpha.reader :as deps.reader])
  (:import (java.io File)))

(defn deps->classpath
  "Returns the classpath according to deps.edn, adds *compile-path*."
  [deps-map]
  (let [lib-map (deps/resolve-deps deps-map nil)]
    (deps/make-classpath lib-map (:paths deps-map) {:extra-paths [*compile-path*]})))

(defn merged-deps []
  "Merges install, user, local deps.edn maps left-to-right."
  (-> (deps.reader/clojure-env)
      (:config-files)
      (concat ["deps.edn"])
      (deps.reader/read-deps)))

(defn exec-native-image
  "Executes native-image (bin) with opts, specifying a classpath,
   main/entrypoint class, and destination path."
  [bin opts cp main]
  (let [all-args (cond-> []
                   (seq opts) (into opts)
                   cp (into ["-cp" cp])
                   main (conj main))]
    (apply sh bin all-args)))

(defn build-native-image [cp main nat-img-path opts]
  (println (format "Building native image '%s' with classpath '%s'" main cp))
  (let [{:keys [exit out err]}
        (exec-native-image nat-img-path (conj opts "--no-server") cp main)]
    ;; TODO would be nice to stream output here
    (some-> err not-empty println)
    (some-> out not-empty println)
    exit))

(defn prep-compile-path []
  (doseq [file (-> (io/file *compile-path*) (file-seq) (rest) (reverse))]
    (io/delete-file file))
  (.mkdir (io/file *compile-path*)))

(defn native-image-path []
  (-> (io/file (System/getenv "GRAALVM_HOME") "bin/native-image")
      (.getAbsolutePath)))

(defn- munge-class-name [class-name]
  (cs/replace class-name "-" "_"))

(defn -main [main & opts]
  (let [[nat-img-path & nat-img-opts]
        (if (some-> (first opts) (io/file) (.exists)) ;; check first arg is file path
          opts
          (cons (native-image-path) opts))]
    (when-not (string? main)
      (binding [*out* *err*] (println "Main namespace required e.g. \"script\" if main file is ./script.clj"))
      (System/exit 1))

    (println "Loading" main)
    (load (-> main
              (cs/replace "." File/separator)
              (munge-class-name)))

    (println "Compiling" main)
    (prep-compile-path)
    (compile (symbol main))

    (System/exit
      (build-native-image
       (deps->classpath (merged-deps))
       (munge-class-name main)
       nat-img-path
       nat-img-opts))))
