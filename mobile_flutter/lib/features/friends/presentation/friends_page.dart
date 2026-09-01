import 'package:flutter/material.dart';
import '../../../core/supabase/supabase_client.dart';
import '../data/friends_repository.dart';

class FriendsPage extends StatefulWidget {
  const FriendsPage({super.key});
  @override State<FriendsPage> createState() => _FriendsPageState();
}
class _FriendsPageState extends State<FriendsPage> {
  final repo = FriendsRepository(SupabaseClientProvider.client);
  FriendsSnapshot? snapshot; String search = ''; String? error; bool loading = true;
  final pending = <String, String>{};
  @override void initState(){super.initState(); _load();}
  Future<void> _load() async { setState((){loading=true;error=null;}); try { final s=await repo.load(); pending..clear()..addEntries(s.outgoing.map((r)=>MapEntry(r['receiver_id'] as String,r['id'] as String))); setState(()=>snapshot=s); } catch(e){setState(()=>error=e.toString());} finally {if(mounted)setState(()=>loading=false);} }
  @override Widget build(BuildContext context){
    final s=snapshot; final people=(s?.people??[]).where((p){final q=search.trim().toLowerCase(); return q.isEmpty || (p['display_name']??'').toString().toLowerCase().contains(q);}).toList();
    return Scaffold(body: SafeArea(child: RefreshIndicator(onRefresh:_load,child: ListView(padding:const EdgeInsets.fromLTRB(16,18,16,110),children:[
      Container(padding:const EdgeInsets.all(22),decoration:BoxDecoration(borderRadius:BorderRadius.circular(26),gradient:const LinearGradient(colors:[Color(0xff171a3a),Color(0xff20265c),Color(0xff5d2ca8)]),boxShadow:const [BoxShadow(blurRadius:30,color:Color(0x5231195b),offset:Offset(0,14))]),child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[const Text('WORK SOCIAL',style:TextStyle(color:Color(0xff8de7ff),fontWeight:FontWeight.w900,fontSize:12,letterSpacing:2)),const SizedBox(height:4),const Text('Friends',style:TextStyle(color:Colors.white,fontSize:30,fontWeight:FontWeight.w900)),const SizedBox(height:8),const Text('Connect with people, manage requests and build your circle.',style:TextStyle(color:Color(0xb8ffffff),fontSize:14)),const SizedBox(height:18),TextField(onChanged:(v)=>setState(()=>search=v),style:const TextStyle(color:Colors.white),decoration:InputDecoration(hintText:'Search people...',hintStyle:const TextStyle(color:Color(0xb8ffffff)),prefixIcon:const Icon(Icons.search,color:Colors.white),filled:true,fillColor:Color(0x1affffff),border:OutlineInputBorder(borderRadius:BorderRadius.all(Radius.circular(17)),borderSide:BorderSide.none))) ])),
      if(error!=null) Padding(padding:const EdgeInsets.only(top:12),child:Text(error!,style:const TextStyle(color:Color(0xffb4233c))),
      if(loading) const Padding(padding:EdgeInsets.all(30),child:Center(child:CircularProgressIndicator())),
      if(!loading && (s?.incoming.isNotEmpty??false)) _section('🤝 Friend Requests',s!.incoming.map((r)=>_request(r)).toList()),
      if(!loading) _section(search.trim().isEmpty?'✨ People':'🔎 Search results',people.map((p)=>_person(p,s!)).toList()),
    ]))));
  }
  Widget _section(String title,List<Widget> children)=>Container(margin:const EdgeInsets.only(top:16),padding:const EdgeInsets.all(16),decoration:BoxDecoration(borderRadius:BorderRadius.circular(22),color:Colors.white,border:Border.all(color:const Color(0x1f6366f1)),boxShadow:const [BoxShadow(blurRadius:24,color:Color(0x1f18193c),offset:Offset(0,10))]),child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[Text(title,style:const TextStyle(fontSize:17,fontWeight:FontWeight.w800)),...children]);
  Widget _avatar(Map p)=>CircleAvatar(radius:24,backgroundImage:(p['avatar_url'] as String?)?.isNotEmpty==true?NetworkImage(p['avatar_url']):null,child:(p['avatar_url'] as String?)?.isNotEmpty==true?null:const Text('👤'));
  Widget _person(Map p,FriendsSnapshot s){final id=p['id'] as String; final friend=s.friendIds.contains(id); final following=s.followingIds.contains(id); final req=pending[id]; return Container(margin:const EdgeInsets.only(top:8),padding:const EdgeInsets.all(11),decoration:BoxDecoration(borderRadius:BorderRadius.circular(17),color:Colors.white,border:Border.all(color:const Color(0x14203a)),boxShadow:const [BoxShadow(blurRadius:12,color:Color(0x12203a),offset:Offset(0,5))]),child:Column(children:[Row(children:[_avatar(p),const SizedBox(width:11),Expanded(child:Text(p['display_name']??'User',style:const TextStyle(fontSize:15,fontWeight:FontWeight.w700))),]),const SizedBox(height:8),Row(mainAxisAlignment:MainAxisAlignment.end,children:[TextButton(onPressed:()=>_follow(id,following),child:Text(following?'Following':'Follow')),const SizedBox(width:6),ElevatedButton(onPressed:friend?null:()=>req==null?_send(id):_cancel(id,req),child:Text(friend?'✓ Friends':req==null?'Add friend':'Cancel'))]) ]));}
  Widget _request(Map r){final sender=(snapshot?.people??[]).where((p)=>p['id']==r['sender_id']).firstOrNull; return Container(margin:const EdgeInsets.only(top:8),child:Row(children:[_avatar(sender??{}),const SizedBox(width:10),Expanded(child:Text(sender?['display_name']??'User',style:const TextStyle(fontWeight:FontWeight.w700))),TextButton(onPressed:()=>_respond(r,true),child:const Text('Accept')),TextButton(onPressed:()=>_respond(r,false),child:const Text('Reject'))]));}
  Future<void> _send(String id)async{try{final rid=await repo.sendRequest(id);pending[id]=rid;setState((){});}catch(e){setState(()=>error=e.toString());}}
  Future<void> _cancel(String id,String rid)async{try{await repo.cancelRequest(rid,id);pending.remove(id);setState((){});}catch(e){setState(()=>error=e.toString());}}
  Future<void> _respond(Map r,bool accept)async{try{await repo.respond(r['id'],r['sender_id'],accept);await _load();}catch(e){setState(()=>error=e.toString());}}
  Future<void> _follow(String id,bool following)async{try{await repo.toggleFollow(id,following);await _load();}catch(e){setState(()=>error=e.toString());}}
}
