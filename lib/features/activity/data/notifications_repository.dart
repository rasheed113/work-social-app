import 'package:supabase_flutter/supabase_flutter.dart';

class NotificationsRepository {
  NotificationsRepository(this.client);

  final SupabaseClient client;

  Future<List<Map<String, dynamic>>> load() async {
    final user = client.auth.currentUser;
    if (user == null) {
      throw const AuthException('You must be signed in.');
    }

    final rows = await client
        .from('notifications')
        .select(
          'id,receiver_id,sender_id,type,post_id,comment_id,is_read,created_at,metadata',
        )
        .eq('receiver_id', user.id)
        .order('created_at', ascending: false)
        .limit(100);

    final items = List<Map<String, dynamic>>.from(rows);
    final senderIds = items
        .map((item) => item['sender_id'])
        .whereType<String>()
        .toSet()
        .toList();

    if (senderIds.isEmpty) {
      return items;
    }

    final profiles = await client
        .from('profiles')
        .select('id,display_name,username,avatar_url')
        .inFilter('id', senderIds);

    final profileMap = <String, Map<String, dynamic>>{
      for (final profile in profiles)
        profile['id'] as String: Map<String, dynamic>.from(profile),
    };

    return items
        .map(
          (item) => <String, dynamic>{
            ...item,
            'sender': profileMap[item['sender_id']],
          },
        )
        .toList();
  }

  Future<void> markRead(String id) {
    return client.from('notifications').update({'is_read': true}).eq('id', id);
  }

  Future<void> markAllRead() async {
    final userId = client.auth.currentUser?.id;
    if (userId == null) return;

    await client
        .from('notifications')
        .update({'is_read': true})
        .eq('receiver_id', userId)
        .eq('is_read', false);
  }

  RealtimeChannel subscribe(void Function() onChange) {
    final userId = client.auth.currentUser!.id;

    return client
        .channel('notifications:$userId')
        .onPostgresChanges(
          event: PostgresChangeEvent.insert,
          schema: 'public',
          table: 'notifications',
          callback: (_) => onChange(),
        )
        .onPostgresChanges(
          event: PostgresChangeEvent.update,
          schema: 'public',
          table: 'notifications',
          callback: (_) => onChange(),
        )
      ..subscribe();
  }
}
