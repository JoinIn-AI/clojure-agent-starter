---
name: style-review
description: Review Clojure/ClojureScript/CLJC code against the JoinIn house style guide (docs/style/CLOJURE-STYLE.md). Use when the user asks for a style review, style check, or to check code against the style guide — for a diff, a file, a subsystem, or the whole codebase.
---

# Style Review

Review Clojure code against the JoinIn house style guide. This covers what
clj-kondo can't: naming, structure, idiom, alias consistency, and boundary
discipline. It is NOT a bug hunt (use /code-review for that) and NOT a
formatter (clj-kondo + the paren-repair hook own syntax).

## Inputs

Scope from `$ARGUMENTS`, defaulting sensibly:
- No args + on a feature branch with changes → review the diff vs the base
  branch (changed `.clj`/`.cljs`/`.cljc` files only).
- A path (file or directory) → review those files.
- `all` / `codebase` → review all of `src` (fan out subagents by
  area; see below).

## Method

1. **Load the rulebook.** Read `docs/style/CLOJURE-STYLE.md` in full. It has
   precedence over the vendored community guide
   (`docs/style/clojure-style-guide-community.adoc`), which is the fallback
   for anything the house guide doesn't cover.

2. **Mechanical checks first** (cheap, run over the whole scope with grep —
   do these before reading files):
   - **Alias consistency**: tally `[ns :as alias]` across the scope
     (`grep -rhoE '\[[a-z][a-z0-9._-]+ :as [a-z0-9.-]+\]' <scope> | sort -u`),
     flag any namespace with 2+ aliases and any alias diverging from the
     canonical table in CLOJURE-STYLE.md.
   - `:refer :all` or `:use` in `ns` forms.
   - Missing file headers, per the project's header convention (if any).
   - `println` outside `(comment ...)` blocks and dev/script namespaces.
   - jsonista/cheshire requires in new code (should be `clojure.data.json`).
   - Provenance comments (grep for `\.py:`, `\.ts:`, `app\.py`, references
     to the old Python/TS sources in comments/docstrings).
   - `deftest` names not ending in `-test`.

3. **Read-through review** of each file in scope against the guide's
   sections: layout (let-binding one-per-line, closing parens, docstrings),
   function size (>10 lines is a flag; judge whether splitting genuinely
   helps), naming (`?`/`!`/`->` conventions, core shadowing), data & state
   (records where maps would do, keyword-vs-string key discipline at the
   Firestore boundary, side effects mixed into pure logic, missing specs at
   producer→consumer boundaries), idioms (one-armed `if`, nested `get`s vs
   threading, `cond` without `:else`).

   For codebase-wide scope, fan out one subagent per top-level `src`
   area, each given the rulebook
   summary and told to return findings as structured `file:line — rule —
   note` entries. Do not have agents propose rewrites — findings only.

4. **Filter ruthlessly.** Report only findings a reviewer would act on.
   Skip: pure formatting the tools already enforce, matters of taste the
   guide doesn't cover, and violations inside vendored/generated code.
   When the codebase consistently deviates from the guide in some way,
   report it ONCE as a guide-vs-reality decision to make, not N times.

5. **Report.** Group by severity:
   - **Guide violations** — concrete `file:line` findings with the rule.
   - **Consistency drift** — same-thing-two-ways findings (aliases, key
     conventions, test naming).
   - **Guide gaps** — recurring patterns the guide should rule on; propose
     the rule.

   For a diff-scoped review, present findings inline in the conversation.
   For subsystem/codebase scope, also write the report to
   `docs/style/reviews/<YYYY-MM-DD>-<scope>.md`.

## Fixing

Only apply fixes when the user asked for them (`--fix` or explicit
follow-up). Fix mechanically checkable items first (aliases, headers,
refer :all); structural findings (function splits, boundary refactors) get
one change per commit, following the write-first-then-eval REPL discipline
from CLAUDE.md.
