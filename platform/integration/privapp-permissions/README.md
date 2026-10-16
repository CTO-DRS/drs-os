# Privapp-Permissions Templates

Status: **intentionally EMPTY whitelists.** In Phase 3 v1 the DRS
Security Center requests zero permissions, so there is nothing to
whitelist — and manufacturing a whitelist "for later" is exactly how
over-privileged platforms happen. This directory exists to pin the
RULE for the privileged-integration stage:

1. A permission enters the whitelist only with: (a) the platform API
   that requires it, (b) the exact user-facing capability it enables,
   (c) why a normal permission grant is insufficient.
2. Each entry lives in `privapp-permissions-com.drs.os.securitycenter.xml`
   with an inline comment carrying (a)–(c).
3. The whitelist is reviewed together with `system/sepolicy` changes
   (C-1) and never ships with `allow-all` wildcards.

When the privileged stage of the Security Center begins (SafetyCenter
surfacing, permission-usage reads), its required signatures appear
here first, in review, before any build consumes them.
