(ns hydra.core-test
  (:require [midje.sweet :refer :all]
            [midje.util :refer [testable-privates]]
            [hydra.core :refer :all])
  (:import (hydra.core IndexWrapper)))

(testable-privates hydra.core prepend from-path-set-to-map-of-maps vectorize prepend-path width-at)


(def simple-map
  {"a" 1 "b" 2 "c" 3})

(def simple-path-map {["a"] 1 ["b"] 2 ["c"] 3})

(def simple-vec
  ["a" "b" "c"])

(def two-level-map
  {"a" 1 "b" 2 "c" {"d" 3}})

(def two-level-path-map {["a"] 1 ["b"] 2 ["c" "d"] 3})

(def deeply-nested-map
  {"a" {"b" 1}
   "c" 2
   "d" {"e" 3
        "f" {"g" 4
             "h" 5}}})

(def deeply-nested-path-map {["a" "b"] 1 ["c"] 2 ["d" "e"] 3 ["d" "f" "g"] 4 ["d" "f" "h"] 5})

(def simple-map-with-vector
  {"a" ["b" "d" "e"]
   "f" 2})

(def simple-map-with-vector-path-map {["a" #hydra.core.IndexWrapper{:index 0}] "b"
                                      ["a" #hydra.core.IndexWrapper{:index 1}] "d"
                                      ["a" #hydra.core.IndexWrapper{:index 2}] "e"
                                      ["f"] 2})

(def vector-with-map
  [{"a" 1} {"b" 2} {"c" 3}])

(def numeric-keyed-map
  {#hydra.core.IndexWrapper{:index 0} "a"
   #hydra.core.IndexWrapper{:index 1} "b"
   #hydra.core.IndexWrapper{:index 2} "c"})

(def map-with-numeric-keys-representing-vector
  {"a" {#hydra.core.IndexWrapper{:index 0} "b"
        #hydra.core.IndexWrapper{:index 1} "c"
        #hydra.core.IndexWrapper{:index 2} "d"}
   "e" 2})

(def deeply-nested-map-with-vectors
  {"a" {"b" 1}
   "c" ["i" "j" "k"]
   "d" {"e" 3
        "f" {"g" 4
             "h" [5 6 7]}}})

(fact "index-wrapper? detects IndexWrapper objects"
      (index-wrapper? true) => false
      (index-wrapper? 17) => false
      (index-wrapper? (->IndexWrapper 17)) => true
      (index-wrapper? (->IndexWrapper -5)) => true)


(fact "prepending an element to a map prepends the element to each key of the map"
      (prepend :a {'(:b :c) :d '(:f :g) :h}) => {'(:a :b :c) :d '(:a :f :g) :h})

(fact "simple map path-set test"
      (to-path-map simple-map) => simple-path-map)

(fact "simple vec path-set test"
      (to-path-map simple-vec) => {[#hydra.core.IndexWrapper{:index 0}] "a" [#hydra.core.IndexWrapper{:index 1}] "b" [#hydra.core.IndexWrapper{:index 2}] "c"})

(fact "two level map test"
      (to-path-map two-level-map) => two-level-path-map)

(fact "A deeply nested map of maps can create a pathmap"
      (to-path-map deeply-nested-map) => deeply-nested-path-map)

(fact "You can mix maps and vectors"
      (to-path-map simple-map-with-vector) => simple-map-with-vector-path-map)

(fact "vectors can be the outermost structure"
      (to-path-map vector-with-map) => {[#hydra.core.IndexWrapper{:index 0} "a"] 1
                                        [#hydra.core.IndexWrapper{:index 1} "b"] 2
                                        [#hydra.core.IndexWrapper{:index 2} "c"] 3})

(fact "You can recreate a simple map"
      (from-path-set-to-map-of-maps simple-path-map) => simple-map)

(fact "You can recreate a two level map"
      (from-path-set-to-map-of-maps two-level-path-map) => two-level-map)

(fact "you can recreate a deeply nested map"
      (from-path-set-to-map-of-maps deeply-nested-path-map) => deeply-nested-map)

(fact "vectorizing maps without numeric keys returns an unchanged map"
      (vectorize simple-map) => simple-map
      (vectorize two-level-map) => two-level-map
      (vectorize deeply-nested-map) => deeply-nested-map)

(fact "vectorizing a map with exclusively numeric keys returns a vector"
      (vectorize numeric-keyed-map) => ["a" "b" "c"])

(fact "vectorizing a map with embeded numeric keys returns a map with an embeded vector"
      (vectorize map-with-numeric-keys-representing-vector) => {"a" ["b" "c" "d"] "e" 2})

(fact "you can convert a path set into map of maps and vecs"
      (from-path-map simple-map-with-vector-path-map) => simple-map-with-vector)

(fact "you can make a round trip from maps to path sets and back"
      (-> deeply-nested-map-with-vectors to-path-map from-path-map) => deeply-nested-map-with-vectors)

(fact "path creates a path map representing a simple path"
      (path [1 2 3]) => {[1 2] 3}
      (path '("a" "b" "c")) => {["a" "b"] "c"})

(defn path-longer-than? [len path]
  (> (count path) len))

(fact "You can cleave a set of paths into two with a predicate"
      (-> (cleave (to-path-map deeply-nested-map-with-vectors) (partial path-longer-than? 3)) first) =>
      {["d" "f" "h" #hydra.core.IndexWrapper{:index 2}] 7
       ["d" "f" "h" #hydra.core.IndexWrapper{:index 1}] 6
       ["d" "f" "g"] 4
       ["d" "f" "h" #hydra.core.IndexWrapper{:index 0}] 5}
      (-> (cleave (to-path-map deeply-nested-map-with-vectors) (partial path-longer-than? 3) ) second) =>
      {["a" "b"] 1
       ["c" #hydra.core.IndexWrapper{:index 2}] "k"
       ["c" #hydra.core.IndexWrapper{:index 0}] "i"
       ["d" "e"] 3
       ["c" #hydra.core.IndexWrapper{:index 1}] "j"})

(fact "Splicing is the inverse of cleaving"
      (from-path-map (apply merge (-> deeply-nested-map-with-vectors to-path-map (cleave (partial path-longer-than? 3)))))  => deeply-nested-map-with-vectors)

(fact "splicing can update values, (c moves from 3 to 4)"
      (-> (merge simple-path-map {["c"] 4}) from-path-map) => {"a" 1 "b" 2 "c" 4})

(fact "prepend path takes a pair representing a path and value, from one map and a pair representing the path and value from a second map and prepends the first to the second"
      (prepend-path [[:a :b] :c] [[:d :e] :f]) => [[:a :b :c :d :e] :f])

(future-fact "Cross product applies a binary function taking two pairs and returning a pair, to every combinations of path/value in two path maps"
      (cross-product prepend-path {[:a] 1 [:b] 2} {[:c] 3 [:d] 4}) => {[:a 1 :c] 3 [:a 1 :d] 4 [:b 2 :c] 3 [:b 2 :d] 4})

(fact "modifying leaf"
      (-> deeply-nested-map-with-vectors to-path-map (reset-leaf ["a"] 42) from-path-map) =>
      {"a" {"b" 42}
       "c" ["i" "j" "k"]
       "d" {"e" 3
            "f" {"g" 4
                 "h" [5 6 7]}}})

(fact "transforming leaves"
      (-> deeply-nested-map-with-vectors to-path-map (transform-leaf ["d"] inc) from-path-map) =>
      {"a" {"b" 1}
       "c" ["i" "j" "k"]
       "d" {"e" 4
            "f" {"g" 5
                 "h" [6 7 8]}}})

(fact "The starts-with? function identifies paths starting with a given route"
      (starts-with? [] ["a" 0 "b"]) => true
      (starts-with? ["a" 0] ["a" 0 "b"]) => true
      (starts-with? ["a" number?] ["a" 0 "b"]) => true
      (starts-with? ["a" 1] ["a" 0 "b"]) => false
      (starts-with? ["a" string?] ["a" 0 "b"]) => false)

(fact "The ends-with? function identifies paths starting with a given route"
      (ends-with? [] ["a" 0 "b"]) => true
      (ends-with? [0 "b"] ["a" 0 "b"]) => true
      (ends-with? [number? "b"] ["a" 0 "b"]) => true
      (ends-with? [1 "b"] ["a" 0 "b"]) => false
      (ends-with? [string? "b"] ["a" 0 "b"]) => false)

(future-fact "upsert allows structures to be inserted into other structures"
      (let [deep (to-path-map deeply-nested-map-with-vectors)
            simple (to-path-map simple-map)]
        (-> (upsert (path ["d" "f" "i"]) deep simple) from-path-map)) =>
      {"a" {"b" 1}
       "c" ["i" "j" "k"]
       "d" {"e" 3
            "f" {"g" 4
                 "h" [5 6 7]
                 "i" {"a" 1
                      "b" 2
                      "c" 3}}}})

(future-fact "upsert allows vectors to be created on the fly"
      (let [deep (to-path-map deeply-nested-map-with-vectors)
            simple (to-path-map simple-map)]
        (-> (upsert (path ["d" "f" "i" 0]) deep simple) from-path-map)) =>
      {"a" {"b" 1}
       "c" ["i" "j" "k"]
       "d" {"e" 3
            "f" {"g" 4
                 "h" [5 6 7]
                 "i" [{"a" 1
                       "b" 2
                       "c" 3}]}}})

(fact "width at finds the width of a vector at the given route"
      (width-at (to-path-map deeply-nested-map-with-vectors) ["c"]) => 3)

(future-fact "upsert-after wilL place a new element at the end of a vector"
      (let [deep (to-path-map deeply-nested-map-with-vectors)
            simple (to-path-map simple-map)]
        (-> (upsert-after deep ["c"] simple) from-path-map)) =>
      {"a" {"b" 1}
       "c" ["i" "j" "k" {"a" 1
                         "b" 2
                         "c" 3}]
       "d" {"e" 3
            "f" {"g" 4
                 "h" [5 6 7]}}})
