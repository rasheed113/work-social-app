import 'package:flutter/material.dart';

import '../../../core/supabase/supabase_client.dart';
import '../../posts/data/post_repository.dart';
import '../../posts/presentation/create_post_card.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});
  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  late final PostRepository _repository;
  late Future<List<Map<String, dynamic>>> _posts;

  @override
  void initState() {
    super.initState();
    _repository = PostRepository(SupabaseClientProvider.client);
    _posts = _repository.fetchPublicPosts();
  }

  Future<void> _reload() async {
    setState(() => _posts = _repository.fetchPublicPosts());
    await _posts;
  }

  @override
  Widget build(BuildContext context) => RefreshIndicator(
        onRefresh: _reload,
        child: CustomScrollView(
          slivers: [
            SliverAppBar(
              pinned: true,
              title: const Text('Work Social'),
              actions: [
                IconButton(
                  tooltip: 'Sign out',
                  onPressed: () => SupabaseClientProvider.client.auth.signOut(),
                  icon: const Icon(Icons.logout),
                ),
              ],
            ),
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
              sliver: SliverToBoxAdapter(child: CreatePostCard(onCreated: _reload)),
            ),
            FutureBuilder<List<Map<String, dynamic>>>(
              future: _posts,
              builder: (context, snapshot) {
                if (snapshot.connectionState == ConnectionState.waiting) {
                  return const SliverFillRemaining(child: Center(child: CircularProgressIndicator()));
                }
                if (snapshot.hasError) {
                  return SliverFillRemaining(child: Center(child: Text('Unable to load posts: ${snapshot.error}')));
                }
                final posts = snapshot.data ?? const [];
                if (posts.isEmpty) {
                  return const SliverFillRemaining(child: Center(child: Text('No public posts yet.')));
                }
                return SliverPadding(
                  padding: const EdgeInsets.all(16),
                  sliver: SliverList.separated(
                    itemCount: posts.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 12),
                    itemBuilder: (_, index) => PostCard(post: posts[index]),
                  ),
                );
              },
            ),
          ],
        ),
      );
}

class PostCard extends StatelessWidget {
  const PostCard({super.key, required this.post});
  final Map<String, dynamic> post;

  @override
  Widget build(BuildContext context) {
    final profile = post['profiles'] is Map ? Map<String, dynamic>.from(post['profiles']) : const <String, dynamic>{};
    final name = (profile['display_name'] ?? profile['username'] ?? 'Work Social user').toString();
    final content = (post['content'] ?? '').toString();
    final location = post['location_name'];
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            CircleAvatar(child: Text(name.isEmpty ? '?' : name[0].toUpperCase())),
            const SizedBox(width: 12),
            Expanded(child: Text(name, style: const TextStyle(fontWeight: FontWeight.w700))),
          ]),
          if (content.isNotEmpty) ...[
            const SizedBox(height: 14),
            Text(content),
          ],
          if (location != null && location.toString().isNotEmpty) ...[
            const SizedBox(height: 10),
            Row(children: [const Icon(Icons.location_on_outlined, size: 17), const SizedBox(width: 4), Text(location.toString())]),
          ],
        ]),
      ),
    );
  }
}
