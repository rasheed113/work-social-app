import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../features/home/presentation/home_page.dart';
import '../features/friends/presentation/friends_page.dart';
import '../features/activity/presentation/notifications_page.dart';

class AppShell extends StatefulWidget { const AppShell({super.key,required this.initialIndex}); final int initialIndex; @override State<AppShell> createState()=>_AppShellState(); }
class _AppShellState extends State<AppShell>{late int index;static const labels=['Home','Friends','Activity','Profile','Worker House'];static const icons=[Icons.home_outlined,Icons.people_outline,Icons.notifications_none,Icons.person_outline,Icons.work_outline];@override void initState(){super.initState();index=widget.initialIndex;}Widget _page(){if(index==0)return const HomePage();if(index==1)return const FriendsPage();if(index==2)return const NotificationsPage();return Center(child:Text(labels[index]));}@override Widget build(BuildContext context)=>Scaffold(body:SafeArea(child:_page()),bottomNavigationBar:NavigationBar(selectedIndex:index,onDestinationSelected:(value){setState(()=>index=value);context.go(['/home','/friends','/activity','/profile','/worker-house'][value]);},destinations:[for(var i=0;i<labels.length;i++)NavigationDestination(icon:Icon(icons[i]),label:labels[i])],));}
