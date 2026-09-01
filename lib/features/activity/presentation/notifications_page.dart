import 'package:flutter/material.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

import '../../../core/design/app_tokens.dart';
import '../../../core/supabase/supabase_client.dart';
import '../data/notifications_repository.dart';

class NotificationsPage extends StatefulWidget {
  const NotificationsPage({super.key});

  @override
  State<NotificationsPage> createState() => _NotificationsPageState();
}

class _NotificationsPageState extends State<NotificationsPage> {
  final NotificationsRepository repo =
      NotificationsRepository(SupabaseClientProvider.client);

  List<Map<String, dynamic>> items = <Map<String, dynamic>>[];
  RealtimeChannel? channel;
  bool loading = true;
  String? error;

  @override
  void initState() {
    super.initState();
    _load();
    channel = repo.subscribe(_load);
  }

  @override
  void dispose() {
    final activeChannel = channel;
    if (activeChannel != null) {
      SupabaseClientProvider.client.removeChannel(activeChannel);
    }
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final result = await repo.load();
      if (mounted) {
        setState(() {
          items = result;
          error = null;
          loading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          error = e.toString();
          loading = false;
        });
      }
    }
  }

  String _label(String type) {
    const labels = <String, String>{
      'friend_request': 'sent you a friend request',
      'friend_accept': 'accepted your friend request',
      'like': 'liked your post',
      'comment': 'commented on your post',
      'comment_reply': 'replied to your comment',
      'follow': 'started following you',
      'message': 'sent you a message',
    };
    return labels[type] ?? 'sent you a notification';
  }

  @override
  Widget build(BuildContext context) {
    final unread = items.where((item) => item['is_read'] != true).length;

    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 18, 16, 110),
      children: [
        Container(
          padding: const EdgeInsets.all(22),
          decoration: BoxDecoration(
            borderRadius: AppTokens.radiusLg,
            gradient: AppTokens.heroGradient,
            boxShadow: AppTokens.heroShadow,
          ),
          child: Row(
            children: [
              const Expanded(
                child: Text(
                  'Activity',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 28,
                    fontWeight: FontWeight.w900,
                  ),
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 10,
                  vertical: 6,
                ),
                decoration: BoxDecoration(
                  color: Colors.white24,
                  borderRadius: AppTokens.radiusPill,
                ),
                child: Text(
                  unread > 0 ? '$unread unread' : 'All caught up',
                  style: const TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.w800,
                    fontSize: 12,
                  ),
                ),
              ),
            ],
          ),
        ),
        Align(
          alignment: Alignment.centerRight,
          child: TextButton(
            onPressed: unread == 0
                ? null
                : () async {
                    await repo.markAllRead();
                    await _load();
                  },
            child: const Text('Mark all read'),
          ),
        ),
        if (error != null)
          Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: Text(
              error!,
              style: const TextStyle(color: AppTokens.danger),
            ),
          ),
        if (loading)
          const Padding(
            padding: EdgeInsets.all(30),
            child: Center(child: CircularProgressIndicator()),
          ),
        if (!loading && items.isEmpty)
          const Padding(
            padding: EdgeInsets.all(30),
            child: Center(child: Text('🔔\nNo notifications yet.')),
          ),
        ...items.map(_item),
      ],
    );
  }

  Widget _item(Map<String, dynamic> notification) {
    final sender = notification['sender'] as Map<String, dynamic>?;
    final avatar = '${sender?['avatar_url'] ?? ''}';
    final read = notification['is_read'] == true;
    final created = DateTime.tryParse('${notification['created_at']}');
    final senderName =
        '${sender?['display_name'] ?? sender?['username'] ?? 'Someone'}';

    return InkWell(
      borderRadius: AppTokens.radiusMd,
      onTap: () async {
        await repo.markRead('${notification['id']}');
        await _load();
      },
      child: Container(
        margin: const EdgeInsets.only(bottom: 10),
        padding: const EdgeInsets.all(13),
        decoration: BoxDecoration(
          color: AppTokens.surface,
          borderRadius: AppTokens.radiusSm,
          border: Border.all(
            color: read ? AppTokens.border : AppTokens.borderStrong,
          ),
          boxShadow: AppTokens.subtleShadow,
        ),
        child: Row(
          children: [
            CircleAvatar(
              radius: 23,
              backgroundImage:
                  avatar.isNotEmpty ? NetworkImage(avatar) : null,
              child: avatar.isEmpty ? const Icon(Icons.person) : null,
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '$senderName ${_label('${notification['type']}')}',
                    style: const TextStyle(fontWeight: FontWeight.w700),
                  ),
                  if (created != null)
                    Text(
                      '${created.hour.toString().padLeft(2, '0')}:${created.minute.toString().padLeft(2, '0')}',
                      style: const TextStyle(
                        fontSize: 11,
                        color: AppTokens.textMuted,
                      ),
                    ),
                ],
              ),
            ),
            if (!read)
              const Icon(
                Icons.circle,
                size: 8,
                color: AppTokens.interactive,
              ),
          ],
        ),
      ),
    );
  }
}
