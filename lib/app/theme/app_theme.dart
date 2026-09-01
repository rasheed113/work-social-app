import 'package:flutter/material.dart';
import '../../core/design/app_theme.dart' as design;

/// Compatibility facade for older feature imports.
/// New code must import `core/design/app_theme.dart` directly.
@Deprecated('Use core/design/app_theme.dart')
class AppTheme {
  const AppTheme._();
  static ThemeData light() => design.AppTheme.light();
}
