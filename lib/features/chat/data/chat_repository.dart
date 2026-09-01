import 'dart:convert';
import 'package:supabase_flutter/supabase_flutter.dart';

class ChatRepository {
  ChatRepository(this.client);
  final SupabaseClient client;
  Future<Map<String,dynamic>> load() async {
    final uid=client.auth.currentUser?.id;
    if(uid==null) throw const AuthException('You must be signed in.');
    final mine=await client.from('conversation_members').select('conversation_id,profile_id,last_read_at').eq('profile_id',uid);
    final ids=List<Map<String,dynamic>>.from(mine).map((x)=>x['conversation_id'] as String).toList();
    if(ids.isEmpty)return {'conversations':<Map<String,dynamic>>[],'members':<Map<String,dynamic>>[],'messages':<Map<String,dynamic>>[],'profiles':<Map<String,dynamic>>[],'media':<String,String>{}};
    final r=await Future.wait([
      client.from('conversations').select('id,kind,title,avatar_url,created_by,updated_at').inFilter('id',ids).order('updated_at',ascending:false),
      client.from('conversation_members').select('conversation_id,profile_id,last_read_at').inFilter('conversation_id',ids),
      client.from('messages').select('id,conversation_id,sender_id,content,message_type,media_path,media_mime,media_name,media_size,media_duration_ms,created_at,read_at,reply_to_message_id,edited_at,deleted_at').inFilter('conversation_id',ids).order('created_at',ascending:true),
    ]);
    final messages=List<Map<String,dynamic>>.from(r[2] as List), members=List<Map<String,dynamic>>.from(r[1] as List);
    final pids={...members.map((x)=>x['profile_id'] as String),...messages.map((x)=>x['sender_id'] as String)}.toList();
    final profiles=pids.isEmpty?<Map<String,dynamic>>[]:List<Map<String,dynamic>>.from(await client.from('profiles').select('id,display_name,username,avatar_url').inFilter('id',pids));
    final paths=<String>[];
    for(final m in messages){final legacy=parseLegacyMedia(m['content'] as String?);final path=m['media_path'] as String? ?? legacy?.path;if(path!=null)paths.add(path);}
    final uniquePaths=paths.toSet().toList(); final media=<String,String>{};
    if(uniquePaths.isNotEmpty){final sr=await client.storage.from('chat-media').createSignedUrls(uniquePaths,3600);if(!sr.error)for(var i=0;i<sr.data!.length;i++){final u=sr.data![i].signedUrl;if(u!=null)media[uniquePaths[i]]=u;}}
    return {'conversations':List<Map<String,dynamic>>.from(r[0] as List),'members':members,'messages':messages,'profiles':profiles,'media':media};
  }
  static LegacyMedia? parseLegacyMedia(String? content){const prefix='__work_social_media__:';if(content==null||!content.startsWith(prefix))return null;try{final p=Map<String,dynamic>.from(jsonDecode(content.substring(prefix.length)) as Map);if(p['path'] is String&&p['mime'] is String)return LegacyMedia(path:p['path'],mime:p['mime'],name:(p['name']??'Media').toString(),size:(p['size']??0) as num);}catch(_){ }return null;}
  Future<void> sendText(String conversationId,String text)async{await client.from('messages').insert({'conversation_id':conversationId,'content':text.trim(),'message_type':'text'});}
}
class LegacyMedia{const LegacyMedia({required this.path,required this.mime,required this.name,required this.size});final String path,mime,name;final num size;}
