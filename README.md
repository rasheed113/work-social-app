# Work Social Android

Native Android application for Work Social. The website repository `rasheed113/work-social` is the product source of truth; this repository is the Android implementation boundary and does not modify the website.

## Foundation

- Kotlin + Jetpack Compose
- Native Android application ID: `com.rasheed113.worksocial`
- `versionCode = 1`, `versionName = 1.0.0`
- Supabase Kotlin Auth + PostgREST
- Real persisted Supabase authentication/session restoration
- Authenticated Social / Work House shell with no mock data
- Explicit push/call integration boundaries; no fake delivery
- Release signing configured through environment variables
- Compile/target SDK 37

## Configuration

The Supabase URL is the same project used by the website. The publishable client key must be supplied locally/CI with `WORK_SOCIAL_SUPABASE_PUBLISHABLE_KEY` or the Gradle property `supabasePublishableKey`.

Never supply a Supabase service-role key to this application.

## Build

With Gradle 9.6.1 and JDK 21 available:

```bash
gradle --no-daemon assembleDebug
gradle --no-daemon testDebugUnitTest
```

GitHub Actions installs Gradle 9.6.1 and runs both commands. A Gradle wrapper binary is not committed in this foundation because the available execution environment did not provide Gradle to generate/verify it.

## Release updates

Release APKs must keep the same application ID and production signing key while increasing `versionCode`. See `docs/APK_UPDATES.md`.

## Audit

See `docs/FOUNDATION_AUDIT.md` for the website audit, real backend contracts identified, and intentionally missing push/call contracts.
