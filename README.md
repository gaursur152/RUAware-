# Crime Report - RUAware

## General Idea
An app for anonymously reporting sexual assault / related incidents on campus,
seeing whether an area has a high volume of reports, and connecting students to
support resources.

- A tab for reporting incidents
- A mapping/aggregation view showing areas with frequent reports
- A resources tab (RUPD, CAPS, VPVA, Nightmover, etc.)

---

## Core Principle: Anonymity by Design

Reports are **fully anonymous by default**. Authentication (proving someone is a
Rutgers student) and attribution (linking a report to a person) are kept as two
separate concerns:

- A student's identity is verified at sign-in/submission time, but that identity
  is **never persisted alongside report content**. Verification is a gate, not a
  foreign key.
- Identity only legitimately enters the system when a reporter *explicitly opts
  in* to escalation (see `EscalationRequest` below). That is the one deliberate,
  consented exception.
- Decisions about retention, mandatory reporting, and what data could be legally
  discoverable should be confirmed with Rutgers VPVA/Title IX before launch, not
  assumed.

---

## Data Model

### `Report` (anonymous)
No user ID, session ID, device ID, IP, or RUID is ever stored on this entity.

| Field | Notes |
|---|---|
| `reportId` | UUID, not sequential (sequential IDs leak submission-order/volume info) |
| `incidentType` | Primary category — see taxonomy below |
| `incidentSubtype` | Optional, more specific detail |
| `description` | Free text |
| `locationZone` | Coarse campus zone — see taxonomy below, never exact GPS/address |
| `occurredAtBucket` | Coarse time bucket (e.g. weekday evening / date range), not exact timestamp |
| `severityLevel` | Optional, self-reported: Low / Moderate / High / Prefer not to say |
| `urgencyLevel` | Optional, self-reported: Immediate-Ongoing / Recent / Past / Prefer not to say |
| `reportSubmittedAt` | Exact system timestamp — fine to store, it's about the data not the incident |
| `status` | `submitted → under_review → {closed_no_action, referred_to_resources, awaiting_reporter_response, escalation_offered, closed}` |
| `campus` | New Brunswick / Newark / Camden |
| `reporterTokenHash` | Keyed hash of the reporter's persistent `AnonymousAccountToken` (see below) — used to match status-check requests and as a pattern-clustering signal for reviewers. Never the token itself. |

**Explicitly excluded:** RUID, session/device identifiers, raw IP (long-term),
exact GPS, free-text address, unstripped file/photo metadata (EXIF can contain
GPS/device info — strip before storage if attachments are ever added).

#### `incidentType` taxonomy (draft — confirm against Clery Act / VPVA categories)
- Sexual Assault
- Dating Violence
- Domestic Violence
- Stalking
- Harassment
- Other (free text)

#### `locationZone` taxonomy (two-tier: Campus → Zone)
- New Brunswick: College Ave, Busch, Livingston, Cook/Douglass, Downtown New Brunswick
- Newark
- Camden
- Off-campus (its own option — many incidents won't occur on campus property)

Stops at zone level deliberately — building-level + time bucket + incident type
together risks re-identifying a specific incident.

### `AnonymousAccountToken` (persistent pseudonym, not an identity)
Issued once per verified student, used both to submit/check-status on reports
and as the basis for abuse-prevention. Deliberately **not** derived from RUID —
generated randomly so there is never a recoverable path back to a person.

| Field | Notes |
|---|---|
| `tokenHash` | Keyed hash (HMAC with a server-held secret) of a high-entropy random token (recommend 256 bits, base32-encoded for manual transcription, excluding ambiguous characters like `0`/`O`, `1`/`l`). The raw token is shown to the student **once** at issuance and never stored. |
| `submissionCount` / `windowStart` | Rolling counter for rate-limiting, checked at submission time instead of scanning the `Report` table. |
| `lastUsedAt` | For abuse monitoring. |

**Issuance flow:** student verifies via the email-verification MVP (see Account
Provisioning below) → server generates the random token → shown once with a
"save this now, we cannot recover it" warning → RUID/email is discarded
immediately → only `tokenHash` persists.

**Design tradeoff, decided deliberately:** using one persistent token for both
login and rate-limiting means every report submitted under it shares the same
`reporterTokenHash`, so they're linkable to each other (same anonymous source)
even though never linkable to an identity. This was chosen over fully
independent per-report tokens because it (a) enables real per-person rate
limiting that survives session logout, and (b) doubles as the pattern-tracking
signal reviewers use (see Triage Workflow) — "these reports share a source" is
useful to VPVA/Title IX even without knowing who.

**Revocation/moderation:** a keyed hash means rotating the server secret
invalidates all tokens at once (incident response). A specific abusive
`tokenHash` can also be individually blocklisted without ever knowing who
issued it.

**No recovery mechanism should exist** — any "recover my token" flow would
require re-verifying identity, defeating the purpose. Losing the token means
losing access to that history; the submission-confirmation UI must make this
unambiguous.

### `EscalationRequest` (identified, opt-in only)
The one entity in the system that holds real contact info. Held to a stricter
access/audit standard than everything else.

| Field | Notes |
|---|---|
| `escalationId` | UUID |
| `linkedReportId` | Nullable — populated only if reporter used their `AnonymousAccountToken` to opt into escalation from an existing anonymous report; null if they went straight to this path |
| `requestType` | Align with VPVA/Title IX's existing intake categories — e.g. RUPD Report, Title IX Formal Process, Supportive Measures Only, Consultation/Information |
| `requestFollowUpContact` | Boolean — a reporter may want a pattern flagged/patrols increased *without* personal contact |
| `contactMethod` / `contactInfo` | Required only if follow-up requested; encrypted at rest |
| `preferredContactNotes` | Optional — safe times/methods to reach them (trauma-informed detail) |
| `consentTimestamp` | Explicit record of when identity-attachment was consented to |
| `status` | `pending → contacted → in_progress → closed` |
| `assignedCoordinator` | Which staff member is handling it |
| `createdAt` | |

**Access:** narrower than general report review — recommend a `COORDINATOR`-only
role, its own separate audit log, and a conversation with VPVA/Title IX about
whether this needs isolation from the anonymous-report store entirely.

**Retention:** likely differs from the anonymous `Report` table (Title IX records
often carry federal recordkeeping requirements). Don't assume a retention period
— confirm with VPVA/Title IX.

---

## Reviewer / Triage Workflow

**Reviewers are trained VPVA / Title IX staff** (not general admins or student
volunteers). Two role types to confirm with them:
- `ADVOCATE` — support/resources focus
- `COORDINATOR` — formal process, compliance, and the only role with access to
  `EscalationRequest` contact info

**Queue:** priority queue, not FIFO — sorted by `urgencyLevel`, then
`severityLevel`, then oldest-first within a tier. Reports unreviewed past an SLA
threshold (e.g. 24-48h) surface to the top regardless of priority.

**Claiming:** reviewers claim a report (`under_review by [reviewer]`) rather than
being pre-assigned, with an auto-release timeout if a claimed report goes
untouched.

**Status lifecycle:** see `Report.status` above. Note there is no unilateral
"escalated" action — a reviewer cannot push an anonymous report to RUPD, since
there's no identity to hand over. The real action is replying via the ticket
link to offer escalation; it only proceeds if the reporter opts in through
`EscalationRequest`.

**Pattern tracking:** even when no individual follow-up is possible, repeated
reports sharing a zone/time/category may obligate the Title IX office to act
(e.g. increased patrols, safety alerts) without identifying anyone. This should
have its own queue/view, separate from case-by-case triage, and ties directly
into the frequency-map aggregation below.

**Audit trail:** append-only log of every reviewer view/action on a report
(reviewer ID + timestamp), separate from the `EscalationRequest` audit log.

---

## Frequency Mapping / Aggregation

- Computed from a separate aggregated table/materialized view — never queries
  raw report rows live.
- Buckets by `locationZone` + `occurredAtBucket` (+ possibly `incidentType`),
  deliberately coarse.
- Enforces a minimum count threshold (k-anonymity) before a bucket is rendered
  publicly — e.g. no display until a cell has ≥5 reports.
- `severityLevel`/`urgencyLevel` are excluded from the public aggregate
  dimensions — they stay internal to reviewer triage only, since adding more
  dimensions makes individual reports easier to re-identify.
- Resolution may differ by audience: public/student view likely campus-zone
  level; reviewer view may justify finer resolution — an explicit policy
  decision, not a default.

---

## Abuse Prevention (Anonymous Submission)

No identity exists to throttle against, so prevention works on the
`AnonymousAccountToken`, not on who someone is:

- **CAPTCHA/proof-of-work on submission** — cheap first line of defense against
  naive bot spam, independent of identity.
- **Per-token rate limiting** — `AnonymousAccountToken.submissionCount` /
  `windowStart` caps submissions per rolling window. This survives logout/new
  sessions, unlike session- or IP-based limiting.
- **Progressive friction, not hard bans** — escalate (CAPTCHA → delay →
  temporary cooldown) rather than a permanent block, since a hard ban is easy
  to route around (new verification) and risks blocking a legitimate student
  who tripped a threshold.
- **Blocklist a specific `tokenHash`** if abuse is clearly occurring, without
  ever needing to know who it belongs to.
- Malicious/false *content* (as opposed to volume) is left to reviewer triage
  rather than automated detection — a bad-faith report mostly costs reviewer
  time, so it doesn't justify heavier identity-adjacent tracking unless real
  abuse patterns show up in practice.

---

## Resources Tab

Lower-risk, essentially a CMS: category (RUPD, CAPS, VPVA, Nightmover, blue-pole
locations, etc.), name, contact method, hours, description. Kept in its own
service/table, independent of the report pipeline.

---

## Account Provisioning / Auth Model

Two separate flows — student access and reviewer access should never share a
sign-up path.

### Student-facing access
Full Rutgers NetID/RUID SSO (CAS/Shibboleth) would be ideal but likely requires
an official partnership with Rutgers OIT, which may be out of reach for a
student-built app initially. Practical MVP path:
- **Email-domain verification**: OTP/magic link sent to a `@scarletmail.rutgers.edu`
  (or `@rutgers.edu`) address, confirming Rutgers affiliation without needing
  official SSO integration.
- The verified email is used only to establish a session (proof of "verified
  Rutgers student"); it is not stored alongside report content. If dedup/abuse
  prevention requires tracking an account, store only a salted hash of the
  email, not the email itself.
- True RUID/CAS SSO integration is a stretch goal pending Rutgers IT
  partnership — worth validating feasibility early rather than assuming it.

### Reviewer / Coordinator access
- **No self-registration.** Accounts are provisioned out-of-band by whoever
  administers the app, based on a roster provided by VPVA/Title IX — not open
  sign-up, even via Rutgers email.
- Role assigned at provisioning time: `ADVOCATE` or `COORDINATOR`.
- MFA required for any `ADVOCATE`/`COORDINATOR` login, given the sensitivity of
  what these accounts can access — stricter than the student-facing flow.
- Shorter session idle-timeout than student sessions.

---

## Open Questions for VPVA / Title IX Office
- Final `incidentType` / `EscalationRequest.requestType` taxonomy — align with
  their existing intake categories and Clery Act reporting categories.
- Retention policy for `Report` vs. `EscalationRequest` (likely different).
- How they currently log/act on anonymous tips today — mirror existing process
  where possible rather than designing from scratch.
- Whether `ADVOCATE`/`COORDINATOR` role split matches how their office actually
  divides responsibilities.
- Feasibility/timeline of official RUID/CAS SSO integration vs. the email-
  verification MVP.

---

## API
Open to Rutgers Students (see Account Provisioning above for the access model).

Filter for the campus you'd like to look on.

What we want to raise awareness to:
- Blue light poles — the one nearest to you
- Rutgers student mental health resources (CAPS, VPVA)
- The Nightmover

## Community Forum
A place where... *(to be defined)*
