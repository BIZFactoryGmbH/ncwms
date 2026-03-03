# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| 2.5.x   | ✅ Yes    |
| < 2.5   | ❌ No     |

## Reporting a Vulnerability

**Please do NOT open a public GitHub Issue for security vulnerabilities.**

Report security issues via **[GitHub Private Security Advisories](https://github.com/Reading-eScience-Centre/ncwms/security/advisories/new)** (requires GitHub login).

Include in your report:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Affected versions
- Suggested fix (optional)

## Response SLA

| Severity | Acknowledgement | Fix Target |
|----------|----------------|------------|
| Critical (CVSS ≥ 9.0) | 24 hours | 7 days |
| High (CVSS 7.0–8.9) | 48 hours | 30 days |
| Medium/Low (CVSS < 7.0) | 5 business days | Next release |

## Scope

In scope:
- Authentication bypass in admin interface (`/admin/*`)
- Unauthenticated access to state-changing endpoints (`/refresh/*`)
- Remote code execution via dataset configuration
- XML injection (XXE) in WMS request parsing
- Path traversal via dataset location configuration

Out of scope:
- Vulnerabilities in downstream dependencies (report upstream; we will track and patch)
- Denial of service via legitimate large WMS requests (by design)
- Issues requiring physical access to the server

## Disclosure Policy

We follow **coordinated disclosure**:
1. You report privately
2. We confirm and fix within SLA
3. We publish a GitHub Security Advisory
4. You may publish your own disclosure 14 days after the fix is released

## CVE Triage Policy

For dependency CVEs:

| CVSS | Action |
|------|--------|
| ≥ 9.0 | Emergency patch release within 7 days |
| 7.0–8.9 | Patch in next scheduled release (≤ 30 days) |
| < 7.0 | Assess reachability; patch in backlog |
| Not reachable via public API | Document in this file; no release required |

## Contact

For general security questions (not vulnerability reports): open a [GitHub Discussion](https://github.com/Reading-eScience-Centre/ncwms/discussions).
