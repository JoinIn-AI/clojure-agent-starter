# CLAUDE.md

Operating doctrine for an AI agent writing Clojure against a live REPL. This
is the distilled version of what JoinIn runs in production; adjust the port
numbers and paths to your project.

## Write first, then eval

Edit the file, then eval the form in the REPL to verify it works. Not the
other way around.

Why this order: if you eval first and it works but never gets written to a
file, it's lost on the next reload — and you won't know until it's gone. The
file is the source of truth. The clj-kondo hook catches syntax errors on
write; the REPL eval confirms behavior.

Work in small increments — one `defn` at a time, not 500 lines. Write it,
eval it, confirm it, move to the next.

**Read-only evals don't need files.** Queries, inspections, and debugging
evals are ephemeral — eval them freely. Only *mutations* (new defns, changed
functions) need the write-first rule.

## REPL interaction

Use `clj-nrepl-eval` (or your nREPL client of choice) with heredoc syntax to
avoid shell escaping issues with `!` in names like `swap!`:

```bash
clj-nrepl-eval --discover-ports        # find running nREPLs
clj-nrepl-eval -p 7888 <<'EOF'
(require '[example.specs :as specs])
(clojure.spec.alpha/exercise ::specs/transcript-chunk 3)
EOF
```

## Leverage the running image

The JVM holds the full application state in memory. Use it.

- **Inspect live state** — `(deref state-atom)` instead of adding log
  statements. Never add temporary print debugging when you can query the
  live image.
- **Snapshot for offline debugging** — `(def debug-state @some-atom)`
  captures live state; then test pure functions against `debug-state`
  without touching the running system.
- **Don't restart after fixes** — re-eval the fixed `defn` and the running
  service picks it up immediately. If you're suggesting a service restart
  after a code change, you're doing it wrong.
- **Guard against image drift** — the image and the files can diverge.
  Three checks before committing:
  1. **Cold load** (fresh JVM): `clojure -M -e "(require 'the.namespace)"`
  2. **Read the diff** — verify what you wrote matches what you tested
  3. **Run tests** — incrementally: `(clojure.test/run-tests 'the.ns-test)`
- **Explore Java APIs** — `(bean obj)` to see properties as a map,
  `(.getMethods SomeClass)` to list methods. Faster than Javadocs.
- **Debug spec failures** — `(s/explain ::spec bad-data)` names the exact
  failing field and predicate. Don't guess.

## Expose the system for introspection

Every service init should `reset!` the running system map into a top-level
`(defonce service-state (atom nil))`. Without it the system is closure-local
and live inspection from nREPL can't reach it.

## Write code the REPL can chew

- **Pure functions, side effects at the edges.** The pure part is freely
  eval-able; the I/O wrapper is thin and obvious.
- **Plain maps, not custom types.** Maps are transparent in the REPL.
  Records and deftypes are opaque. Reserve protocols for genuine
  polymorphism.
- **Small functions.** A 5-line function is one eval. If a function is hard
  to eval in isolation, it's too big.
- **Destructure at the boundary** — the expected shape is visible in the
  signature.
- **Threading for data flow** — `(-> event :payload :kind)` reads
  left-to-right.

## Specs at service boundaries

Clojure's silent `nil` on missing keys is the bug-class equivalent of a
wrong-type return. The mitigation is `clojure.spec.alpha` at every
producer→consumer boundary. New payload kinds go in the specs namespace
*first* as design documentation, then get wired at ingress/egress. Use spec
everywhere — discipline is not scarce anymore.

## Structural discipline

- One form per line in `let` bindings.
- Close all parens for forms longer than 3 lines on their own line.
- Multi-line docstrings: always verify the closing `"` is present.
- **Parenthesis repair is automatic** — the `clj-paren-repair-claude-hook`
  (PreToolUse) fixes delimiter errors before writes land; the clj-kondo hook
  (PostToolUse) catches semantic errors after. Do NOT count parens manually.
  If clj-kondo blocks a write, the error is semantic (wrong arity, unresolved
  symbol), not structural.

## Idioms

- Macros are a last resort, as is the style. Don't reach for them early.
- `(comment ...)` blocks at the bottom of files — executable examples.
- `(tap> ...)` to inspect intermediate values during debugging.
- Atoms for mutable state (thread-safe CAS).
- No provenance comments: describe the behavior, not which old file it came
  from — those comments rot the moment the old code changes.

## Integration test errors are real

When tests surface errors, fix them — don't dismiss them as "cosmetic." Any
error that reaches a log is a real failure mode in some code path; the test
just happens to be the first place that exercised it.
