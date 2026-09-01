import 'package:flutter/material.dart';
import 'app_tokens.dart';

class AppTheme {
  const AppTheme._();

  static ThemeData light() {
    final scheme = ColorScheme.light(
      primary: AppTokens.brandPurple,
      onPrimary: Colors.white,
      secondary: AppTokens.brandCyan,
      onSecondary: Colors.white,
      tertiary: AppTokens.brandPink,
      surface: AppTokens.surface,
      onSurface: AppTokens.textPrimary,
      error: AppTokens.danger,
      onError: Colors.white,
      outline: AppTokens.border,
    );

    return ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      scaffoldBackgroundColor: AppTokens.pageBackground,
      fontFamily: 'Roboto',
      splashFactory: InkSparkle.splashFactory,
      visualDensity: VisualDensity.standard,
      appBarTheme: const AppBarTheme(
        backgroundColor: Colors.transparent,
        foregroundColor: AppTokens.textPrimary,
        elevation: 0,
        scrolledUnderElevation: 0,
        surfaceTintColor: Colors.transparent,
      ),
      cardTheme: const CardThemeData(
        color: AppTokens.surface,
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: AppTokens.radiusMd,
          side: BorderSide(color: AppTokens.border),
        ),
      ),
      inputDecorationTheme: const InputDecorationTheme(
        filled: true,
        fillColor: AppTokens.glassWhite,
        contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        border: OutlineInputBorder(
          borderRadius: AppTokens.radiusMd,
          borderSide: BorderSide(color: AppTokens.border),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: AppTokens.radiusMd,
          borderSide: BorderSide(color: AppTokens.border),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: AppTokens.radiusMd,
          borderSide: BorderSide(color: AppTokens.brandPurple, width: 1.5),
        ),
        hintStyle: TextStyle(color: AppTokens.textMuted),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          elevation: 0,
          foregroundColor: Colors.white,
          backgroundColor: AppTokens.brandPurple,
          shape: const RoundedRectangleBorder(borderRadius: AppTokens.radiusSm),
          padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 13),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: AppTokens.interactive,
          shape: const RoundedRectangleBorder(borderRadius: AppTokens.radiusSm),
        ),
      ),
      chipTheme: ChipThemeData(
        backgroundColor: AppTokens.surfaceSoft,
        selectedColor: AppTokens.brandPurple.withValues(alpha: .12),
        side: const BorderSide(color: AppTokens.border),
        shape: const RoundedRectangleBorder(borderRadius: AppTokens.radiusPill),
        labelStyle: const TextStyle(color: AppTokens.textPrimary, fontWeight: FontWeight.w600),
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: AppTokens.glassWhite,
        elevation: 0,
        indicatorColor: AppTokens.brandPurple,
        indicatorShape: const RoundedRectangleBorder(borderRadius: AppTokens.radiusSm),
        labelTextStyle: WidgetStatePropertyAll(
          const TextStyle(fontSize: 11, fontWeight: FontWeight.w700),
        ),
      ),
    );
  }
}
