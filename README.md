# hydra

A tiny Clojure library for working with deeply nested data structures

<p style="text-align: right;">Hail Hydra!<br>
- Captain America</p>

## Rationale
Working with a deeply nested data structures in Clojure can be difficult. In the absence of any other library, the usual approach to changing such a structure is to navigate to the area of interest in the structure, produce a modified version, and then reacreate the rest of the original structure with the new piece 'plugged in' (for some value of 'plugged in').

In Clojure this is generally done with a combination of map, reduce, get-in and update-in. It can be time consuming and error prone to create such functions and they are difficult to read and understand afterwards.

Alternative approaches include the standard Clojure zipper library and Nathan Marz's clever and highly performant library [Specter](https://github.com/nathanmarz/specter).

Hydra is an experimental library that takes a different approach to the problem. The basic assumption is that if it is difficult to work with a deeply nested data structure, then the standard abstraction for representing that structure may not be the best choice.

The standard approach to representing a tree of data is to nest maps and perhaps sequences. A particular leaf can be reached with the get-in function, and modified structures created with update-in.

Hydra's approach is to replace the nested map with a collection of paths where paths are vectors. Since each path in a tree is unique, the fundamental data structure is a set of paths.

This alternative representation of trees seems to offer a fruitful and extremely compact set of functions for manipulating trees. Ignoring comments, the library is extremely small and one of the goals of this work is to keep it that way.

As it's not particularly convenient to create trees represented as sets of vectors, a translation function to-path-set is provided, along with from-path-set to translate from Hydra's representation to the standard on completion.

## Why 'Hydra' ?
The classical [Hydra](https://en.wikipedia.org/wiki/Lernaean_Hydra) was a multi-headed serpent with the unfortunate ability to grow extra heads if one was cut off. Over the last 2000 years its occasionally been used as a metaphor for a tricky problem. It's also true that if you turn a typical whiteboard diagram of a tree upside down you get something that might just be a Hydra if you squint hard enough. However the truth is more frivolous - its actually an excuse for the quote at the top of this page and the thought that the Specter might also have been named after a famous criminal organization (yes I know the spelling isn't quite right).

## Usage
TBS when the design has settled dow

## Examples

Example 1: Append a sequence of elements to a nested vector

  (def data {:a [1 2 3]})

  ;; Manual Clojure
  (update data :a (fn [v] (reduce conj v [4 5])))

  ;; Hydra


Example 2: Increment every even number nested within map of vector of maps

  (def data {:a [{:aa 1 :bb 2}
                 {:cc 3}]
             :b [{:dd 4}]})

  ;; Manual Clojure
  (defn map-vals [m afn]
    (->> m (map (fn [[k v]] [k (afn v)])) (into {})))

  (map-vals data
    (fn [v]
      (mapv
        (fn [m]
          (map-vals
            m
            (fn [v] (if (even? v) (inc v) v))))
        v)))

  ;; Hydra


Example 3: Reverse the order of even numbers in a tree (with order based on depth first search):

(transform (subselect (walker number?) even?)
  reverse
  [1 [[[2]] 3] 5 [6 [7 8]] 10])
;; => [1 [[[10]] 3] 5 [8 [7 6]] 2]

Example 4: Replace every continuous sequence of odd numbers with its sum:


## Stability
Note the use of the word 'experimental' further up the page. Things may change radically. Use at your own risk.

## Limitations
The library currently deals with maps containing vectors and vectors containing maps. It does not yet allow sets to be included as interior structure. This _may_ change in future versions.

The library makes an assumption that the input structure is a tree and therefore acyclic. This is not going to change.

Performance is currently ignored.

## License

Copyright © 2016 Andy Dwelly

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
