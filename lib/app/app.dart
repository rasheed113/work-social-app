import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

import 'theme/app_theme.dart';
import '../features/auth/presentation/login_page.dart';
import '../features/home/presentation/home_page.dart';

class WorkSocialApp extends StatefulWidget {
  const WorkSocialApp({super.key});

  @override
  State<WorkSocialApp> createState() => _WorkSocialAppState();
}

class _WorkSocialAppState extends State<WorkSocialApp> {
  late final GoRouter _router;

  @override
  void initState() {
    super.initState();
    final auth = Supabase.instance.client.auth;
    _router = GoRouter(
      initialLocation: '/home',
      refreshListenable: GoRouterRefreshStream(auth.onAuthStateChange),
      redirect: (context, state) {
        final signedIn = auth.currentSession != null;
        final onLogin = state.matchedLocation == '/login';
        if (!signedIn && !onLogin) return '/login';
        if (signedIn && onLogin) return '/home';
        return null;
      },
      routes: [
        GoRoute(path: '/login', builder: (_, __) => const LoginPage()),
        GoRoute(path: '/home', builder: (_, __) => const AppShell(initialIndex: 0)),
        GoRoute(path: '/friends', builder: (_, __) => const AppShell(initialIndex: 1)),
        GoRoute(path: '/activity', builder: (_, __) => const AppShell(initialIndex: 2)),
        GoRoute(path: '/profile', builder: (_, __) => const AppShell(initialIndex: 3)),
        GoRoute(path: '/worker-house', builder: (_, __) => const AppShell(initialIndex: 4)),
      ],
    );
  }

  @override
  Widget build(BuildContext context) => MaterialApp.router(
        title: 'Work Social',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.light(),
        routerConfig: _router,
      );
}

class GoRouterRefreshStream extends ChangeNotifier {
  GoRouterRefreshStream(Stream<AuthState> stream) {
    _subscription = stream.listen((_) => notifyListeners());
  }
  late final StreamSubscription<AuthState> _subscription;

  @override
  void dispose() {
    _subscription.cancel();
    super.dispose();
  }
}

class AppShell extends StatefulWidget {
  const AppShell({super.key, required this.initialIndex});
  final int initialIndex;

  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> {
  late int _index;

  static const labels = ['Home', 'Friends', 'Activity', 'Profile', 'Worker House'];
  static const icons = [
    Icons.home_outlined,
    Icons.people_outline,
    Icons.notifications_none,
    Icons.person_outline,
    Icons.work_outline,
  ];

  @override
  void initState() {
    super.initState();
    _index = widget.initialIndex;
  }

  void _select(int index) {
    setState(() => _index = index);
    final paths = ['/home', '/friends', '/activity', '/profile', '/worker-house'];
    context.go(paths[index]);
  }

  @override
  Widget build(BuildContext context) {
    final page = switch (_index) {
      0 => const HomePage(),
      _ => PlaceholderPage(title: labels[_index]),
    };
    return Scaffold(
      body: SafeArea(child: page),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: _select,
        destinations: [
          for (var i = 0; i < labels.length; i++)
            NavigationDestination(icon: Icon(icons[i]), label: labels[i]),
        ],
      ),
    );
  }
}

class PlaceholderPage extends StatelessWidget {
  const PlaceholderPage({super.key, required this.title});
  final String title;
  @override
  Widget build(BuildContext context) => Center(
        child: Text(title, style: Theme.of(context).textTheme.headlineSmall),
      );
}
