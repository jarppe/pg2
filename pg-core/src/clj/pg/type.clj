(ns pg.type
  (:refer-clojure :exclude [vector])
  (:import
   java.util.Map
   java.io.Writer
   org.pg.type.SparseVector
   org.pg.error.PGError
   org.pg.type.processor.IProcessor
   org.pg.type.processor.Processors))

(def ^IProcessor vector Processors/vector)

(def ^IProcessor sparsevec Processors/sparsevec)

(defmethod print-method SparseVector
  [^SparseVector sv ^Writer w]
  (.write w (format "<SparseVector %s>" sv)))

(defn ->sparse-vector
  ^SparseVector [^Integer dim ^Map index]
  (let [-index
        (reduce-kv
         (fn [acc k v]
           (assoc acc (int k) (float v)))
         {}
         index)]
    (new SparseVector dim -index)))
