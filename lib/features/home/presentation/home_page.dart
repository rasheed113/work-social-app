import 'package:flutter/material.dart';

import '../../../core/design/app_tokens.dart';
import '../../../core/supabase/supabase_client.dart';
import '../../posts/data/post_repository.dart';
import '../../posts/presentation/create_post_card.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});
  @override State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  late final PostRepository _repository;
  late Future<List<Map<String, dynamic>>> _posts;
  @override void initState() { super.initState(); _repository = PostRepository(SupabaseClientProvider.client); _posts = _repository.fetchPublicPosts(); }
  Future<void> _reload() async { setState(() => _posts = _repository.fetchPublicPosts()); await _posts; }
  @override Widget build(BuildContext context) => RefreshIndicator(
    color: AppTokens.brandPurple,
    onRefresh: _reload,
    child: CustomScrollView(slivers: [
      SliverAppBar(pinned: true, toolbarHeight: 68, backgroundColor: AppTokens.pageBackground.withValues(alpha: .96), titleSpacing: 16, title: const _Brand(), actions: [IconButton(tooltip: 'Sign out', onPressed: () => SupabaseClientProvider.client.auth.signOut(), icon: const Icon(Icons.logout_rounded)), const SizedBox(width: 8)]),
      SliverPadding(padding: const EdgeInsets.fromLTRB(16, 8, 16, 10), sliver: SliverToBoxAdapter(child: CreatePostCard(onCreated: _reload))),
      FutureBuilder<List<Map<String, dynamic>>>(future: _posts, builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) return const SliverFillRemaining(hasScrollBody: false, child: Center(child: CircularProgressIndicator()));
        if (snapshot.hasError) return SliverFillRemaining(hasScrollBody: false, child: _StateCard(icon: Icons.cloud_off_rounded, title: 'Unable to load posts', message: 'Check your connection and try again.', action: TextButton(onPressed: _reload, child: const Text('Retry'))));
        final posts = snapshot.data ?? const [];
        if (posts.isEmpty) return const SliverFillRemaining(hasScrollBody: false, child: _StateCard(icon: Icons.forum_outlined, title: 'No public posts yet', message: 'Be the first to share something with Work Social.'));
        return SliverPadding(padding: const EdgeInsets.fromLTRB(16, 6, 16, 24), sliver: SliverList.separated(itemCount: posts.length, separatorBuilder: (_, __) => const SizedBox(height: 14), itemBuilder: (_, i) => PostCard(post: posts[i])));
      }),
    ]),
  );
}

class _Brand extends StatelessWidget { const _Brand(); @override Widget build(BuildContext context) => Row(mainAxisSize: MainAxisSize.min, children: [Container(width: 38, height: 38, decoration: const BoxDecoration(gradient: AppTokens.brandGradient, borderRadius: AppTokens.radiusSm, boxShadow: AppTokens.subtleShadow), child: const Icon(Icons.hub_rounded, color: Colors.white, size: 21)), const SizedBox(width: 10), ShaderMask(shaderCallback: (b) => AppTokens.brandGradient.createShader(b), child: const Text('Work Social', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w800, color: Colors.white)))]); }

class PostCard extends StatelessWidget {
  const PostCard({super.key, required this.post}); final Map<String, dynamic> post;
  @override Widget build(BuildContext context) {
    final profile = post['profiles'] is Map ? Map<String, dynamic>.from(post['profiles']) : const <String, dynamic>{};
    final name = (profile['display_name'] ?? profile['username'] ?? 'Work Social user').toString();
    final username = profile['username']?.toString(); final avatar = profile['avatar_url']?.toString();
    final content = (post['content'] ?? '').toString(); final location = post['location_name']?.toString();
    final created = DateTime.tryParse(post['created_at']?.toString() ?? '');
    final attachments = post['attachments'] is List ? List<Map<String, dynamic>>.from((post['attachments'] as List).map((e) => Map<String, dynamic>.from(e as Map))) : const <Map<String, dynamic>>[];
    return Container(decoration: BoxDecoration(color: AppTokens.surface, borderRadius: AppTokens.radiusMd, border: Border.all(color: AppTokens.border), boxShadow: AppTokens.cardShadow), child: Padding(padding: const EdgeInsets.fromLTRB(18, 17, 18, 10), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Row(children: [_Avatar(name: name, url: avatar), const SizedBox(width: 11), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Row(children: [Flexible(child: Text(name, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w800, color: AppTokens.textPrimary))), const SizedBox(width: 7), const _Badge(label: 'PUBLIC')]), const SizedBox(height: 2), Text([if (username != null && username.isNotEmpty) '@$username', if (created != null) _age(created)].join('  •  '), style: const TextStyle(fontSize: 12, color: AppTokens.textMuted, fontWeight: FontWeight.w500))])), IconButton(onPressed: () {}, icon: const Icon(Icons.more_horiz_rounded), color: AppTokens.textMuted)]),
      if (content.isNotEmpty) ...[const SizedBox(height: 14), Text(content, style: const TextStyle(fontSize: 15, height: 1.48, color: AppTokens.textPrimary))],
      if (location != null && location.isNotEmpty) ...[const SizedBox(height: 10), Row(children: [const Icon(Icons.location_on_outlined, size: 17, color: AppTokens.interactive), const SizedBox(width: 4), Flexible(child: Text(location, style: const TextStyle(fontSize: 12.5, color: AppTokens.textSecondary)))])],
      if (attachments.isNotEmpty) ...[const SizedBox(height: 14), _Attachments(items: attachments)],
      const Padding(padding: EdgeInsets.only(top: 13), child: Divider(height: 1, color: AppTokens.border)),
      const Row(children: [_Action(Icons.favorite_border_rounded, 'Like'), _Action(Icons.chat_bubble_outline_rounded, 'Comment'), _Action(Icons.share_outlined, 'Share')]),
    ])));
  }
}

class _Attachments extends StatelessWidget { const _Attachments({required this.items}); final List<Map<String, dynamic>> items; @override Widget build(BuildContext context) => Column(children: [for (final item in items) ...[_Attachment(item), if (item != items.last) const SizedBox(height: 8)]]); }
class _Attachment extends StatelessWidget { const _Attachment(this.item); final Map<String, dynamic> item; @override Widget build(BuildContext context) { final kind=item['kind']?.toString(); final mime=item['mime_type']?.toString() ?? ''; final url=item['public_url']?.toString(); final name=item['file_name']?.toString() ?? 'Attachment'; if ((kind=='image'||mime.startsWith('image/')) && url!=null) return ClipRRect(borderRadius: AppTokens.radiusSm, child: AspectRatio(aspectRatio: 16/10, child: Image.network(url, fit: BoxFit.cover, errorBuilder: (_,__,___)=>const _Broken()))); final video=kind=='video'||mime.startsWith('video/'); return Container(width: double.infinity, padding: const EdgeInsets.all(13), decoration: BoxDecoration(color: AppTokens.surfaceSoft, borderRadius: AppTokens.radiusSm, border: Border.all(color: AppTokens.border)), child: Row(children: [Icon(video?Icons.play_circle_outline_rounded:Icons.insert_drive_file_outlined,color:AppTokens.interactive), const SizedBox(width:10), Expanded(child: Text(name,maxLines:2,overflow:TextOverflow.ellipsis,style:const TextStyle(fontWeight:FontWeight.w600,color:AppTokens.textPrimary))), const Icon(Icons.open_in_new_rounded,size:18,color:AppTokens.textMuted)])); } }
class _Avatar extends StatelessWidget { const _Avatar({required this.name,this.url}); final String name; final String? url; @override Widget build(BuildContext context) { final fallback=name.isEmpty?'?':name.characters.first.toUpperCase(); return Container(width:44,height:44,padding:const EdgeInsets.all(2),decoration:const BoxDecoration(shape:BoxShape.circle,gradient:AppTokens.brandGradient),child:CircleAvatar(backgroundColor:AppTokens.surface,backgroundImage:url!=null&&url!.isNotEmpty?NetworkImage(url!):null,child:url==null||url!.isEmpty?Text(fallback,style:const TextStyle(fontWeight:FontWeight.w800,color:AppTokens.interactive)):null)); } }
class _Badge extends StatelessWidget { const _Badge({required this.label}); final String label; @override Widget build(BuildContext context)=>Container(padding:const EdgeInsets.symmetric(horizontal:7,vertical:3),decoration:BoxDecoration(color:AppTokens.brandPurple.withValues(alpha:.10),borderRadius:AppTokens.radiusPill,border:Border.all(color:AppTokens.brandPurple.withValues(alpha:.18))),child:Text(label,style:const TextStyle(fontSize:9,letterSpacing:.45,fontWeight:FontWeight.w800,color:AppTokens.interactive))); }
class _Action extends StatelessWidget { const _Action(this.icon,this.label); final IconData icon; final String label; @override Widget build(BuildContext context)=>Expanded(child:TextButton.icon(onPressed:(){},icon:Icon(icon,size:18),label:Text(label),style:TextButton.styleFrom(foregroundColor:AppTokens.textSecondary,padding:const EdgeInsets.symmetric(vertical:9),textStyle:const TextStyle(fontSize:12.5,fontWeight:FontWeight.w700)))); }
class _Broken extends StatelessWidget { const _Broken(); @override Widget build(BuildContext context)=>Container(color:AppTokens.surfaceSoft,child:const Center(child:Icon(Icons.broken_image_outlined,color:AppTokens.textMuted))); }
class _StateCard extends StatelessWidget { const _StateCard({required this.icon,required this.title,required this.message,this.action}); final IconData icon; final String title; final String message; final Widget? action; @override Widget build(BuildContext context)=>Center(child:Padding(padding:const EdgeInsets.all(24),child:Container(padding:const EdgeInsets.all(24),decoration:BoxDecoration(color:AppTokens.surface,borderRadius:AppTokens.radiusMd,border:Border.all(color:AppTokens.border),boxShadow:AppTokens.cardShadow),child:Column(mainAxisSize:MainAxisSize.min,children:[Icon(icon,size:34,color:AppTokens.interactive),const SizedBox(height:12),Text(title,textAlign:TextAlign.center,style:const TextStyle(fontWeight:FontWeight.w800,color:AppTokens.textPrimary)),const SizedBox(height:6),Text(message,textAlign:TextAlign.center,style:const TextStyle(color:AppTokens.textSecondary)),if(action!=null)action!])))); }
String _age(DateTime date){final d=DateTime.now().difference(date.toLocal());if(d.inMinutes<1)return'now';if(d.inHours<1)return'${d.inMinutes}m';if(d.inDays<1)return'${d.inHours}h';if(d.inDays<7)return'${d.inDays}d';return'${date.month}/${date.day}/${date.year}';}
