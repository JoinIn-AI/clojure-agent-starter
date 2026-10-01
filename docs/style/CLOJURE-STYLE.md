# JoinIn Clojure Style Guide

Copyright © 2026 JoinIn AI, Inc. Released under the MIT License (see LICENSE).

This is the **operative** style guide for all Clojure, ClojureScript, and
CLJC code in this repository. It applies to code written by humans and by
Claude Code alike.

## Precedence

1. **This document** — house rules and deviations.
2. **[The community Clojure style guide](https://github.com/bbatsov/clojure-style-guide)**
   (bbatsov, vendored) — the default answer for anything not covered here.
   Rendered version: <https://guide.clojure.style>.
3. **[Nubank's style guide](clojure-style-guide-nubank.md)** (vendored) — a
   fork of the community guide; kept as reference for its namespace, testing,
   and library-organization sections. We adopt specific rules from it below;
   its Nubank-specific alias tables (Datomic, plumatic schema) do **not**
   apply here.

When this document is silent, follow the community guide. When you find a
recurring decision neither covers, add it here.

## Layout & structure (house rules)

These extend the community guide's layout section:

- **One form per line in `let` bindings.** Never pack multiple
  binding pairs on one line.
- **Closing parens on their own line for forms longer than 3 lines.**
  Short forms keep trailing parens gathered, per the community guide.
- **Multi-line docstrings**: verify the closing `"` is present; indent
  continuation lines two spaces (community guide rule).
- **Small functions.** Target ≤ 10 lines, ideally ≤ 5 (adopted from Nubank).
  The REPL-driven test: if a function is hard to eval in isolation, it's
  too big.
- **≤ 3–4 positional parameters** (community + Nubank). Past that, take a
  destructured map.
- **`(comment ...)` blocks at the bottom of files** with executable usage
  examples. These are documentation *and* REPL test snippets.
- **Copyright header** in every new file, single-semicolon banner style
  (the codebase convention, 98 of 101 files):
  `; Copyright © 2026 JoinIn AI, Inc. All rights reserved.`

## Namespaces (adopted from Nubank, adapted)

- Follow [Stuart Sierra's namespace alias guidelines](https://stuartsierra.com/2015/05/10/clojure-namespace-aliases):
  alias = last segment, or last two joined by `.` when ambiguous.
- **One alias per namespace, repo-wide.** Never alias the same namespace two
  different ways in different files. Canonical aliases in this repo:

  | Namespace | Alias |
  |---|---|
  | `clojure.string` | `str` |
  | `clojure.data.json` | `json` |
  | `clojure.spec.alpha` | `s` |
  | `clojure.set` | `set` |
  | `example.log` | `log` |  <!-- replace this table with YOUR core namespaces -->
  |  `example.db` | `fs` |
  |  `example.ulid` | `ulid` |
  |  `example.auth` | `auth` |
  |  `example.meeting-state` | `ms` |
  | `integrant.core` | `ig` |
  | `re-frame.core` | `rf` |
  | `reagent.core` | `r` |
  | `hato.client` | `hato` |
  | `org.httpkit.server` | `http-kit` |
  | `reitit.ring` | `ring` |
  | `muuntaja.core` | `m` |

  When you introduce a new commonly-required namespace, add its alias here.
- **Prefer `:require :as` over `:refer`**; `:refer` is acceptable for a
  handful of symbols (e.g. test macros like `deftest`, `is`, `testing`).
  Never `:refer :all` or `:use` in new code.
- **No single-segment namespaces**; avoid namespaces deeper than 5 segments.

## Data & state (house rules)

- **Plain maps, not records/deftypes**, for data. Reserve protocols for
  genuine polymorphism. Maps are transparent in the REPL.
- **Keyword keys everywhere past the Firestore read boundary** — normalize
  with your DB boundary normalizer. Functions returning raw
  string-keyed Firestore shapes are prefixed `raw-`.
- **Atoms for mutable state**; Integrant for service lifecycle. Every
  service exposes its running system via `(defonce service-state (atom nil))`
  reset in `ig/init-key`.
- **Pure functions, side effects at the edges.** Separate computation from
  I/O (Firestore writes, Pub/Sub publishes) so the pure part is eval-able.
- **Destructure at the boundary**: `(defn process [{:keys [meeting-id text]}] ...)`
  makes the expected shape visible in the signature.
- **Threading macros for data flow**: `(-> event :payload :kind)`, not
  nested `get`s. Don't thread just one call, and don't mix `->`/`->>`
  in one pipeline when a `let` is clearer.
- **Specs at every producer→consumer boundary.** New payload kinds go in
  the shared specs `.cljc` namespace first. Egress validates
  warn-and-continue; ingress validates with `js/console.warn` /
  logged `s/explain-str`.

## Libraries & idioms

- **`clojure.data.json` for all JSON** in new code (not jsonista, not cheshire).
- `(requiring-resolve 'ns/fn)` for lazy loading.
- `(tap> ...)` for debugging intermediate values — never leave `println`
  debugging in committed code.
- Prefer `when` over one-armed `if`; `if-let`/`when-let` where they remove
  a binding+test pair; `cond` with `:else`; sets/keywords as functions
  where idiomatic (community guide, enforced).

## Naming (community guide, highlights we enforce)

- `lisp-case` for functions and vars; `CamelCase` for protocols, records,
  types.
- Predicates end in `?` (`active?`), unsafe-in-transaction fns end in `!`
  (`accrue-meeting-usage!`), conversions use `->` (`event->row`).
- Don't shadow `clojure.core` names (`name`, `type`, `key`, `val`, `map`).

## Comments

- **No provenance comments.** Never reference the old Python/TS sources, a
  ticket-sized history, or "matches X.py:1234". Describe behavior, not
  genealogy.
- **No file:line references to live code either.** "See emitter.clj:196"
  rots the moment that file changes. Name the namespace and var
  (e.g. an emitter's build-event) — those are greppable
  and survive edits.
- Comments explain constraints the code can't show — not what the next
  line does. Prefer making the code self-explanatory.
- `TODO`/`FIXME`/`HACK` annotations per the community guide; include enough
  context that someone else can act on them.

## Testing

- Test namespaces: `your.ns-test` in `test/.../your/ns_test.clj`.
- `deftest` names are descriptive behavior statements:
  `(deftest adjacency-closes-on-answer ...)`. The namespace already carries
  the `-test` suffix; don't repeat it on every deftest. (Deliberate
  deviation from Nubank's `something-test` rule — this codebase's 1300+
  tests use behavior-descriptive names, and they read better in failure
  output.)
- Run incrementally from the REPL: `(clojure.test/run-tests 'your.ns-test)`.
- Errors surfaced by tests are real bugs — fix, don't dismiss as cosmetic.

## ClojureScript / re-frame specifics

- Never dispatch from `:reagent-render` — side-effect dispatches go in
  `:component-did-mount` / `:component-did-update`. This includes the
  **setup body of a form-2 component**: it runs during first render, so no
  `rf/dispatch` there, and an `@(rf/subscribe ...)` deref there captures a
  one-time non-reactive snapshot.
- Wire-format keys (`snake_case`) are renamed to app-db keys (`dash-case`)
  in the ingress event handler, in the same handler that validates the spec.
- WebSocket replacement uses `.close(1000, reason)`, never bare `.close()`.

## Enforcement

- `clj-kondo` runs as a PostToolUse hook and in CI (`bb lint`) — it owns
  syntax/arity/unresolved-symbol errors.
- The **`/style-review` skill** (`.claude/skills/style-review/`) reviews code
  against this guide — run it on a branch before a PR, or over a whole
  subsystem periodically. It covers what clj-kondo can't: naming, structure,
  idiom, alias consistency, boundary discipline.
