(ns hydra.core
  (:require [clojure.set :refer [union]]))

(defn to-path-set
  ([coll] (to-path-set "" coll))
  ([path coll]
   (cond (map? coll)    (reduce union #{} (for [[k v] coll] (to-path-set (str path "/" k) v)))
         (vector? coll) (reduce union #{} (for [i (range (count coll))] (to-path-set (str path "/" i) (nth coll i))))
         :else #{(str path "/" coll)})))