(ns hydra.core
  (:require [clojure.set :refer [union]]
            [clojure.string :refer [split]]))

(defrecord IndexWrapper [index]
  Comparable
  (compareTo [_ o] (- index (get o :index))))

(defn index-wrapper? [x]
  (instance? hydra.core.IndexWrapper x))

(defrecord SetWrapper [uuid])

(defn uuid []
  (str (java.util.UUID/randomUUID)))

(defn set-wrapper? [x]
  (instance? hydra.core.SetWrapper x))

(defn- prepend [elem mp]
  (into {} (for [[k v] mp] [(conj k elem) v])))

(defn keys-to-vecs [mp]
  (into {} (for [[k v] mp] [(vec k) v])))

(defn keys-to-path-seq [coll]
  (cond (map? coll) (apply merge (for [[k v] coll] (prepend k (keys-to-path-seq v))))
        (vector? coll) (apply merge (for [i (range (count coll))] (prepend (IndexWrapper. i) (keys-to-path-seq (nth coll i)))))
        (set? coll) (let [set-as-seq (seq coll)]
                      (apply merge (for [i (range (count coll))] (prepend (SetWrapper. (uuid)) (keys-to-path-seq (nth set-as-seq i))))))
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
    (cond (->> sub-maps keys (every? index-wrapper?)) (mapv second (sort-by first (into [] sub-maps)))
          (->> sub-maps keys (every? set-wrapper?)) (set (mapv second (into [] sub-maps)))
          :else sub-maps)))

(defn from-path-map [pm]
  (-> pm from-path-set-to-map-of-maps insert-sets-vectors))

;; Basic operators

(defn path [v]
  {(-> v butlast vec) (last v)})

(defn starts-with? [route path]
  (when (<= (count route) (count path))
    (every? identity (map #(if (clojure.test/function? %1) (%1 %2) (= %1 %2)) route path))))

(defn ends-with? [route path]
  (starts-with? (reverse route) (reverse path)))

(def upsert merge)

(defn kmap [pm f]
  (zipmap (map f (keys pm)) (vals pm)))

(defn vmap [pm f]
  (zipmap (keys pm) (map f (vals pm))))

(defn kreduce [pm init f]
  (reduce f init (keys pm)))

(defn vreduce [pm init f]
  (reduce f init (vals pm)))

(defn pmfilter [pm pred]
  (filter (fn [[k v]] (pred k v)) pm))

(defn kfilter [pm pred]
  (into {} (for [[k v] pm :when (pred k)] [k v])))

(defn vfilter [pm pred]
  (into {} (for [[k v] pm :when (pred v)] [k v])))

;; Some predicates for kreduce

(defn largest-index [route i path]
  (let [cr (count route)
        index (if (and (starts-with? route path)
                       (< cr (count path))
                       (index-wrapper? (nth path cr)))
                (:index (nth path cr)) -1)]
    (if (< i index) index i)))

;; Higher level functions

(defn append-value [pm route v]
    (assoc pm (conj route (IndexWrapper. (kreduce pm -1 (partial largest-index route)))) v))

(defn incr-when-gteq [path index]
  (let [iw (last path)
        original-index (get iw :index)
        new-index (if (>= original-index index) original-index (inc original-index))]
    (conj (vec (butlast path)) (IndexWrapper. new-index))))

(defn insert-value-at [pm route index v]
  (let [vector-paths (kfilter pm #(starts-with? route %))
        updated-paths (kmap vector-paths #(incr-when-gteq index %))
        new-path {(conj route (IndexWrapper. index)) v}]
    (upsert pm updated-paths new-path)))


