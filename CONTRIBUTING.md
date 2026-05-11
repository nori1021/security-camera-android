# Contributing & publishing this repo

## Secrets (mandatory before `git push`)

Never commit:

- `SecurityCameraAndroid/local.properties` (SDK path + optional baked-in URLs/keys)
- `securitycam-functions/local.settings.json`
- Azure / ACS / storage keys, PEM files, keystores (`*.jks`, `*.keystore`)
- Large JSON payloads with real biometric samples (`curl-samples/register.json` and `analyze.json` are ignored by default)

If keys ever leaked in Git history, **rotate** them in Azure; rewriting history alone is not sufficient.

## Portfolio screenshots

When adding images to `README.md`, redact:

- Function keys and connection strings
- Real email addresses unless you intend to expose them
- End-user faces — use synthetic or consent-based demo images

## Code style

- User-facing docs and comments added for maintainers: **English**.
- Prefer short **why** comments over narrating **what** the next line does.
