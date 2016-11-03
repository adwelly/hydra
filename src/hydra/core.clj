(ns hydra.core
  (:require [clojure.set :refer [union]]
            [clojure.string :refer [split]]))

(defrecord IndexWrapper [index]
  Comparable
  (compareTo [_ o] (- index (get o :index))))

(defn index-wrapper? [x]
  (instance? hydra.core.IndexWrapper x))

(defn pos-index-wrapper? [x]
  (and (instance? hydra.core.IndexWrapper x) (<= 0 (:index x))))

(defn neg-index-wrapper? [x]
  (and (instance? hydra.core.IndexWrapper x) (> 0 (:index x))))

(defn- prepend [elem mp]
  (into {} (for [[k v] mp] [(conj k elem) v])))

(defn keys-to-vecs [mp]
  (into {} (for [[k v] mp] [(vec k) v])))

(defn keys-to-path-seq [coll]
  (cond (map? coll) (apply merge (for [[k v] coll] (prepend k (keys-to-path-seq v))))
        (vector? coll) (apply merge (for [i (range (count coll))] (prepend (IndexWrapper. i) (keys-to-path-seq (nth coll i)))))
        (set? coll) (let [set-as-seq (seq coll)]
                      (apply merge (for [i (range (count coll))] (prepend (IndexWrapper. (* (+ i 1) -1)) (keys-to-path-seq (nth set-as-seq i))))))
        :else {'() coll}))

;; To and from path map

(defn to-path-map [coll]
  (-> coll keys-to-path-seq keys-to-vecs))

(defn- add-paths [mp [k v]]
  (if (= 1 (count k))
    (assoc mp (first k) v)
    (assoc mp (first k) (add-paths (get mp (first k) {}) [(rest k) v]))))

(defn- from-path-set-to-map-of-maps [path-set]
  (reduce add-paths {} path-set))

(defn- insert-sets-vectors [m-of-m]
  (let [sub-maps (into {} (for [[k v] m-of-m] [k (if (map? v) (insert-sets-vectors v) v)]))]
    (cond (->> sub-maps keys (every? pos-index-wrapper?)) (mapv second (sort-by first (into [] sub-maps)))
          (->> sub-maps keys (every? neg-index-wrapper?)) (set (mapv second (into [] sub-maps)))
          :else sub-maps)))

(defn from-path-map [pm]
  (-> pm from-path-set-to-map-of-maps insert-sets-vectors))

;; Basic operators

(defn path [v]
  {(-> v butlast vec) (last v)})

(defn cleave [pm pred]
  (loop [results '(() ()) paths (seq pm)]
    (if (not paths)
      (list (into {} (first results)) (into {} (second results)))
      (let [[k v] (first paths)
            passes (first results)
            fails (second results)]
        (recur (if (pred k v) (list (conj passes [k v]) fails) (list passes (conj fails [k v]))) (next paths))))))

;; Some predicates for cleave

(defn starts-with? [route path _]
  (when (<= (count route) (count path))
    (every? identity (map #(if (clojure.test/function? %1) (%1 %2) (= %1 %2)) route path))))

(defn ends-with? [route path val]
  (starts-with? (reverse route) (reverse path) val))

;; Other operators

(def upsert merge)

(defn splice [[pm0 pm1]]
  (merge pm0 pm1))

(defn kmap [f pm]
  (zipmap (map f (keys pm)) (vals pm)))

(defn vmap [f pm]
  (zipmap (keys pm) (map f (vals pm))))
;
;(defn- width-at [pm route]
;  (let [route-len (count route)
;        [passed failed] (cleave pm #(starts-with? route %))
;        passed-keys (keys passed)
;        indexes (map #(:index  (nth % route-len)) passed-keys)]
;    (inc (apply max indexes))))
;
;(defn insert-at [])
;
;(defn insert-before [])
;
;(defn upsert-after [pm route inserted-pm]
;  (let [width (width-at pm route)
;        new-path (conj route width)]
;    (upsert (path new-path) pm inserted-pm)))
