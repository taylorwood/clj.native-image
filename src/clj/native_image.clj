(ns clj.native-image
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as cs])
  (:import (java.io File)))

(defn classpath
  "Returns the classpath string minus clj.native-image path and plus *compile-path*."
  []
  (as-> (System/getProperty "java.class.path") $
    (cs/split $ (re-pattern (str File/pathSeparatorChar)))
    (remove #(cs/includes? "clj.native-image" %) $) ;; exclude ourselves
    (cons *compile-path* $) ;; prepend compile path for classes
    (cs/join File/pathSeparatorChar $)))

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
    ;; TODO would be nice to stream/"redirect" output here
    (some-> err not-empty println)
    (some-> out not-empty println)
    exit))

(defn prep-compile-path []
  (doseq [file (-> (io/file *compile-path*) (file-seq) (rest) (reverse))]
    (io/delete-file file))
  (.mkdir (io/file *compile-path*)))

(defn -main [main & [nat-img-path & nat-img-opts]]
  (when-not (string? main)
    (binding [*out* *err*] (println "Main namespace required e.g. \"script\" if main file is ./script.clj"))
    (System/exit 1))

  (println "Loading" main)
  (load main)

  (println "Compiling" main)
  (prep-compile-path)
  (compile (symbol main))

  (System/exit
   (build-native-image
    (classpath)
    main
    (or nat-img-path
        ;; else try to resolve from env var
        (-> (System/getenv "GRAALVM_HOME")
            (io/file "bin/native-image")
            (.getAbsolutePath)))
    nat-img-opts)))
