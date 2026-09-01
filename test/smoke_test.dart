import 'package:flutter_test/flutter_test.dart';

import 'package:work_social_app/core/design/app_theme.dart';
import 'package:work_social_app/core/design/app_tokens.dart';

void main() {
  test('Work Social theme exposes centralized design tokens', () {
    final theme = AppTheme.light();

    expect(theme.useMaterial3, isTrue);
    expect(theme.scaffoldBackgroundColor, AppTokens.pageBackground);
    expect(AppTokens.radiusCard, 22.0);
    expect(AppTokens.radiusHero, 26.0);
  });
}
