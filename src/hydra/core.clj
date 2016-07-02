(ns hydra.core
  (:require [clojure.set :refer [union]]
            [clojure.string :refer [split]]))

(defn to-path-set
  ([coll] (to-path-set "" coll))
  ([path coll]
   (cond (map? coll)    (reduce union #{} (for [[k v] coll] (to-path-set (str path "/" k) v)))
         (vector? coll) (reduce union #{} (for [i (range (count coll))] (to-path-set (str path "/" i) (nth coll i))))
         :else #{(str path "/" coll)})))

(defn str->int [s]
  (Integer. s))

(defn numeric? [s]
  (re-matches #"\d+" s))

(defn every-key-numeric? [m]
  (every? numeric? (keys m)))

(defn add-paths [mp [hd & tail :as path]]
  (if (= 2 (count path))
    (assoc mp hd (let [val (second path)] (if (numeric? val) (str->int val) val)))
    (assoc mp hd (add-paths (get mp hd {}) tail))))

(defn from-path-set-to-map-of-maps [ps]
  (let [paths (mapv #(-> % (split #"/") rest) ps)]
    (reduce add-paths {} paths)))

(defn vectorize [m-of-m]
  (let [sub-maps (into {} (for [[k v] m-of-m] [k (if (map? v) (vectorize v) v)]))]
    (if (every-key-numeric? sub-maps)
      (map second (sort-by first (into [] sub-maps)))
      sub-maps)))

(defn from-path-set [ps]
  (-> ps from-path-set-to-map-of-maps vectorize))