import 'package:supabase_flutter/supabase_flutter.dart';

/// Shared realtime boundary. Feature repositories own event semantics; this
/// class only creates Supabase channels so UI code never manages channels.
class RealtimeChannels {
  const RealtimeChannels(this.client);
  final SupabaseClient client;

  RealtimeChannel messages(String conversationId) => client.channel(
        'messages:$conversationId',
      );

  RealtimeChannel notifications(String profileId) => client.channel(
        'notifications:$profileId',
      );
}
