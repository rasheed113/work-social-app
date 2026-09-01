# Phase 10 — Android calling contract

The Android calling implementation follows the existing Work Social website contract instead of inventing a second signaling protocol.

## Backend contract used

`public.call_signals` is the signaling table used by the website. The audited migration defines:

- `call_id` UUID identifying a call
- `conversation_id` referencing `public.conversations`
- `sender_id` and `recipient_id` referencing `public.profiles`
- `kind`: `audio` or `video`
- `signal_type`: `offer`, `answer`, `ice`, `hangup`, `reject`
- JSONB `sdp` and `candidate`
- participant/conversation-member RLS for reads and inserts
- Supabase Realtime publication for `call_signals`

Android writes through the authenticated Supabase client. The sender is taken from the authenticated session, not from UI identity state. Backend RLS remains authoritative for conversation membership and recipient membership.

## WebRTC contract

The website uses a real WebRTC peer connection and the same signaling values. Android now uses a native WebRTC `PeerConnection`, real SDP offer/answer, trickled ICE, microphone capture, optional camera capture, remote video tracks, connection-state handling and cleanup.

The STUN/TURN endpoints match the existing website source contract. No Android-only TURN credentials were invented.

## Incoming/background limitation

Foreground/background-while-process-is-alive incoming signaling is delivered through authenticated Supabase Realtime. The existing website contract does **not** provide Android device-token registration, FCM routing, or a locked/process-dead wake-up contract.

Therefore Android does not claim true process-dead incoming-call delivery and does not fake a push notification. A production background calling contract still requires:

1. authenticated Android device-token persistence;
2. server-side push routing for `call_signals` offers;
3. FCM (or the backend's chosen push provider) credentials/configuration on the server only;
4. an Android incoming-call wake-up/notification path tied to the authenticated recipient.

Until that server contract exists, foreground Realtime calling remains real and the missing background transport is explicitly bounded rather than simulated.
