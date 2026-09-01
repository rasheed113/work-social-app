# Work Social Flutter Android

This repository is the native Android client for the `rasheed113/work-social` web application.

## Run

Install Flutter, then from the repository root:

```bash
flutter pub get
flutter analyze
flutter test
flutter run --dart-define=SUPABASE_URL="<existing-project-url>" --dart-define=SUPABASE_PUBLISHABLE_KEY="<existing-publishable-key>"
```

The publishable key is client-safe; never place a service-role/secret key in this application.

## Current implementation

- Flutter entrypoint and Supabase initialization
- Supabase PKCE authentication
- Persistent Supabase session handling through `supabase_flutter`
- Router/auth guard
- Five-destination navigation shell
- Real public `posts` query with `profiles` relation
- Real post insertion
- `post-media` Storage uploads
- `post_attachments` persistence
- cleanup of uploaded objects and post row when attachment processing fails
- native image/file/location selection

## Source of truth

UI, routes, Supabase contracts and behavior are ported from `rasheed113/work-social`. No mock/seed data is used.

Before merging a feature, verify it against the corresponding web component and its Supabase API contract.
