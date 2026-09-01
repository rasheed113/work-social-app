# Work Social Android device forensics

## P0 evidence boundary

Current forensic conclusion:

`SOURCE CLEAN -> CI CLEAN -> APK CLEAN -> DEVICE UNKNOWN`

The physical-device failure previously observed as `ejpcgcaoqyqjionvtsdi.supabase.co))` remains a P0 runtime-verification blocker until a physical-device run proves the actual request host.

Do not change the canonical Supabase URL as part of this procedure.

## Debug diagnostic

Debug builds record a small `WorkSocialForensics` log stream. It is not shown in normal production UI and is disabled when `BuildConfig.DEBUG` is false.

Recorded fields:

- application version and versionCode
- BuildConfig Supabase hostname
- Supabase client initialization success/failure
- failing request hostname when it can be extracted from the exception message
- exception class/message
- sanitized stack trace

Passwords, access/refresh/session tokens, bearer credentials, and JWTs are redacted before logging. No user data is intentionally collected.

The diagnostic establishes the chain:

`BuildConfig host -> Supabase client initialization -> failing request host`

## Physical-device procedure

1. Keep the existing installed `com.rasheed113.worksocial` application. Update it normally; do not uninstall, clear data, or change the package ID.
2. Install the CI-generated APK from the new workflow artifact over the existing installation.
3. Before launching, record the artifact SHA-256 from CI.
4. On the device, verify package identity is exactly `com.rasheed113.worksocial`.
5. Verify `versionCode` and `versionName` match the CI artifact metadata. The next release is versionCode `2`, versionName `1.1.0`.
6. Verify the installed APK SHA-256 matches the CI artifact SHA-256.
7. Reproduce the login failure without changing app data.
8. Capture only the `WorkSocialForensics` diagnostic output. Do not capture credentials, tokens, or private user data.
9. Compare the three hosts in order: BuildConfig host, Supabase client host, and actual failing request host.
10. If the actual request host contains the extra `))`, classify P0 as `BROKEN` at runtime even if source/CI/APK remain clean. If the actual request host is canonical, the prior device symptom is not reproduced and P0 can move to `REAL VERIFIED` only after the physical run is independently documented.

## APK identity commands

On a machine with Android platform tools, use package inspection and SHA-256 tooling against the downloaded artifact. The commands may vary by OS; the required evidence is the exact package ID, versionCode, versionName, and SHA-256, not a particular command implementation.

Never use uninstall/reinstall as part of the normal verification workflow.
