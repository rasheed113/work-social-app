import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'theme/app_theme.dart';
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
    final page = index == 0 ? const HomePage() : Center(child: Text(labels[index], style: Theme.of(context).textTheme.headlineSmall));
    return Scaffold(
      body: SafeArea(bottom: false, child: page),
      bottomNavigationBar: SafeArea(
        minimum: const EdgeInsets.fromLTRB(8, 0, 8, 8),
        child: Container(
          height: 56,
          padding: const EdgeInsets.all(5),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(18),
            border: Border.all(color: AppTheme.border),
            boxShadow: const [BoxShadow(blurRadius: 24, offset: Offset(0, 10), color: Color(0x3D0F172A))],
          ),
          child: Row(children: [for (var i = 0; i < labels.length; i++) Expanded(child: _Destination(index: i, selected: index == i, icon: icons[i], label: labels[i], onTap: () { setState(() => index = i); context.go(['/home', '/friends', '/activity', '/profile', '/worker-house'][i]); }))]),
        ),
      ),
    );
  }
}

class _Destination extends StatelessWidget {
  const _Destination({required this.index, required this.selected, required this.icon, required this.label, required this.onTap});
  final int index;
  final bool selected;
  final IconData icon;
  final String label;
  final VoidCallback onTap;
  @override
  Widget build(BuildContext context) => Semantics(button: true, selected: selected, label: label, child: InkWell(
    borderRadius: BorderRadius.circular(13),
    onTap: onTap,
    child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
      Icon(icon, size: 18, color: selected ? AppTheme.brand : AppTheme.textSecondary),
      const SizedBox(height: 1),
      Text(label, maxLines: 1, overflow: TextOverflow.ellipsis, style: TextStyle(fontSize: 8, fontWeight: FontWeight.w800, color: selected ? AppTheme.brand : AppTheme.textSecondary)),
    ]),
  ));
}
