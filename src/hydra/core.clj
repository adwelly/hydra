(ns hydra.core)

(defn to-path-map
  ([coll] (to-path-map "" coll))
  ([path coll]
   (if (map? coll)
     (into {} (for [[k v] coll] (to-path-map (str path "/" k) v)))
     (if (vector? coll)
       (into {} (for [i (range (count coll))] (to-path-map (str path "/" i) (nth coll i))))
       [path coll]))))
