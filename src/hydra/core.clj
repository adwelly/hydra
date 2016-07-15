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

(defn splice [path-sets]
  (reduce union path-sets))

(defn cross-product [f ps0 ps1]
  (set (for [x ps0 y ps1] (vec (f x y)))))

(defn starts-with? [route path]
  (when (< (count route) (count path))
    (every? identity (for [i (range (count route)) :let [route-val (nth route i) path-val (nth path i)]]
                       (if (clojure.test/function? route-val)
                         (route-val path-val)
                         (= route-val path-val))))))

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

;(defn insert [target-path target-path-set inserted-path-set]
;  (union (map #(vec (concat target-path %)) inserted-path-set) target-path-set))

(defn update [routes-path-set target-path-set inserted-path-set]
  (splice [(cross-product concat routes-path-set inserted-path-set) target-path-set]))

(defn insert-at [])

(defn insert-before [])

;(defn insert-after [target-path vector-path-set]
;  (let [largest-index (lergest-index-from-target target)]))

(defn append-next [paths elem]
  (conj paths (conj (vec (last paths)) elem)))

(defn all-subpaths-of-path [path]
  (reduce append-next [] path))




