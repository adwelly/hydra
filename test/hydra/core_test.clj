(ns hydra.core-test
  (:require [midje.sweet :refer :all]
            [midje.util :refer [testable-privates]]
            [hydra.core :refer :all])
  (:import (hydra.core IndexWrapper)))

(testable-privates hydra.core prepend from-path-set-to-map-of-maps insert-sets-vectors prepend-path width-at)


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

(def deeply-nested-map-with-sets-and-vectors
  {:a {:b 1}
   :c #{"i" "j" #{:k}}
   "d" {"e" 3
        "f" [4 :h #{5 6 7}]}})

(def simple-set-map
  {[#hydra.core.IndexWrapper{:index -1}] :a
   [#hydra.core.IndexWrapper{:index -2}] :b})

(fact "index-wrapper? detects IndexWrapper objects"
      (index-wrapper? true) => false
      (index-wrapper? 17) => false
      (index-wrapper? (->IndexWrapper 17)) => true
      (index-wrapper? (->IndexWrapper -5)) => true)

(fact "pos-index-wrapper? detects positive IndexWrapper objects"
      (pos-index-wrapper? true) => false
      (pos-index-wrapper? 17) => false
      (pos-index-wrapper? (->IndexWrapper 17)) => true
      (pos-index-wrapper? (->IndexWrapper -5)) => false)

(fact "neg-index-wrapper? detects negative IndexWrapper objects"
      (neg-index-wrapper? true) => false
      (neg-index-wrapper? 17) => false
      (neg-index-wrapper? (->IndexWrapper 17)) => false
      (neg-index-wrapper? (->IndexWrapper -5)) => true)

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
      (insert-sets-vectors simple-map) => simple-map
      (insert-sets-vectors two-level-map) => two-level-map
      (insert-sets-vectors deeply-nested-map) => deeply-nested-map)

(fact "vectorizing a map with exclusively numeric keys returns a vector"
      (insert-sets-vectors numeric-keyed-map) => ["a" "b" "c"])

(fact "vectorizing a map with embeded numeric keys returns a map with an embeded vector"
      (insert-sets-vectors map-with-numeric-keys-representing-vector) => {"a" ["b" "c" "d"] "e" 2})

(fact "you can convert a path set into map of maps and vecs"
      (from-path-map simple-map-with-vector-path-map) => simple-map-with-vector)

(fact "you can make a round trip from maps to path sets and back"
      (-> deeply-nested-map-with-vectors to-path-map from-path-map) => deeply-nested-map-with-vectors)

(fact "path creates a path map representing a simple path"
      (path [1 2 3]) => {[1 2] 3}
      (path '("a" "b" "c")) => {["a" "b"] "c"})

(defn path-longer-than? [len path _]
  (> (count path) len))

(fact "You can cleave a set of paths into two with a predicate"
      (-> (cleave (to-path-map deeply-nested-map-with-vectors) (partial path-longer-than? 3)) first) =>
      {["d" "f" "h" #hydra.core.IndexWrapper{:index 2}] 7
       ["d" "f" "h" #hydra.core.IndexWrapper{:index 1}] 6
       ["d" "f" "h" #hydra.core.IndexWrapper{:index 0}] 5}
      (-> (cleave (to-path-map deeply-nested-map-with-vectors) (partial path-longer-than? 3) ) second) =>
      {["a" "b"] 1
       ["c" #hydra.core.IndexWrapper{:index 2}] "k"
       ["c" #hydra.core.IndexWrapper{:index 0}] "i"
       ["d" "f" "g"] 4
       ["d" "e"] 3
       ["c" #hydra.core.IndexWrapper{:index 1}] "j"})

(fact "upsert is the inverse of cleaving"
      (from-path-map (apply upsert (-> deeply-nested-map-with-vectors to-path-map (cleave (partial path-longer-than? 3)))))  => deeply-nested-map-with-vectors)

(fact "splice is the inverse of cleaving"
      (-> deeply-nested-map-with-vectors to-path-map (cleave (partial path-longer-than? 3)) splice from-path-map) => deeply-nested-map-with-vectors)

(fact "upsert can update values, (c moves from 3 to 4)"
      (-> (upsert simple-path-map {["c"] 4}) from-path-map) => {"a" 1 "b" 2 "c" 4})

(fact "kmap maps a function over the keys of a map"
      (kmap {[:a :b] 1 [:c :d] 2} #(conj % :e)) => {[:a :b :e] 1 [:c :d :e] 2})

(fact "vmap maps a function over the vals of a map"
      (vmap {[:a :b] 1 [:c :d] 2} inc) => {[:a :b] 2 [:c :d] 3})

(fact "The starts-with? function identifies paths starting with a given route"
      (starts-with? [] ["a" 0 "b"] 7) => true
      (starts-with? ["a" 0] ["a" 0 "b"] 7) => true
      (starts-with? ["a" number?] ["a" 0 "b"] 8) => true
      (starts-with? ["a" 1] ["a" 0 "b"] 9) => false
      (starts-with? ["a" string?] ["a" 0 "b"] 7) => false)

(fact "The ends-with? function identifies paths starting with a given route"
      (ends-with? [] ["a" 0 "b"] 3) => true
      (ends-with? [0 "b"] ["a" 0 "b"] 2) => true
      (ends-with? [number? "b"] ["a" 0 "b"] 1) => true
      (ends-with? [1 "b"] ["a" 0 "b"] 7) => false
      (ends-with? [string? "b"] ["a" 0 "b"] 9) => false)


;; Tests for sets

(fact "A set is converted to paths with index wrappers with unique negative numbers in it"
      (to-path-map #{:a}) => {[#hydra.core.IndexWrapper{:index -1}] :a}
      (-> (to-path-map #{:a :b}) clojure.set/map-invert :a first index-wrapper?) => true
      (-> (to-path-map #{:a :b}) clojure.set/map-invert :b first index-wrapper?) => true
      (let [index-a (-> (to-path-map #{:a :b}) clojure.set/map-invert :a first :index)
            index-b (-> (to-path-map #{:a :b}) clojure.set/map-invert :b first :index)]
        index-a => (fn [x] (< x 0))
        index-b => (fn [x] (< x 0))
        (= index-a index-b) => false))

(fact "a pathmap representing a set is converted to a set by from-path-map"
      (from-path-map simple-set-map) => #{:a :b})

(fact "You can do a round trip with deeply nested maps containing sets and vectors"
      (-> deeply-nested-map-with-sets-and-vectors to-path-map from-path-map) => deeply-nested-map-with-sets-and-vectors)


(def world
  {:people
   [{:money 129825 :name "Alice Brown"}
    {:money 100 :name "John Smith"}
    {:money 500000000 :name "Scrooge McDuck"}
    {:money 2870 :name "Charlie Johnson"}
    {:money 8273280 :name "Michael Smith"}]
   :bank {:funds 470000000000}})

;; Anyone with more than 100000 must give 100 to the bank....

(defn accounts-with-more-than-100000? [path v]
  (and (starts-with? [:people] path v)
       (ends-with? [:money] path v)
       (<= 100000 v)))

(fact "The accounts with more than 100,000 are selected by the accounts-with-more-than-100000? function"
      (-> world to-path-map (cleave accounts-with-more-than-100000?) first from-path-map) =>
        {:people
               [{:money 129825}
                {:money 500000000}
                {:money 8273280}]})

(fact "vmap allows a fixed amount to be removed"
      (let [big-accounts (-> world to-path-map (cleave accounts-with-more-than-100000?) first)]
        (from-path-map (vmap big-accounts #(- % 1000)))) =>
      {:people
       [{:money 128825}
        {:money 499999000}
        {:money 8272280}]})

(fact "the standard count function allows the amount going to the bank to be calculated"
      (let [big-accounts (-> world to-path-map (cleave accounts-with-more-than-100000?) first)]
        (* 1000 (count big-accounts)) => 3000))

(fact "the original bank funds is given by the standard get function:"
      (-> world to-path-map (get [:bank :funds])) => 470000000000)

(fact "this can now be assembled into a working example"
      (let [world-paths (to-path-map world)
            [big-accounts _] (cleave world-paths accounts-with-more-than-100000?)
            taxed-accounts (vmap big-accounts #(- % 1000))
            total-collected (* 1000 (count big-accounts))]
        (from-path-map (upsert world-paths taxed-accounts {[:bank :funds] (+ total-collected (world-paths [:bank :funds]))}))) =>
      {:people
             [{:money 128825 :name "Alice Brown"}
              {:money 100 :name "John Smith"}
              {:money 499999000 :name "Scrooge McDuck"}
              {:money 2870 :name "Charlie Johnson"}
              {:money 8272280 :name "Michael Smith"}]
       :bank {:funds 470000003000}})







