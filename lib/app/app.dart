import 'dart:async';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:supabase_flutter/supabase_flutter.dart';
import 'app_shell.dart';
import 'theme/app_theme.dart';
import '../features/auth/presentation/login_page.dart';
import '../features/chat/presentation/inbox_page.dart';

class WorkSocialApp extends StatefulWidget { const WorkSocialApp({super.key}); @override State<WorkSocialApp> createState()=>_WorkSocialAppState(); }
class _WorkSocialAppState extends State<WorkSocialApp>{late final GoRouter _router;@override void initState(){super.initState();final auth=Supabase.instance.client.auth;_router=GoRouter(initialLocation:'/home',refreshListenable:GoRouterRefreshStream(auth.onAuthStateChange),redirect:(_,s){final signed=auth.currentSession!=null;if(!signed&&s.matchedLocation!='/login')return'/login';if(signed&&s.matchedLocation=='/login')return'/home';return null;},routes:[GoRoute(path:'/login',builder:(_,__)=>const LoginPage()),GoRoute(path:'/home',builder:(_,__)=>const AppShell(initialIndex:0)),GoRoute(path:'/friends',builder:(_,__)=>const AppShell(initialIndex:1)),GoRoute(path:'/activity',builder:(_,__)=>const AppShell(initialIndex:2)),GoRoute(path:'/profile',builder:(_,__)=>const AppShell(initialIndex:3)),GoRoute(path:'/worker-house',builder:(_,__)=>const AppShell(initialIndex:4)),GoRoute(path:'/inbox',builder:(_,__)=>const InboxPage())]);} @override Widget build(BuildContext c)=>MaterialApp.router(title:'Work Social',debugShowCheckedModeBanner:false,theme:AppTheme.light(),routerConfig:_router);@override void dispose(){_router.dispose();super.dispose();}}
class GoRouterRefreshStream extends ChangeNotifier{GoRouterRefreshStream(Stream<AuthState> stream){_subscription=stream.listen((_)=>notifyListeners());}late final StreamSubscription<AuthState> _subscription;@override void dispose(){_subscription.cancel();super.dispose();}}
