# Work Social Android Foundation Audit

Date: 2026-08-31  
Source: `rasheed113/work-social` (`main`)  
Target: `rasheed113/work-social-app` (`main`)

## Website audit

- **Framework/build:** React + TypeScript + Vite. `package.json` builds with TypeScript and Vite and depends on React/React DOM and `@supabase/supabase-js`.
- **Supabase:** `src/lib/supabase/client.ts` uses the Work Social Supabase project, a publishable client key, PKCE, persisted sessions and automatic token refresh.
- **Authentication:** email/password sign-in, email/password sign-up with `display_name` metadata, sign-out, session restoration, OAuth code exchange, and an auth-state subscription.
- **Application shell:** `src/app/App.tsx` restores the session, then renders `src/app/Router.tsx` for the authenticated product shell.
- **Social routes:** `/`, `/friends`, `/notifications`, `/profile`, `/profile/settings`, `/inbox`, `/chat`, `/blocked-users`, and public profile routes.
- **Work routes:** `/work`, `/work/identity`, `/work/finance`, `/work/settings`, `/work/settings/team-joining`, and `/work/history`. The worker feature tree also contains Work House, Diary and related components/pages.
- **Social data:** posts, profiles, notifications, conversations/messages/members and related interactions are backed by Supabase queries/RPCs/realtime.
- **Work data:** worker APIs expose `work_entries`, worker profile, finance and diary contracts. Work history uses cursor pagination and period bounds; totals use `get_worker_work_totals`.
- **Notifications:** the website currently uses browser Notification APIs plus Supabase realtime events for notifications/messages. That is not Android push delivery.
- **Chat/calls:** the website has chat data/realtime and a `public.call_signals` table. The audited call migration defines audio/video signals with offer/answer/ice/hangup/reject types, participant RLS and Supabase Realtime publication.
- **Background-call gap:** the audited call signaling contract does not itself provide Android background/locked-device wake-up. A real device-token/push transport contract is still required.
- **Environment:** the website `.env.example` contains the Supabase URL and publishable key only. No service-role credential belongs in the client.

## Android foundation

- Kotlin + Gradle + Android SDK + Jetpack Compose.
- Application ID: `com.rasheed113.worksocial`.
- Initial version: `versionCode 1`, `versionName 1.0.0`.
- Minimum SDK 26; compile/target SDK 36.
- Supabase Kotlin client `3.7.0` with Auth and PostgREST modules.
- Auth state is driven by Supabase `sessionStatus`; no hardcoded user identity or mock login exists.
- Only Social and Work House product boundaries are navigable in this phase. Individual feature screens are deliberately not faked.
- Push and incoming-call interfaces are explicit integration boundaries. There is no fake push delivery or fake call UI.
- Release signing is environment-driven. Future release APKs must use the same production signing key to be normal Android updates.
- Debug builds use an application-id suffix and are not release-update artifacts.

## Missing follow-up contracts

1. Real Android device-token persistence/registration API.
2. FCM delivery and authenticated notification routing.
3. Background/locked incoming-call wake-up tied to `call_signals`.
4. Real Social feature slices from website APIs/RLS.
5. Real Work House feature slices from worker APIs/RPCs.
