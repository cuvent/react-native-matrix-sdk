export enum EventTypes {
  PAGINATE_BACK_TOKEN_END = "PAGINATE_BACK_TOKEN_END",

  EVENT_TYPE_PRESENCE = "m.presence",
  EVENT_TYPE_MESSAGE = "m.room.message",
  EVENT_TYPE_STICKER = "m.sticker",
  EVENT_TYPE_MESSAGE_ENCRYPTED = "m.room.encrypted",
  EVENT_TYPE_MESSAGE_ENCRYPTION = "m.room.encryption",
  EVENT_TYPE_FEEDBACK = "m.room.message.feedback",
  EVENT_TYPE_TYPING = "m.typing",
  EVENT_TYPE_REDACTION = "m.room.redaction",
  EVENT_TYPE_RECEIPT = "m.receipt",
  EVENT_TYPE_ROOM_PLUMBING = "m.room.plumbing",
  EVENT_TYPE_ROOM_BOT_OPTIONS = "m.room.bot.options",

  // Possible value for room account data type
  EVENT_TYPE_TAGS = "m.tag",
  EVENT_TYPE_READ_MARKER = "m.fully_read",
  EVENT_TYPE_URL_PREVIEW = "org.matrix.room.preview_urls",

  // State events
  EVENT_TYPE_STATE_ROOM_NAME = "m.room.name",
  EVENT_TYPE_STATE_ROOM_TOPIC = "m.room.topic",
  EVENT_TYPE_STATE_ROOM_AVATAR = "m.room.avatar",
  EVENT_TYPE_STATE_ROOM_MEMBER = "m.room.member",
  EVENT_TYPE_STATE_ROOM_THIRD_PARTY_INVITE = "m.room.third_party_invite",
  EVENT_TYPE_STATE_ROOM_CREATE = "m.room.create",
  EVENT_TYPE_STATE_ROOM_JOIN_RULES = "m.room.join_rules",
  EVENT_TYPE_STATE_ROOM_GUEST_ACCESS = "m.room.guest_access",
  EVENT_TYPE_STATE_ROOM_POWER_LEVELS = "m.room.power_levels",
  EVENT_TYPE_STATE_ROOM_ALIASES = "m.room.aliases",
  EVENT_TYPE_STATE_ROOM_TOMBSTONE = "m.room.tombstone",
  EVENT_TYPE_STATE_CANONICAL_ALIAS = "m.room.canonical_alias",
  EVENT_TYPE_STATE_HISTORY_VISIBILITY = "m.room.history_visibility",
  EVENT_TYPE_STATE_RELATED_GROUPS = "m.room.related_groups",
  EVENT_TYPE_STATE_PINNED_EVENT = "m.room.pinned_events",

  // call events
  EVENT_TYPE_CALL_INVITE = "m.call.invite",
  EVENT_TYPE_CALL_CANDIDATES = "m.call.candidates",
  EVENT_TYPE_CALL_ANSWER = "m.call.answer",
  EVENT_TYPE_CALL_HANGUP = "m.call.hangup",
}
