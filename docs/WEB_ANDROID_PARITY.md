# Work Social Web → Android Product Parity Contract

Status: ACTIVE

The Work Social Web repository (`rasheed113/work-social`) is the primary product/UI/UX source of truth. Android is a native mobile implementation of that product, not an independent Material 3 redesign.

## Source evidence

- Web social router: `src/app/Router.tsx`
- Web base visual language: `src/app/styles.css`
- Web profile/account hierarchy: `src/app/pages/ProfilePage.tsx`
- Web profile settings: `src/app/pages/SettingsPage.tsx`
- Web Work House navigation: `src/features/worker/components/WorkerNavigation.tsx`
- Web Work Home: `src/features/worker/components/WorkerHome.tsx`
- Android shell: `app/src/main/java/com/rasheed113/worksocial/ui/App.kt`
- Android design tokens: `app/src/main/java/com/rasheed113/worksocial/ui/WorkSocialDesignSystem.kt`

## Global rule

For each screen:

`Web structure → Web components → Web actions → Web data/state → Web navigation → Web visual language → native Android implementation`

Android must not invent product hierarchy, duplicate account actions, or replace real data with mocks.

## Screen mapping

| Web | Android | Structure/components/actions | Data/states | Navigation | Current mismatch / implementation |
|---|---|---|---|---|---|
| Home | `SocialHomeScreen` | Feed shell, post cards, create-post entry, social actions | Real Supabase posts, likes/comments; loading/error/empty are repository-driven | Primary Social destination | UI still needs physical visual comparison against Web Home. |
| Friends | `FriendsScreen` | Friend lists/actions/profile opening | Real friends repository and relationship state | Primary Friends destination | UI still needs physical comparison against Web Friends. |
| Activity/Notifications | `ActivityScreen` | Activity list, unread badge, open related post | Real notifications/activity repository | Primary Activity destination | UI still needs physical comparison against Web Activity. |
| Profile | `ProfileScreen` + `ProfileAccountActions` | Profile card, avatar, edit/follow/friend/block actions, Posts; Account section below profile | Real profile/social state | Primary Profile destination | Logout was previously global; now moved to the Profile Account section. Physical UX still UNVERIFIED. |
| Public Profile | `ProfileScreen(targetProfileId)` | Same profile composition but viewer-specific actions; no owner account actions | Real target profile + relationship state | `profile/{profileId}` | Physical visual parity UNVERIFIED. |
| Inbox | `ChatScreen` | Conversation list + message area/call controls | Real private conversations/messages/realtime | Primary Inbox destination | Physical responsive parity UNVERIFIED. |
| Chat | `ChatScreen` conversation state | Conversation header, messages, composer, realtime/call controls | Real private realtime messages | Inbox → conversation | Physical runtime/realtime behavior remains separately verified. |
| Create Post | `CreatePostScreen` | Native form corresponding to Web create-post flow | Real post persistence; no fake success | Home → Create Post | Visual parity UNVERIFIED. |
| Work House | `WorkHouseScreen` | Work Home / Finance / Settings primary navigation | Real worker identity, totals, finance, history | Social → Work House; Work House → primary tabs | Android primary tabs now match Web Home/Finance/Settings. History and Identity are secondary from Home, matching Web hierarchy. |
| Work Finance | `FinanceScreen` | Finance records/summary/actions | Real worker finance repository | Work House → Finance | Visual parity UNVERIFIED. |
| Work History | `WorkHistory` within Work Home | History list, pagination/load-more | Real worker history | Work Home secondary action | Android currently renders as an expandable secondary section rather than a dedicated route; native hierarchy is intentionally being aligned progressively. |
| Worker Identity | `WorkIdentity` within Work Home | Worker identity fields/empty state | Real `worker_profiles` data | Work Home secondary action | Android editing/navigation parity is incomplete and remains explicitly UNVERIFIED. |
| Work Settings | `WorkSettings` | Worker settings and current Team Joining boundary | Real worker identity; no fake team membership | Work House → Settings | Team Joining remains honestly unimplemented where Web says it is not implemented. |
| Profile Settings | `ProfileSettingsScreen` | `Profile Settings` → `Privacy & Safety` | Current Web page is static product structure; Android does not fake blocked-user navigation | Profile → Account → Settings | Blocked Users Android screen is not implemented yet. This is a known parity gap, not replaced by fake navigation. |

## Account / logout contract

The Web owner profile currently exposes an `Account` section containing the `Sign out` action. Android therefore must expose exactly one owner logout entry in the Profile account hierarchy. It must not render a global sign-out control on Home, Friends, Activity, Inbox, Chat, or other pages.

The underlying real authentication logout remains the existing `AuthViewModel.signOut` path. This change is UI hierarchy only; it does not remove logout functionality.

## Web-derived design tokens

Android tokens are extracted from actual Web values rather than a generic Material 3 palette:

- Base text: `#17202A`
- Strong text: `#111827`
- Muted text: `#64748B`
- Base background: `#F5F7FA`
- Surface: `#FFFFFF`
- Base border: `#DFE5EB`
- Premium primary: `#6D5DFC`
- Deep primary: `#5146E5`
- Cyan accent: `#22B8D4`
- Error/account destructive text: `#B4232D`
- Brand gradient: `#6D5DFC → #22B8D4 → #FF5CA8`
- Work/header gradient: dark navy → indigo/purple
- Base card radius: `16px`
- Premium/profile radius: approximately `18–22px`
- Common spacing: `4 / 8 / 12 / 16 / 20 / 24dp`

These tokens describe the source language. They do not constitute proof of visual parity until rendered Android screens are physically compared with the Web.

## Validation status rule

Source inspection can prove structural intent and code wiring. It cannot prove visual parity, touch behavior, device back-stack behavior, realtime delivery, or physical-device rendering.

Therefore every screen not physically tested is **UNVERIFIED** for runtime/visual parity.

## Progressive implementation order

1. Global shell
2. Navigation
3. Home/feed
4. Profile
5. Friends
6. Activity
7. Inbox/chat
8. Create Post
9. Work House
10. Settings/account

Each stage must preserve real Supabase data paths, existing package identity, session/data continuity, and current authentication behavior.
