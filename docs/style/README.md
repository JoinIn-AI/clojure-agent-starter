# Style guides

| File | What it is |
|---|---|
| [CLOJURE-STYLE.md](CLOJURE-STYLE.md) | **The operative house guide (JoinIn's, shipped as a worked example).** Start here. |
| [clojure-style-guide-community.adoc](clojure-style-guide-community.adoc) | Vendored copy of the community Clojure style guide ([bbatsov/clojure-style-guide](https://github.com/bbatsov/clojure-style-guide), CC BY 3.0). Fallback for anything the house guide doesn't cover. |
| [clojure-style-guide-nubank.md](clojure-style-guide-nubank.md) | Vendored copy of [Nubank's fork](https://github.com/nubank/clojure-style-guide) (CC BY 3.0). Reference only — we adopted its namespace/testing rules into the house guide; its Nubank-specific alias tables don't apply. |

The vendored copies are snapshots (fetched 2026-07-02) — don't edit them
locally; re-fetch upstream if they need updating. House rules and deviations
go in `CLOJURE-STYLE.md`.

To review code against the guide, use the `/style-review` skill
(`.claude/skills/style-review/`).
