# Clojure Agent Starter

The companion repo to **"What Tipped the Scales for Clojure"**
(Clojure/conj 2026, Brendan Foote). Everything here is lifted from the setup
JoinIn runs in production, where an AI agent rewrote ~116K lines of
TypeScript/Python into ~37K lines of Clojure and has been the primary pair
ever since.

Clone it, hand it to your agent — you're 80% of the way.

## What's in the box

| Piece | Where | Talk section |
|---|---|---|
| Paren-repair hook (PreToolUse) + clj-kondo-on-write (PostToolUse) | `.claude/settings.json`, `scripts/hook-clj-kondo.sh` | §8 — the parens tax, mechanized away |
| clj-kondo security ruleset (RCE/XXE var bans) | `.clj-kondo/config.edn` | — |
| Structured logging utility | `src/example/log.clj` | §8 — stack traces, tamed |
| Spec patterns: envelope + payload multispec, boundary helpers | `src/example/specs.cljc` | §6 — one definition replaces four |
| Generated test data demo | `examples/spec_gen.clj` | §6 — every spec is a generator |
| REPL-tuned `diagnosing-bugs` skill (fork of [Matt Pocock's](https://github.com/mattpocock/skills)) | `.claude/skills/diagnosing-bugs/` | §3 — the loop |
| `style-review` skill + the house style guide it enforces | `.claude/skills/style-review/`, `docs/style/CLOJURE-STYLE.md` | — |
| Agent operating doctrine (write-first-then-eval, live-image discipline, image-drift checks) | `CLAUDE.md` | §3 |

## Quickstart

Prerequisites: JDK 21+, [Clojure CLI](https://clojure.org/guides/install_clojure),
[clj-kondo](https://github.com/clj-kondo/clj-kondo),
[Babashka](https://babashka.org) (the hooks and `clj-nrepl-eval` are bb scripts),
`jq` (used by the lint hook), and [bbin](https://github.com/babashka/bbin):

```bash
bbin install https://github.com/bhauman/clojure-mcp-light.git --tag v0.2.2
```

That one install provides both `clj-paren-repair-claude-hook` (the PreToolUse
hook) and `clj-nrepl-eval` (the REPL client the skills and CLAUDE.md use).

Then:

```bash
# see spec-generated test data (the §6 demo)
clojure -M examples/spec_gen.clj

# start an nREPL the agent can drive
clojure -M:nrepl
```

Open the repo in Claude Code and the hooks and skills are live: writes get
paren-repaired and cljfmt'd before they land, then linted by clj-kondo;
`/diagnosing-bugs` reaches for the running REPL before anything else.

## Adapting to your project

- `CLAUDE.md` is the doctrine — merge it into your own, keep your port map.
- `example.log` and `example.specs` are patterns, not a library: rename the
  namespaces and grow them in place.
- The skills assume a live nREPL and `clj-nrepl-eval` (any nREPL CLI client
  works — adjust the commands inside the skills).

## The claim, in one line

Don't let the agent dictate the language. The agent can handle yours.
