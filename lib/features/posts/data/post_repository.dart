import 'dart:io';

import 'package:mime/mime.dart';
import 'package:supabase_flutter/supabase_flutter.dart';
import 'package:uuid/uuid.dart';

class PostDraft {
  const PostDraft({
    required this.content,
    this.latitude,
    this.longitude,
    this.locationName,
    this.files = const [],
  });

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
    final response = await client
        .from('posts')
        .select('id, profile_id, content, privacy, latitude, longitude, location_name, created_at, profiles(username, display_name, avatar_url)')
        .eq('privacy', 'public')
        .order('created_at', ascending: false);
    return List<Map<String, dynamic>>.from(response);
  }

  Future<void> createPost(PostDraft draft) async {
    final content = draft.content.trim();
    if (content.isEmpty && draft.files.isEmpty) {
      throw const PostValidationException('Post cannot be empty.');
    }

    final user = client.auth.currentUser;
    if (user == null) throw const PostValidationException('You must be signed in.');

    final postId = _uuid.v4();
    final uploaded = <String>[];

    try {
      await client.from('posts').insert({
        'id': postId,
        'profile_id': user.id,
        'content': content,
        'privacy': 'public',
        'latitude': draft.latitude,
        'longitude': draft.longitude,
        'location_name': draft.locationName,
      });

      for (final file in draft.files) {
        final original = file.uri.pathSegments.isEmpty ? 'upload' : file.uri.pathSegments.last;
        final path = '${user.id}/$postId/${_uuid.v4()}-$original';
        await client.storage.from(_bucket).upload(
              path,
              file,
              fileOptions: FileOptions(
                contentType: lookupMimeType(file.path),
                upsert: false,
              ),
            );
        uploaded.add(path);

        await client.from('post_attachments').insert({
          'post_id': postId,
          'storage_path': path,
          'file_name': original,
          'mime_type': lookupMimeType(file.path),
        });
      }
    } catch (_) {
      if (uploaded.isNotEmpty) {
        try {
          await client.storage.from(_bucket).remove(uploaded);
        } catch (_) {}
      }
      try {
        await client.from('posts').delete().eq('id', postId);
      } catch (_) {}
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
