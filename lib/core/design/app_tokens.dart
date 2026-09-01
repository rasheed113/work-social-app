import 'package:flutter/material.dart';

/// Work Social design tokens translated from the web CSS.
/// Keep visual values here; feature widgets should not invent brand colors,
/// radii, borders, or elevations locally.
class AppTokens {
  const AppTokens._();

  // Brand palette used by the web gradient system.
  static const brandPurple = Color(0xFF6D5DFC);
  static const brandIndigo = Color(0xFF5B4DE8);
  static const brandCyan = Color(0xFF22C1DC);
  static const brandPink = Color(0xFFFF5CA8);
  static const darkPurple = Color(0xFF171A3A);
  static const darkIndigo = Color(0xFF20265C);

  static const pageBackground = Color(0xFFF8FAFC);
  static const surface = Color(0xFFFFFFFF);
  static const surfaceSoft = Color(0xFFF1F5FF);
  static const textPrimary = Color(0xFF172033);
  static const textSecondary = Color(0xFF64748B);
  static const textMuted = Color(0xFF94A3B8);
  static const interactive = Color(0xFF4F46E5);
  static const danger = Color(0xFFDC2626);
  static const success = Color(0xFF0F766E);

  // Web uses translucent indigo borders/glass surfaces extensively.
  static const border = Color(0x2699A2D5); // rgba(99,102,241,.15)
  static const borderStrong = Color(0x385B5FEA);
  static const glassWhite = Color(0xF2FFFFFF);
  static const glassWhiteSoft = Color(0xD9FFFFFF);

  // Reusable radii observed across the social UI.
  static const radiusSmall = 15.0;
  static const radiusCard = 22.0;
  static const radiusHero = 26.0;

  static const radiusSm = BorderRadius.all(Radius.circular(radiusSmall));
  static const radiusMd = BorderRadius.all(Radius.circular(radiusCard));
  static const radiusLg = BorderRadius.all(Radius.circular(radiusHero));
  static const radiusPill = BorderRadius.all(Radius.circular(999));

  // Soft multi-layer elevation translated from web box-shadows.
  static const cardShadow = <BoxShadow>[
    BoxShadow(
      color: Color(0x140F172A),
      blurRadius: 10,
      offset: Offset(0, 4),
    ),
    BoxShadow(
      color: Color(0x0F0F172A),
      blurRadius: 24,
      offset: Offset(0, 10),
    ),
  ];

  static const heroShadow = <BoxShadow>[
    BoxShadow(
      color: Color(0x5231195B),
      blurRadius: 30,
      offset: Offset(0, 14),
    ),
  ];

  static const subtleShadow = <BoxShadow>[
    BoxShadow(
      color: Color(0x160F172A),
      blurRadius: 12,
      offset: Offset(0, 5),
    ),
  ];

  static const brandGradient = LinearGradient(
    begin: Alignment.centerLeft,
    end: Alignment.centerRight,
    colors: <Color>[brandPurple, brandCyan, brandPink],
  );

  static const heroGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: <Color>[darkPurple, darkIndigo, Color(0xFF5D2CA8)],
  );

  static const authRadialPurple = RadialGradient(
    center: Alignment(-0.76, -0.84),
    radius: 1.15,
    colors: <Color>[Color(0x216D5DFC), Color(0x006D5DFC)],
    stops: <double>[0, 0.68],
  );

  static const authRadialCyan = RadialGradient(
    center: Alignment(0.82, 0.84),
    radius: 1.08,
    colors: <Color>[Color(0x1C17AECA), Color(0x0017AECA)],
    stops: <double>[0, 0.68],
  );
}
