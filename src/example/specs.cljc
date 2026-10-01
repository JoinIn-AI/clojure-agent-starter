;; Copyright © 2026 JoinIn AI, Inc. Released under the MIT License (see LICENSE).
(ns example.specs
  "The spec patterns from the talk, in miniature.

   One spec definition replaces four artifacts from the old stack (TS types +
   Zod schemas, Python type hints + Pydantic models) and covers:
   - development-time checking (s/valid?, s/explain at the REPL)
   - runtime validation at service boundaries
   - generated test data (s/exercise) — see the comment block at the bottom

   The pattern that carries a whole event-sourced system: an envelope spec plus
   a multispec that dispatches payload validation on a :kind field. New payload
   kinds get a spec here FIRST — the spec is the design document — then get
   wired at ingress/egress.

   .cljc on purpose: the same forms load on the JVM and in ClojureScript, so
   the browser validates with the same registry the server does. Nothing to
   drift."
  (:require [clojure.spec.alpha :as s]))

;; ── Envelope ─────────────────────────────────────────────────────────────────

(s/def ::event-id string?)
(s/def ::ts-server string?)
(s/def ::source #{"client" "system" "ingest"})
(s/def ::envelope
  (s/keys :req-un [::event-id ::ts-server ::source ::payload]))

;; ── Payloads: multispec dispatching on :kind ─────────────────────────────────

(s/def ::text (s/and string? seq))
(s/def ::speaker_id string?)
(s/def ::speaker_name string?)
(s/def ::is_final boolean?)

;; Pin each payload's :kind to its literal — that's what makes the multispec
;; dispatch AND the generator produce the right tag.
(s/def :example.specs.transcript-chunk/kind #{"transcript_chunk"})
(s/def ::transcript-chunk
  (s/keys :req-un [:example.specs.transcript-chunk/kind ::text]
          :opt-un [::speaker_id ::speaker_name ::is_final]))

(s/def :example.specs.timing-signal/kind #{"timing_signal"})
(s/def ::signal #{"lull" "overlap" "handoff"})
(s/def ::timing-signal
  (s/keys :req-un [:example.specs.timing-signal/kind ::signal]))

(defmulti payload-kind :kind)
(defmethod payload-kind "transcript_chunk" [_] ::transcript-chunk)
(defmethod payload-kind "timing_signal"    [_] ::timing-signal)

(s/def ::payload (s/multi-spec payload-kind :kind))

;; ── Boundary helpers ─────────────────────────────────────────────────────────

(defn explain-or-nil
  "nil when valid, else the s/explain-str — the agent's error signal.
   Warn-and-continue at egress; reject at ingress. Either way the string names
   the exact failing key and predicate, which is what the agent needs to fix it."
  [spec x]
  (when-not (s/valid? spec x)
    (s/explain-str spec x)))

(comment
  ;; ── Generated test data — the part we never used in production, and should
  ;;    have. Every spec is also a generator:

  (s/exercise ::transcript-chunk 3)
  ;; Real output, generated inside JoinIn's running ingest service from the
  ;; production specs (2026-09-28):
  ;; [{:kind "transcript_chunk", :text "P"}
  ;;  {:kind "transcript_chunk", :text "z"}
  ;;  {:speaker_name "a", :kind "transcript_chunk", :text "vO"}]
  ;; test.check sizing ramps up — later samples get realistic:
  ;; {:speaker_name "sUD9BAJ", :speaker_id "Wn",
  ;;  :kind "transcript_chunk", :text "vv73P92Qc66S0T"}

  ;; Round-trip: everything the generator emits, the validator accepts.
  (every? #(s/valid? ::payload %)
          (map first (s/exercise ::transcript-chunk 40)))
  ;; => true

  ;; Feed generated payloads through a pure reducer and assert invariants —
  ;; property-based testing with zero fixture files:
  (map first (s/exercise ::payload 10))

  ;; Or hand one spec's generator to another tool entirely:
  (require '[clojure.spec.gen.alpha :as sgen])
  (sgen/sample (s/gen ::timing-signal) 5))
