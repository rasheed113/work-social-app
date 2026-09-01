import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../core/design/app_tokens.dart';
import '../features/activity/presentation/notifications_page.dart';
import '../features/friends/presentation/friends_page.dart';
import '../features/home/presentation/home_page.dart';
import '../features/profile/presentation/profile_page.dart';

class AppShell extends StatefulWidget {
  const AppShell({super.key, required this.initialIndex});

  final int initialIndex;

  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> {
  static const labels = <String>[
    'Home',
    'Friends',
    'Activity',
    'Profile',
    'Worker House',
  ];

  static const icons = <IconData>[
    Icons.home_outlined,
    Icons.people_outline,
    Icons.notifications_none,
    Icons.person_outline,
    Icons.work_outline,
  ];

  late int index;

  @override
  void initState() {
    super.initState();
    index = widget.initialIndex.clamp(0, labels.length - 1);
  }

  Widget _page() {
    switch (index) {
      case 0:
        return const HomePage();
      case 1:
        return const FriendsPage();
      case 2:
        return const NotificationsPage();
      case 3:
        return const ProfilePage();
      default:
        return Center(
          child: Text(
            labels[index],
            style: Theme.of(context).textTheme.headlineSmall,
          ),
        );
    }
  }

  void _select(int nextIndex) {
    setState(() => index = nextIndex);
    context.go(
      <String>['/home', '/friends', '/activity', '/profile', '/worker-house'][
        nextIndex
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(bottom: false, child: _page()),
      bottomNavigationBar: SafeArea(
        minimum: const EdgeInsets.fromLTRB(8, 0, 8, 8),
        child: Container(
          height: 56,
          padding: const EdgeInsets.all(5),
          decoration: BoxDecoration(
            color: AppTokens.surface,
            borderRadius: AppTokens.radiusMd,
            border: Border.all(color: AppTokens.border),
            boxShadow: AppTokens.cardShadow,
          ),
          child: Row(
            children: [
              for (var i = 0; i < labels.length; i++)
                Expanded(
                  child: _Destination(
                    selected: index == i,
                    icon: icons[i],
                    label: labels[i],
                    onTap: () => _select(i),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

class _Destination extends StatelessWidget {
  const _Destination({
    required this.selected,
    required this.icon,
    required this.label,
    required this.onTap,
  });

  final bool selected;
  final IconData icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final foreground = selected ? Colors.white : AppTokens.textSecondary;

    return InkWell(
      borderRadius: AppTokens.radiusSm,
      onTap: onTap,
      child: Container(
        margin: const EdgeInsets.symmetric(horizontal: 2),
        decoration: selected
            ? BoxDecoration(
                gradient: AppTokens.brandGradient,
                borderRadius: AppTokens.radiusSm,
              )
            : null,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, size: 18, color: foreground),
            const SizedBox(height: 1),
            Text(
              label,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                fontSize: 8,
                fontWeight: FontWeight.w800,
                color: foreground,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
