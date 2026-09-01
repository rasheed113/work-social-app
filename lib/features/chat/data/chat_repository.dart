import 'dart:convert';

import 'package:supabase_flutter/supabase_flutter.dart';

class ChatRepository {
  ChatRepository(this.client);

  final SupabaseClient client;

  Future<Map<String, dynamic>> load() async {
    final uid = client.auth.currentUser?.id;
    if (uid == null) {
      throw const AuthException('You must be signed in.');
    }

    final mine = await client
        .from('conversation_members')
        .select('conversation_id,profile_id,last_read_at')
        .eq('profile_id', uid);
    final ids = List<Map<String, dynamic>>.from(mine)
        .map((row) => row['conversation_id'] as String)
        .toList();

    if (ids.isEmpty) {
      return <String, dynamic>{
        'conversations': <Map<String, dynamic>>[],
        'members': <Map<String, dynamic>>[],
        'messages': <Map<String, dynamic>>[],
        'profiles': <Map<String, dynamic>>[],
        'media': <String, String>{},
      };
    }

    final results = await Future.wait<dynamic>([
      client
          .from('conversations')
          .select('id,kind,title,avatar_url,created_by,updated_at')
          .inFilter('id', ids)
          .order('updated_at', ascending: false),
      client
          .from('conversation_members')
          .select('conversation_id,profile_id,last_read_at')
          .inFilter('conversation_id', ids),
      client
          .from('messages')
          .select(
            'id,conversation_id,sender_id,content,message_type,media_path,'
            'media_mime,media_name,media_size,media_duration_ms,created_at,'
            'read_at,reply_to_message_id,edited_at,deleted_at',
          )
          .inFilter('conversation_id', ids)
          .order('created_at', ascending: true),
    ]);

    final messages = List<Map<String, dynamic>>.from(results[2] as List);
    final members = List<Map<String, dynamic>>.from(results[1] as List);
    final profileIds = <String>{
      ...members.map((row) => row['profile_id'] as String),
      ...messages.map((row) => row['sender_id'] as String),
    }.toList();

    final profiles = profileIds.isEmpty
        ? <Map<String, dynamic>>[]
        : List<Map<String, dynamic>>.from(
            await client
                .from('profiles')
                .select('id,display_name,username,avatar_url')
                .inFilter('id', profileIds),
          );

    final paths = <String>[];
    for (final message in messages) {
      final legacy = parseLegacyMedia(message['content'] as String?);
      final path = message['media_path'] as String? ?? legacy?.path;
      if (path != null && path.isNotEmpty) {
        paths.add(path);
      }
    }

    final uniquePaths = paths.toSet().toList();
    final media = <String, String>{};
    if (uniquePaths.isNotEmpty) {
      final signedUrls = await client.storage
          .from('chat-media')
          .createSignedUrls(uniquePaths, 3600);
      for (var i = 0; i < signedUrls.length && i < uniquePaths.length; i++) {
        final signedUrl = signedUrls[i].signedUrl;
        if (signedUrl != null && signedUrl.isNotEmpty) {
          media[uniquePaths[i]] = signedUrl;
        }
      }
    }

    return <String, dynamic>{
      'conversations': List<Map<String, dynamic>>.from(results[0] as List),
      'members': members,
      'messages': messages,
      'profiles': profiles,
      'media': media,
    };
  }

  static LegacyMedia? parseLegacyMedia(String? content) {
    const prefix = '__work_social_media__:';
    if (content == null || !content.startsWith(prefix)) {
      return null;
    }

    try {
      final decoded = jsonDecode(content.substring(prefix.length));
      if (decoded is! Map) return null;
      final payload = Map<String, dynamic>.from(decoded);
      if (payload['path'] is String && payload['mime'] is String) {
        return LegacyMedia(
          path: payload['path'] as String,
          mime: payload['mime'] as String,
          name: (payload['name'] ?? 'Media').toString(),
          size: payload['size'] is num ? payload['size'] as num : 0,
        );
      }
    } catch (_) {
      return null;
    }
    return null;
  }

  Future<void> sendText(String conversationId, String text) async {
    final trimmed = text.trim();
    if (trimmed.isEmpty) return;

    await client.from('messages').insert(<String, dynamic>{
      'conversation_id': conversationId,
      'content': trimmed,
      'message_type': 'text',
    });
  }
}

class LegacyMedia {
  const LegacyMedia({
    required this.path,
    required this.mime,
    required this.name,
    required this.size,
  });

  final String path;
  final String mime;
  final String name;
  final num size;
}
