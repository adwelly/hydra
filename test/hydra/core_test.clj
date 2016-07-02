(ns hydra.core-test
  (:require [midje.sweet :refer :all]
            [hydra.core :refer :all]))

(def simple-map
  {"a" 1 "b" 2 "c" 3})

(def simple-vec
  ["a" "b" "c"])

(def two-level-map
  {"a" 1 "b" 2 "c" {"d" 3}})

(def deeply-nested-map
  {"a" {"b" 1}
   "c" 2
   "d" {"e" 3
        "f" {"g" 4
             "h" 5}}})

(def simple-map-with-vector
  {"a" ["b" "d" "e"]
   "f" 2})

(def vector-with-map
  [{"a" 1} {"b" 2} {"c" 3}])


(fact "simple map path-set test"
      (to-path-set simple-map) => #{"/a/1" "/b/2" "/c/3"})

(fact "simple vec path-set test"
      (to-path-set simple-vec) => #{"/0/a" "/1/b" "/2/c"})

(fact "two level map test"
      (to-path-set two-level-map) => #{"/a/1" "/b/2" "/c/d/3"})

(fact "A deeply nested map of maps can create a pathmap"
      (to-path-set deeply-nested-map) => #{"/a/b/1" "/c/2" "/d/e/3" "/d/f/g/4" "/d/f/h/5"})

(fact "You can mix maps and vectors"
      (to-path-set simple-map-with-vector) => #{"/a/0/b" "/a/1/d" "/a/2/e" "/f/2"})

(fact "vectors can be the outermost structure"
      (to-path-set vector-with-map) => #{"/0/a/1" "/1/b/2" "/2/c/3"})


