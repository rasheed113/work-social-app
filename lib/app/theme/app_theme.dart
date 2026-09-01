import 'package:flutter/material.dart';

class AppTheme {
  const AppTheme._();
  static const background = Color(0xFFF7F8FC);
  static const surface = Color(0xFFFFFFFF);
  static const textPrimary = Color(0xFF17202A);
  static const textSecondary = Color(0xFF64748B);
  static const border = Color(0x266366F1);
  static const brand = Color(0xFF6D5DFC);
  static const cyan = Color(0xFF22C1DC);
  static const blue = Color(0xFF3B82F6);
  static const danger = Color(0xFFB4232D);
  static const brandGradient = LinearGradient(colors: [Color(0xFF6D5DFC), Color(0xFF3B82F6), Color(0xFF22C1DC)], begin: Alignment.topLeft, end: Alignment.bottomRight);

  static ThemeData light() {
    final scheme = ColorScheme.fromSeed(seedColor: brand, brightness: Brightness.light);
    return ThemeData(
      useMaterial3: true,
      colorScheme: scheme.copyWith(primary: brand, secondary: cyan, surface: surface),
      scaffoldBackgroundColor: background,
      fontFamily: 'Roboto',
      appBarTheme: const AppBarTheme(backgroundColor: Colors.transparent, foregroundColor: textPrimary, elevation: 0, surfaceTintColor: Colors.transparent),
      cardTheme: const CardThemeData(color: surface, elevation: 0, margin: EdgeInsets.zero, shadowColor: Color(0x260F172A), shape: RoundedRectangleBorder(borderRadius: BorderRadius.all(Radius.circular(22)), side: BorderSide(color: border))),
      inputDecorationTheme: InputDecorationTheme(
        filled: true, fillColor: surface, contentPadding: const EdgeInsets.symmetric(horizontal: 15, vertical: 13),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(15), borderSide: const BorderSide(color: border)),
        enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(15), borderSide: const BorderSide(color: border)),
        focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(15), borderSide: const BorderSide(color: brand, width: 1.4)),
      ),
    );
  }
}
