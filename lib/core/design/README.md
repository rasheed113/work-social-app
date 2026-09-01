# Work Social Design System

The Flutter UI is governed by the tokens in `app_tokens.dart` and the Material translation in `app_theme.dart`.

## Web-first source values

- Brand accent gradient: `#6d5dfc → #22c1dc → #ff5ca8`
- Dark hero gradient: `#171a3a → #20265c → #5d2ca8`
- Primary social card radii: `15px`, `22px`, `26px`
- Social card border: `rgba(99,102,241,.15)`
- Primary text: `#172033`
- Muted text: `#64748b`
- Interactive indigo: `#4f46e5`
- Page background: `#f8fafc`

Feature widgets must consume these tokens rather than introduce new hard-coded brand values. Screen-level visual parity is still subject to the screenshot approval gate.
