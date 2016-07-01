(ns hydra.core)

(defn to-path-map
      ([mp] (to-path-map "" mp))
      ([path mp]
        (into {} (for [[k v] mp :let [new-path (str path "/" k)]]
                      (if (map? v)
                        (to-path-map new-path v)
                        [new-path v])))))

