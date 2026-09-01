import 'package:flutter_test/flutter_test.dart';

import 'package:work_social_app/app/theme/app_theme.dart';

void main() {
  test('Work Social theme exposes expected design tokens', () {
    final theme = AppTheme.light();
    expect(theme.useMaterial3, isTrue);
    expect(theme.scaffoldBackgroundColor, AppTheme.background);
  });
}
