import 'dart:io';
import 'package:mime/mime.dart';
import 'package:supabase_flutter/supabase_flutter.dart';
import 'package:uuid/uuid.dart';

class PostDraft {
  const PostDraft({required this.content, this.latitude, this.longitude, this.locationName, this.files = const []});
  final String content;
  final double? latitude;
  final double? longitude;
  final String? locationName;
  final List<File> files;
}

class PostRepository {
  PostRepository(this.client);
  final SupabaseClient client;
  static const _bucket = 'post-media';
  static const _uuid = Uuid();

  Future<List<Map<String, dynamic>>> fetchPublicPosts() async {
    final posts = await client.from('posts').select('id, profile_id, content, privacy, latitude, longitude, location_name, created_at, profiles(username, display_name, avatar_url)').eq('privacy', 'public').order('created_at', ascending: false);
    final rows = List<Map<String, dynamic>>.from(posts);
    if (rows.isEmpty) return rows;
    final attachments = await client.from('post_attachments').select('id, post_id, kind, storage_path, file_name, mime_type, file_size').inFilter('post_id', rows.map((p) => p['id']).toList()).order('created_at', ascending: true);
    final grouped = <String, List<Map<String, dynamic>>>{};
    for (final raw in List<Map<String, dynamic>>.from(attachments)) {
      final row = Map<String, dynamic>.from(raw);
      final url = client.storage.from(_bucket).getPublicUrl(row['storage_path'] as String);
      row['public_url'] = url;
      grouped.putIfAbsent(row['post_id'] as String, () => []).add(row);
    }
    return rows.map((post) => {...post, 'attachments': grouped[post['id']] ?? const []}).toList();
  }

  Future<void> createPost(PostDraft draft) async {
    final content = draft.content.trim();
    if (content.isEmpty && draft.files.isEmpty && draft.latitude == null && draft.longitude == null) throw const PostValidationException('Post cannot be empty.');
    final user = client.auth.currentUser;
    if (user == null) throw const PostValidationException('You must be signed in.');
    final postId = _uuid.v4();
    final uploaded = <String>[];
    try {
      await client.from('posts').insert({'id': postId, 'profile_id': user.id, 'content': content, 'latitude': draft.latitude, 'longitude': draft.longitude, 'location_name': draft.locationName});
      for (final file in draft.files) {
        final name = file.uri.pathSegments.isEmpty ? 'upload' : file.uri.pathSegments.last;
        final safeName = name.replaceAll(RegExp(r'[^a-zA-Z0-9._-]'), '_');
        final mime = lookupMimeType(file.path);
        final kind = mime?.startsWith('image/') == true ? 'image' : mime?.startsWith('video/') == true ? 'video' : 'file';
        final path = '${user.id}/$postId/${_uuid.v4()}-$safeName';
        await client.storage.from(_bucket).upload(path, file, fileOptions: FileOptions(contentType: mime, upsert: false));
        uploaded.add(path);
        await client.from('post_attachments').insert({'post_id': postId, 'profile_id': user.id, 'kind': kind, 'storage_path': path, 'file_name': name, 'mime_type': mime, 'file_size': await file.length()});
      }
    } catch (_) {
      if (uploaded.isNotEmpty) { try { await client.storage.from(_bucket).remove(uploaded); } catch (_) {} }
      try { await client.from('posts').delete().eq('id', postId); } catch (_) {}
      rethrow;
    }
  }
}

class PostValidationException implements Exception {
  const PostValidationException(this.message);
  final String message;
  @override
  String toString() => message;
}
