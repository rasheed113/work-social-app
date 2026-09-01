import 'dart:async';
import 'package:supabase_flutter/supabase_flutter.dart';

class NotificationsRepository {
  NotificationsRepository(this.client);
  final SupabaseClient client;

  Future<List<Map<String, dynamic>>> load() async {
    final user = client.auth.currentUser;
    if (user == null) throw const AuthException('You must be signed in.');
    final rows = await client.from('notifications').select('id, receiver_id, sender_id, type, post_id, comment_id, is_read, created_at, metadata').eq('receiver_id', user.id).order('created_at', ascending: false).limit(100);
    final items = List<Map<String,dynamic>>.from(rows);
    final ids = items.map((e)=>e['sender_id']).whereType<String>().toSet().toList();
    if(ids.isEmpty)return items;
    final profiles = await client.from('profiles').select('id, display_name, username, avatar_url').inFilter('id', ids);
    final map = {for(final p in profiles) p['id'] as String: Map<String,dynamic>.from(p)};
    return items.map((e)=>{...e,'sender':map[e['sender_id']]}).toList();
  }
  Stream<List<Map<String,dynamic>>> watch() async* { yield* Stream.periodic(const Duration(seconds: 1)).asyncMap((_)=>load()); }
  Future<void> markRead(String id) async { await client.from('notifications').update({'is_read':true}).eq('id',id); }
  Future<void> markAllRead() async { final id=client.auth.currentUser?.id; if(id==null)return; await client.from('notifications').update({'is_read':true}).eq('receiver_id',id).eq('is_read',false); }
  RealtimeChannel subscribe(void Function() onChange) { final id=client.auth.currentUser!.id; return client.channel('notifications:$id').onPostgresChanges(event: PostgresChangeEvent.insert,schema:'public',table:'notifications',filter:PostgresChangeFilter(type:FilterType.eq,column:'receiver_id',value:id),callback:(_)=>onChange()).onPostgresChanges(event: PostgresChangeEvent.update,schema:'public',table:'notifications',filter:PostgresChangeFilter(type:FilterType.eq,column:'receiver_id',value:id),callback:(_)=>onChange())..subscribe(); }
}
