import 'package:flutter/material.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

import '../../../core/design/app_tokens.dart';
import '../../../core/supabase/supabase_client.dart';
import '../data/chat_repository.dart';

class InboxPage extends StatefulWidget {
  const InboxPage({super.key});

  @override
  State<InboxPage> createState() => _InboxPageState();
}

class _InboxPageState extends State<InboxPage> {
  final ChatRepository repo = ChatRepository(SupabaseClientProvider.client);
  final TextEditingController controller = TextEditingController();

  Map<String, dynamic>? data;
  String? selected;
  String search = '';
  RealtimeChannel? channel;

  @override
  void initState() {
    super.initState();
    _load();
    channel = SupabaseClientProvider.client
        .channel('work-social-chat-ui')
        .onPostgresChanges(
          event: PostgresChangeEvent.insert,
          schema: 'public',
          table: 'messages',
          callback: (_) => _load(),
        )
        .onPostgresChanges(
          event: PostgresChangeEvent.update,
          schema: 'public',
          table: 'messages',
          callback: (_) => _load(),
        )
      ..subscribe();
  }

  Future<void> _load() async {
    try {
      final result = await repo.load();
      if (mounted) {
        setState(() => data = result);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.toString())),
        );
      }
    }
  }

  @override
  void dispose() {
    final activeChannel = channel;
    if (activeChannel != null) {
      SupabaseClientProvider.client.removeChannel(activeChannel);
    }
    controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final conversations = List<Map<String, dynamic>>.from(
      data?['conversations'] ?? const <Map<String, dynamic>>[],
    );
    final messages = List<Map<String, dynamic>>.from(
      data?['messages'] ?? const <Map<String, dynamic>>[],
    );
    final profiles = List<Map<String, dynamic>>.from(
      data?['profiles'] ?? const <Map<String, dynamic>>[],
    );
    final urls = Map<String, String>.from(data?['media'] ?? const <String, String>{});
    final query = search.trim().toLowerCase();
    final filtered = conversations.where((conversation) {
      if (query.isEmpty) return true;
      return '${conversation['title'] ?? ''}'.toLowerCase().contains(query);
    }).toList();

    return Scaffold(
      backgroundColor: AppTokens.pageBackground,
      body: SafeArea(
        child: Row(
          children: [
            SizedBox(
              width: 180,
              child: Column(
                children: [
                  const Padding(
                    padding: EdgeInsets.all(16),
                    child: Text(
                      'Inbox',
                      style: TextStyle(
                        fontSize: 22,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 10),
                    child: TextField(
                      onChanged: (value) => setState(() => search = value),
                      decoration: const InputDecoration(
                        prefixIcon: Icon(Icons.search),
                        hintText: 'Search',
                      ),
                    ),
                  ),
                  Expanded(
                    child: ListView(
                      children: [
                        for (final conversation in filtered)
                          ListTile(
                            selected: conversation['id'] == selected,
                            title: Text(
                              conversation['title'] ?? 'Conversation',
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                            ),
                            onTap: () => setState(
                              () => selected = conversation['id'] as String,
                            ),
                          ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            Expanded(
              child: selected == null
                  ? const Center(child: Text('Select a conversation'))
                  : _Chat(
                      messages: messages
                          .where((message) =>
                              message['conversation_id'] == selected)
                          .toList(),
                      profiles: profiles,
                      urls: urls,
                      controller: controller,
                      onSend: () async {
                        final text = controller.text.trim();
                        if (text.isEmpty) return;
                        await repo.sendText(selected!, text);
                        controller.clear();
                        await _load();
                      },
                    ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Chat extends StatelessWidget {
  const _Chat({
    required this.messages,
    required this.profiles,
    required this.urls,
    required this.controller,
    required this.onSend,
  });

  final List<Map<String, dynamic>> messages;
  final List<Map<String, dynamic>> profiles;
  final Map<String, String> urls;
  final TextEditingController controller;
  final Future<void> Function() onSend;

  String _name(String id) {
    for (final profile in profiles) {
      if (profile['id'] == id) {
        return (profile['display_name'] ?? profile['username'] ?? 'User')
            .toString();
      }
    }
    return 'User';
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Expanded(
          child: ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: messages.length,
            itemBuilder: (context, index) {
              final message = messages[index];
              final content = (message['content'] ?? '').toString();
              final legacy = ChatRepository.parseLegacyMedia(content);
              final path = message['media_path'] as String? ?? legacy?.path;
              final url = path == null ? null : urls[path];
              final mime =
                  (message['media_mime'] ?? legacy?.mime ?? '').toString();

              return Container(
                margin: const EdgeInsets.only(bottom: 10),
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppTokens.surface,
                  borderRadius: AppTokens.radiusSm,
                  boxShadow: AppTokens.subtleShadow,
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _name(message['sender_id'] as String),
                      style: const TextStyle(fontWeight: FontWeight.w700),
                    ),
                    if (url != null && mime.startsWith('image/'))
                      Padding(
                        padding: const EdgeInsets.only(top: 8),
                        child: ClipRRect(
                          borderRadius: AppTokens.radiusSm,
                          child: Image.network(url, fit: BoxFit.cover),
                        ),
                      ),
                    if (url != null && mime.startsWith('video/'))
                      const Padding(
                        padding: EdgeInsets.only(top: 8),
                        child: ListTile(
                          contentPadding: EdgeInsets.zero,
                          leading: Icon(Icons.play_circle_fill),
                          title: Text('Video attachment'),
                        ),
                      ),
                    if (url != null &&
                        !mime.startsWith('image/') &&
                        !mime.startsWith('video/'))
                      Padding(
                        padding: const EdgeInsets.only(top: 8),
                        child: ListTile(
                          contentPadding: EdgeInsets.zero,
                          leading: const Icon(Icons.insert_drive_file_outlined),
                          title: Text(legacy?.name ?? 'File attachment'),
                          subtitle: const Text('Tap to download'),
                        ),
                      ),
                    if (content.isNotEmpty && legacy == null)
                      Padding(
                        padding: const EdgeInsets.only(top: 8),
                        child: Text(content),
                      ),
                  ],
                ),
              );
            },
          ),
        ),
        SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(10),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: controller,
                    onSubmitted: (_) => onSend(),
                    decoration: const InputDecoration(hintText: 'Message...'),
                  ),
                ),
                IconButton(
                  onPressed: onSend,
                  icon: const Icon(Icons.send),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
