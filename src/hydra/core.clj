(ns hydra.core
  (:require [clojure.set :refer [union]]
            [clojure.string :refer [split]]))

(defn- prepend [elem mp]
  (into {} (for [[k v] mp] [(conj k elem) v])))

(defn keys-to-vecs [mp]
  (into {} (for [[k v] mp] [(vec k) v])))

(defn keys-to-path-seq [coll]
  (cond (map? coll) (apply merge (for [[k v] coll] (prepend k (keys-to-path-seq v))))
        (vector? coll) (apply merge (for [i (range (count coll))] (prepend i (keys-to-path-seq (nth coll i)))))
        :else {'() coll}))

(defn to-path-map [coll]
  (-> coll keys-to-path-seq keys-to-vecs))

;(defn- add-paths [mp [hd & tail :as path]]
;  (cond (= 1 (count path)) (assoc mp hd (get mp hd))
;        (= 2 (count path)) (assoc mp hd (second path))
;        :else (assoc mp hd (add-paths (get mp hd {}) tail))))

(defn- add-paths [mp [k v]]
  (println "Examining path k is" k " and v is " v)
  (if (= 1 (count k))
    (assoc mp (first k) v)
    (assoc mp (first k) (add-paths (get mp (first k) {}) [(rest k) v]))))

(defn- from-path-set-to-map-of-maps [path-set]
  (reduce add-paths {} path-set))

(defn- vectorize [m-of-m]
  (let [sub-maps (into {} (for [[k v] m-of-m] [k (if (map? v) (vectorize v) v)]))]
    (if (->> sub-maps keys (every? number?))
      (mapv second (sort-by first (into [] sub-maps)))
      sub-maps)))

(defn from-path-set [ps]
  (-> ps from-path-set-to-map-of-maps vectorize))

(defn path [v]
  (set [(vec v)]))

(defn cleave [pred ps]
  (loop [results '(() ()) paths (seq ps)]
    (if (not paths)
      (list (set (first results)) (set (second results)))
      (let [p (first paths)
            passes (first results)
            fails (second results)]
        (recur (if (pred p) (list (conj passes p) fails) (list passes (conj fails p))) (next paths))))))

;; splice has to eliminate contradictory paths
;; Question, should a pathset contain all subpaths as well ? - then contains? can be used
(defn splice [path-sets]
  (reduce union path-sets))

(defn cross-product [f ps0 ps1]
  (set (for [x ps0 y ps1] (vec (f x y)))))

(defn starts-with? [route path]
  (when (<= (count route) (count path))
    (every? identity (map #(if (clojure.test/function? %1) (%1 %2) (= %1 %2)) route path))))

(defn ends-with? [route path]
  (starts-with? (reverse route) (reverse path)))

(defn apply-leaf [p f]
  (conj (vec (butlast p)) (f (last p))))

(defn transform-leaf [ps route f]
  (let [[passed failed] (cleave #(starts-with? route %) ps)]
    (splice [(set (mapv #(apply-leaf % f) passed)) failed])))

(def transform-leaves transform-leaf)                       ;; Synonym

(defn reset-leaf [ps route val]
  (transform-leaf ps route (constantly val)))

(def reset-leaves reset-leaf)                               ;; Synonym

(defn upsert [routes-path-set target-path-set inserted-path-set]
  (splice [(cross-product concat routes-path-set inserted-path-set) target-path-set]))

(defn insert-at [])

(defn insert-before [])

;(defn insert-after [target-path vector-path-set]
;  (let [largest-index (lergest-index-from-target target)]))

(defn append-next [paths elem]
  (conj paths (conj (vec (last paths)) elem)))

(defn all-subpaths-of-path [path]
  (reduce append-next [] path))
