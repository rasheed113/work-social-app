import { createClient } from "supabase";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const MODEL = "gpt-5.6-luna";
const KARACHI_TZ = "Asia/Karachi";

const SYSTEM_INSTRUCTIONS = `You are Work Social AI, the authenticated user's friendly work companion inside Work Social.
Be natural and conversational. You can use light Urdu/Hinglish and humor when the user does.
Be concise by default, but be useful.
Never invent Work Social data, users, messages, posts, entries, actions, or memories.
Only state facts about Work Social data when a tool returned them.
Only claim an action was completed when the tool result says it was executed successfully.
You have access only to the explicit tools supplied in this request. Never ask for or attempt SQL, database credentials, service keys, or secrets.
For persistent/external writes, use the write tool to prepare a confirmation rather than executing immediately.
If required information is missing, ask a focused question instead of guessing.
Use the conversation history supplied by the application as context. Do not claim long-term memory beyond that history.
The user's Work Social local timezone is ${KARACHI_TZ}. Current date is supplied by the server. Interpret relative dates such as Friday from that date only when the user's wording makes the intended date clear.`;

function json(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

function getPublishableKey(): string {
  const map = JSON.parse(Deno.env.get("SUPABASE_PUBLISHABLE_KEYS") ?? "{}");
  const key = map.default;
  if (!key) throw new Error("Supabase publishable key is unavailable.");
  return key;
}

function userScopedClient(req: Request) {
  const authorization = req.headers.get("Authorization");
  if (!authorization?.startsWith("Bearer ")) throw new Error("Missing user authorization.");
  return createClient(Deno.env.get("SUPABASE_URL")!, getPublishableKey(), {
    global: { headers: { Authorization: authorization } },
    auth: { persistSession: false, autoRefreshToken: false },
  });
}

async function requireUser(req: Request) {
  const authorization = req.headers.get("Authorization");
  if (!authorization?.startsWith("Bearer ")) throw new Error("Missing user authorization.");
  const token = authorization.slice("Bearer ".length);
  const client = userScopedClient(req);
  const { data, error } = await client.auth.getUser(token);
  if (error || !data.user) throw new Error("Your Work Social session is not valid.");
  return { client, user: data.user };
}

const tools = [
  {
    type: "function",
    name: "get_my_profile",
    description: "Read the authenticated user's Work Social profile.",
    parameters: { type: "object", properties: {}, additionalProperties: false },
    strict: true,
  },
  {
    type: "function",
    name: "get_my_posts",
    description: "Read the authenticated user's recent Work Social posts.",
    parameters: {
      type: "object",
      properties: { limit: { type: "integer", minimum: 1, maximum: 20 } },
      required: ["limit"],
      additionalProperties: false,
    },
    strict: true,
  },
  {
    type: "function",
    name: "get_notifications",
    description: "Read the authenticated user's recent Work Social notifications.",
    parameters: {
      type: "object",
      properties: { limit: { type: "integer", minimum: 1, maximum: 20 } },
      required: ["limit"],
      additionalProperties: false,
    },
    strict: true,
  },
  {
    type: "function",
    name: "get_entries",
    description: "Read the authenticated user's real Work Social diary/todo entries when Work Identity exists.",
    parameters: {
      type: "object",
      properties: { limit: { type: "integer", minimum: 1, maximum: 20 } },
      required: ["limit"],
      additionalProperties: false,
    },
    strict: true,
  },
  {
    type: "function",
    name: "create_entry",
    description: "Prepare a real Work Social diary/todo entry for user confirmation. Never execute the write directly.",
    parameters: {
      type: "object",
      properties: {
        entry_type: { type: "string", enum: ["note", "todo", "idea", "journal", "anything", "event"] },
        title: { type: ["string", "null"], maxLength: 200 },
        content: { type: "string", minLength: 1, maxLength: 20000 },
        deadline_iso: { type: ["string", "null"] },
      },
      required: ["entry_type", "title", "content", "deadline_iso"],
      additionalProperties: false,
    },
    strict: true,
  },
];

async function toolGetMyProfile(client: ReturnType<typeof createClient>, userId: string) {
  const { data, error } = await client.from("profiles").select("id,username,display_name,bio,avatar_url").eq("id", userId).single();
  if (error) throw new Error(error.message);
  return data;
}

async function toolGetMyPosts(client: ReturnType<typeof createClient>, userId: string, limit: number) {
  const safeLimit = Math.min(Math.max(limit, 1), 20);
  const { data, error } = await client.from("posts").select("id,content,privacy,created_at,updated_at,location_name").eq("profile_id", userId).order("created_at", { ascending: false }).limit(safeLimit);
  if (error) throw new Error(error.message);
  return data ?? [];
}

async function toolGetNotifications(client: ReturnType<typeof createClient>, userId: string, limit: number) {
  const safeLimit = Math.min(Math.max(limit, 1), 20);
  const { data, error } = await client.from("notifications").select("id,type,sender_id,post_id,comment_id,is_read,created_at,metadata").eq("receiver_id", userId).order("created_at", { ascending: false }).limit(safeLimit);
  if (error) throw new Error(error.message);
  return data ?? [];
}

async function toolGetEntries(client: ReturnType<typeof createClient>, userId: string, limit: number) {
  const { data: worker, error: workerError } = await client.from("worker_profiles").select("id").eq("profile_id", userId).maybeSingle();
  if (workerError) throw new Error(workerError.message);
  if (!worker) return { available: false, reason: "Work Identity is not set up for this account.", entries: [] };
  const safeLimit = Math.min(Math.max(limit, 1), 20);
  const { data, error } = await client.from("worker_diary_entries").select("id,entry_type,title,content,completed,created_at,updated_at,event_start_at,event_end_at,event_timezone").eq("worker_profile_id", worker.id).order("updated_at", { ascending: false }).limit(safeLimit);
  if (error) throw new Error(error.message);
  return { available: true, entries: data ?? [] };
}

async function prepareCreateEntry(client: ReturnType<typeof createClient>, userId: string, conversationId: string, args: Record<string, unknown>) {
  const { data: worker, error: workerError } = await client.from("worker_profiles").select("id").eq("profile_id", userId).maybeSingle();
  if (workerError) throw new Error(workerError.message);
  if (!worker) return { available: false, reason: "Work Identity is not set up, so a diary/todo entry cannot be created." };

  const entryType = String(args.entry_type);
  const title = args.title === null ? null : String(args.title);
  const content = String(args.content).trim();
  const deadlineIso = args.deadline_iso === null ? null : String(args.deadline_iso);
  if (!content) throw new Error("Entry content is required.");
  if (!['note','todo','idea','journal','anything','event'].includes(entryType)) throw new Error("Unsupported entry type.");

  const displaySummary = [
    title ? `Title: ${title}` : null,
    `Type: ${entryType}`,
    `Content: ${content}`,
    deadlineIso ? `Deadline: ${deadlineIso}` : null,
  ].filter(Boolean).join("\n");

  const { data: action, error } = await client.from("ai_pending_actions").insert({
    user_id: userId,
    conversation_id: conversationId,
    tool_name: "create_entry",
    arguments: { worker_profile_id: worker.id, entry_type: entryType, title, content, deadline_iso: deadlineIso },
    display_summary: displaySummary,
    expires_at: new Date(Date.now() + 10 * 60 * 1000).toISOString(),
  }).select("id,display_summary,expires_at").single();
  if (error) throw new Error(error.message);
  return { confirmation_required: true, action_id: action.id, display_summary: action.display_summary, expires_at: action.expires_at };
}

async function executeCreateEntry(client: ReturnType<typeof createClient>, userId: string, actionId: string) {
  const { data: action, error: actionError } = await client.from("ai_pending_actions").select("id,conversation_id,tool_name,arguments,status,expires_at").eq("id", actionId).eq("user_id", userId).single();
  if (actionError || !action) throw new Error("The requested action no longer exists.");
  if (action.tool_name !== "create_entry") throw new Error("Unsupported pending action.");
  if (action.status !== "pending") throw new Error(`This action is already ${action.status}.`);
  if (new Date(action.expires_at).getTime() <= Date.now()) {
    await client.from("ai_pending_actions").update({ status: "expired" }).eq("id", action.id).eq("user_id", userId);
    throw new Error("The confirmation expired. Please ask Work Social AI again.");
  }

  const args = action.arguments as Record<string, unknown>;
  const { data: worker } = await client.from("worker_profiles").select("id").eq("profile_id", userId).single();
  if (!worker || worker.id !== args.worker_profile_id) throw new Error("Your Work Identity is unavailable.");

  const { data: inserted, error: insertError } = await client.from("worker_diary_entries").insert({
    worker_profile_id: worker.id,
    entry_type: args.entry_type,
    title: args.title,
    content: args.content,
    completed: args.entry_type === "todo" ? false : null,
  }).select("id,entry_type,title,content,completed,created_at,updated_at,event_start_at,event_end_at,event_timezone").single();
  if (insertError) throw new Error(insertError.message);

  const { error: updateError } = await client.from("ai_pending_actions").update({ status: "confirmed" }).eq("id", action.id).eq("user_id", userId).eq("status", "pending");
  if (updateError) throw new Error(updateError.message);
  return { success: true, entry: inserted };
}

async function callOpenAI(input: unknown, previousResponseId?: string) {
  const apiKey = Deno.env.get("OPENAI_API_KEY");
  if (!apiKey) throw new Error("OPENAI_API_KEY is not configured on the AI backend.");
  const body: Record<string, unknown> = {
    model: MODEL,
    store: false,
    instructions: SYSTEM_INSTRUCTIONS,
    tools,
    tool_choice: "auto",
    input,
    reasoning: { effort: "low" },
  };
  if (previousResponseId) body.previous_response_id = previousResponseId;
  const response = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${apiKey}` },
    body: JSON.stringify(body),
  });
  const payload = await response.json();
  if (!response.ok) throw new Error(payload?.error?.message ?? `OpenAI request failed with ${response.status}.`);
  return payload;
}

function extractFunctionCalls(response: any) {
  return (response.output ?? []).filter((item: any) => item?.type === "function_call");
}

function extractOutputText(response: any): string {
  if (typeof response.output_text === "string") return response.output_text.trim();
  const chunks: string[] = [];
  for (const item of response.output ?? []) {
    if (item?.type === "message") {
      for (const content of item.content ?? []) if (content?.type === "output_text" && typeof content.text === "string") chunks.push(content.text);
    }
  }
  return chunks.join("\n").trim();
}

async function handleChat(req: Request) {
  const { client, user } = await requireUser(req);
  const body = await req.json();
  const message = String(body.message ?? "").trim();
  if (!message || message.length > 12000) return json(400, { error: "Message must contain 1-12000 characters." });

  let conversationId = typeof body.conversation_id === "string" ? body.conversation_id : null;
  if (conversationId) {
    const { data: existing, error } = await client.from("ai_conversations").select("id").eq("id", conversationId).eq("user_id", user.id).single();
    if (error || !existing) return json(404, { error: "Conversation not found." });
  } else {
    const { data: conversation, error } = await client.from("ai_conversations").insert({ user_id: user.id, title: message.slice(0, 80) }).select("id").single();
    if (error || !conversation) return json(500, { error: error?.message ?? "Could not create conversation." });
    conversationId = conversation.id;
  }

  const { data: history, error: historyError } = await client.from("ai_messages").select("role,content,tool_name,tool_call_id").eq("conversation_id", conversationId).eq("user_id", user.id).order("created_at", { ascending: false }).limit(30);
  if (historyError) return json(500, { error: historyError.message });
  const orderedHistory = (history ?? []).reverse().filter((item) => item.role === "user" || item.role === "assistant").map((item) => ({ role: item.role, content: item.content }));
  const input = [...orderedHistory, { role: "user", content: message }];

  const { data: userMessage, error: userMessageError } = await client.from("ai_messages").insert({ conversation_id: conversationId, user_id: user.id, role: "user", content: message }).select("id").single();
  if (userMessageError) return json(500, { error: userMessageError.message });

  let response = await callOpenAI(input);
  let pendingActions: unknown[] = [];
  let toolResults: unknown[] = [];

  for (let round = 0; round < 4; round++) {
    const calls = extractFunctionCalls(response);
    if (!calls.length) break;
    const outputs: Array<Record<string, unknown>> = [];

    for (const call of calls) {
      let args: Record<string, unknown> = {};
      try { args = JSON.parse(call.arguments ?? "{}"); } catch { throw new Error("The model returned invalid tool arguments."); }
      let result: unknown;
      let status = "executed";
      try {
        switch (call.name) {
          case "get_my_profile": result = await toolGetMyProfile(client, user.id); break;
          case "get_my_posts": result = await toolGetMyPosts(client, user.id, Number(args.limit)); break;
          case "get_notifications": result = await toolGetNotifications(client, user.id, Number(args.limit)); break;
          case "get_entries": result = await toolGetEntries(client, user.id, Number(args.limit)); break;
          case "create_entry":
            result = await prepareCreateEntry(client, user.id, conversationId!, args);
            status = "awaiting_confirmation";
            if ((result as any).confirmation_required) pendingActions.push(result);
            break;
          default: throw new Error("Tool is not registered.");
        }
      } catch (error) {
        status = "failed";
        result = { error: error instanceof Error ? error.message : "Tool failed." };
      }

      toolResults.push({ tool_name: call.name, status, result });
      const { data: toolCallRow } = await client.from("ai_tool_calls").insert({
        user_id: user.id,
        conversation_id: conversationId,
        message_id: userMessage.id,
        tool_name: call.name,
        arguments: args,
        result,
        status,
        completed_at: new Date().toISOString(),
      }).select("id").single();

      await client.from("ai_messages").insert({
        conversation_id: conversationId,
        user_id: user.id,
        role: "tool",
        content: JSON.stringify(result),
        tool_name: call.name,
        tool_call_id: call.call_id,
        metadata: { tool_call_row_id: toolCallRow?.id ?? null },
      });

      outputs.push({ type: "function_call_output", call_id: call.call_id, output: JSON.stringify(result) });
    }

    response = await callOpenAI(outputs, response.id);
  }

  const assistantText = extractOutputText(response) || "I couldn't produce a response just now. Please try again.";
  const { error: assistantError } = await client.from("ai_messages").insert({ conversation_id: conversationId, user_id: user.id, role: "assistant", content: assistantText, metadata: { pending_actions: pendingActions } });
  if (assistantError) return json(500, { error: assistantError.message });
  await client.from("ai_conversations").update({ updated_at: new Date().toISOString() }).eq("id", conversationId).eq("user_id", user.id);

  if (response.usage) {
    await client.from("ai_usage").insert({ user_id: user.id, conversation_id: conversationId, model: MODEL, input_tokens: response.usage.input_tokens ?? null, output_tokens: response.usage.output_tokens ?? null, total_tokens: response.usage.total_tokens ?? null, request_id: response.id });
  }

  return json(200, { conversation_id: conversationId, message: assistantText, pending_actions: pendingActions, tool_results: toolResults });
}

async function handleConfirm(req: Request) {
  const { client, user } = await requireUser(req);
  const body = await req.json();
  const actionId = String(body.action_id ?? "");
  if (!actionId) return json(400, { error: "action_id is required." });
  const result = await executeCreateEntry(client, user.id, actionId);
  return json(200, result);
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json(405, { error: "Method not allowed." });
  try {
    const body = await req.clone().json();
    if (body?.action === "confirm") return await handleConfirm(req);
    return await handleChat(req);
  } catch (error) {
    console.error("work-social-ai", error);
    return json(500, { error: error instanceof Error ? error.message : "AI request failed." });
  }
});
