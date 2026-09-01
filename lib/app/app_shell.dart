import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../features/home/presentation/home_page.dart';

class AppShell extends StatefulWidget {
  const AppShell({super.key, required this.initialIndex});
  final int initialIndex;
  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> {
  late int index;
  static const labels = ['Home', 'Friends', 'Activity', 'Profile', 'Worker House'];
  static const icons = [Icons.home_outlined, Icons.people_outline, Icons.notifications_none, Icons.person_outline, Icons.work_outline];

  @override
  void initState() { super.initState(); index = widget.initialIndex; }

  @override
  Widget build(BuildContext context) {
    final page = index == 0 ? const HomePage() : Center(child: Text(labels[index]));
    return Scaffold(
      body: SafeArea(child: page),
      bottomNavigationBar: NavigationBar(
        selectedIndex: index,
        onDestinationSelected: (value) {
          setState(() => index = value);
          context.go(['/home', '/friends', '/activity', '/profile', '/worker-house'][value]);
        },
        destinations: [for (var i = 0; i < labels.length; i++) NavigationDestination(icon: Icon(icons[i]), label: labels[i])],
      ),
    );
  }
}
