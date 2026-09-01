import 'package:supabase_flutter/supabase_flutter.dart';

class FriendsSnapshot {
  const FriendsSnapshot({required this.people, required this.incoming, required this.outgoing, required this.friendIds, required this.followingIds});
  final List<Map<String, dynamic>> people;
  final List<Map<String, dynamic>> incoming;
  final List<Map<String, dynamic>> outgoing;
  final Set<String> friendIds;
  final Set<String> followingIds;
}

class FriendsRepository {
  FriendsRepository(this.client);
  final SupabaseClient client;

  Future<FriendsSnapshot> load() async {
    final user = client.auth.currentUser;
    if (user == null) throw const AuthException('You must be signed in.');
    final id = user.id;
    final results = await Future.wait([
      client.from('profiles').select('id, display_name, avatar_url').neq('id', id).order('display_name'),
      client.from('friend_requests').select('id, sender_id, status, created_at').eq('receiver_id', id).eq('status', 'pending').order('created_at', ascending: false),
      client.from('friend_requests').select('id, receiver_id').eq('sender_id', id).eq('status', 'pending'),
      client.from('friends').select('profile_a_id, profile_b_id').or('profile_a_id.eq.$id,profile_b_id.eq.$id'),
      client.from('follows').select('following_id').eq('follower_id', id),
    ]);
    final friendships = List<Map<String, dynamic>>.from(results[3] as List);
    return FriendsSnapshot(
      people: List<Map<String, dynamic>>.from(results[0] as List),
      incoming: List<Map<String, dynamic>>.from(results[1] as List),
      outgoing: List<Map<String, dynamic>>.from(results[2] as List),
      friendIds: friendships.map((f) => f['profile_a_id'] == id ? f['profile_b_id'] as String : f['profile_a_id'] as String).toSet(),
      followingIds: List<Map<String, dynamic>>.from(results[4] as List).map((f) => f['following_id'] as String).toSet(),
    );
  }

  Future<String> sendRequest(String receiverId) async {
    final id = client.auth.currentUser!.id;
    final row = await client.from('friend_requests').insert({'sender_id': id, 'receiver_id': receiverId, 'status': 'pending'}).select('id').single();
    return row['id'] as String;
  }

  Future<void> cancelRequest(String requestId, String receiverId) async {
    await client.from('friend_requests').delete().eq('id', requestId).eq('sender_id', client.auth.currentUser!.id).eq('receiver_id', receiverId).eq('status', 'pending');
  }

  Future<void> respond(String requestId, String senderId, bool accept) async {
    final me = client.auth.currentUser!.id;
    await client.from('friend_requests').update({'status': accept ? 'accepted' : 'rejected'}).eq('id', requestId).eq('receiver_id', me);
    if (accept) {
      final pair = [me, senderId]..sort();
      await client.from('friends').insert({'profile_a_id': pair[0], 'profile_b_id': pair[1]});
    }
  }

  Future<void> toggleFollow(String targetId, bool following) async {
    final me = client.auth.currentUser!.id;
    if (following) {
      await client.from('follows').delete().eq('follower_id', me).eq('following_id', targetId);
    } else {
      await client.from('follows').insert({'follower_id': me, 'following_id': targetId});
    }
  }
}
