(ns hydra.core-test
  (:require [midje.sweet :refer :all]
            [hydra.core :refer :all]))


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

(fact "A map of maps can create a pathmap"
      (to-path-map deeply-nested-map) => {"/a/b" 1
                                   "/c"          2
                                   "/d/e"        3
                                   "/d/f/g"      4
                                   "/d/f/h"      5})

(fact "A map of maps can create a pathmap"
      (to-path-map simple-map-with-vector) => {"/a/0"   "b"
                                               "/a/1"   "d"
                                               "/a/2"   "e"
                                               "/f"     2})

(fact "two level test"
      (to-path-map two-level-map) => {"/a" 1
                                      "/b"    2
                                      "/c/d"  3})

(fact "vector test"
      (to-path-map vector-with-map) => {"/0/a" 1 "/1/b" 2 "/2/c" 3})

