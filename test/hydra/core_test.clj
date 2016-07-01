(ns hydra.core-test
    (:require [midje.sweet :refer :all]
      [hydra.core :refer :all]))

(def simple-map
  {"a" {"b" 1}
   "c" 2
   "d" {"e" 3
        "f" {"g" 4
             "h" 5}}})

(fact "A map of maps can create a pathmap"
      (to-path-map simple-map) => {"/a/b"   1
                                   "/c"     2
                                   "/d/e"   3
                                   "/d/f/g" 4
                                   "/d/f/h" 5})

