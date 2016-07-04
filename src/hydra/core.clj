(ns hydra.core
  (:require [clojure.set :refer [union]]
            [clojure.string :refer [split]]))

(defn to-path-set
  ([coll] (to-path-set '[] coll))
  ([path coll]
   (cond (map? coll) (reduce union #{} (for [[k v] coll] (to-path-set (conj path k) v)))
         (vector? coll) (reduce union #{} (for [i (range (count coll))] (to-path-set (conj path i) (nth coll i))))
         :else #{(conj path coll)})))

(defn- add-paths [mp [hd & tail :as path]]
  (if (= 2 (count path))
    (assoc mp hd (second path))
    (assoc mp hd (add-paths (get mp hd {}) tail))))

(defn- from-path-set-to-map-of-maps [path-set]
  (reduce add-paths {} path-set))

(defn- vectorize [m-of-m]
  (let [sub-maps (into {} (for [[k v] m-of-m] [k (if (map? v) (vectorize v) v)]))]
    (if (->> sub-maps keys (every? number?))
      (mapv second (sort-by first (into [] sub-maps)))
      sub-maps)))

(defn from-path-set [ps]
  (-> ps from-path-set-to-map-of-maps vectorize))

(defn cleave [pred ps]
  (loop [results '(() ()) paths (seq ps)]
    (if (not paths)
      (list (set (first results)) (set (second results)))
      (let [p (first paths)
            passes (first results)
            fails (second results)]
        (recur (if (pred p) (list (conj passes p) fails) (list passes (conj fails p))) (next paths))))))

(defn splice [path-sets]
  (reduce union path-sets))

(defn starts-with? [route path]
  (every? identity (for [i (range (count route))] (= (nth route i) (nth path i)))))

(defn change-leaf [p val]
  (conj (vec (butlast p)) val))

(defn reset-leaf [ps route val]
  (let [[passed failed] (cleave #(starts-with? route %) ps)]
    (splice [(set (mapv #(change-leaf % val) passed)) failed])))

(def reset-leaves reset-leaf) ;; Synonym

