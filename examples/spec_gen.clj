;; Copyright © 2026 JoinIn AI, Inc. Released under the MIT License (see LICENSE).
;;
;; Generated test data from specs — runnable demo.
;;
;;   clojure -M examples/spec_gen.clj
;;
;; Every spec doubles as a generator. This prints valid event payloads
;; conjured from the definitions in src/example/specs.cljc, then proves the
;; round-trip: everything generated is accepted by the validator.
(require '[clojure.spec.alpha :as s]
         '[example.specs :as specs])

(println "── (s/exercise ::specs/transcript-chunk 5) ──")
(doseq [[sample] (s/exercise ::specs/transcript-chunk 5)]
  (prn sample))

(println)
(println "── later samples grow realistic ──")
(prn (first (last (s/exercise ::specs/transcript-chunk 40))))

(println)
(print "── round-trip: 100 generated payloads, all valid? ")
(println (every? #(s/valid? ::specs/payload %)
                 (map first (s/exercise ::specs/payload 100))))
