package headache

import enumeratum.{Enum, EnumEntry}
import enumeratum.values._
import java.time.Instant
import play.api.libs.json.{JsLookupResult, Json, JsSuccess, JsError}
import JsonUtils._, JsonCodecs._

object GatewayEvents {

  sealed abstract class EventType(val value: String) extends StringEnumEntry
  object EventType extends StringEnum[EventType] {
    val values = findValues
    case object Ready extends EventType("READY")
    case object Resumed extends EventType("RESUMED")
    case object ChannelCreate extends EventType("CHANNEL_CREATE")
    case object ChannelUpdate extends EventType("CHANNEL_UPDATE")
    case object ChannelDelete extends EventType("CHANNEL_DELETE")
    case object GuildCreate extends EventType("GUILD_CREATE")
    case object GuildUpdate extends EventType("GUILD_UPDATE")
    case object GuildDelete extends EventType("GUILD_DELETE")
    case object GuildBanAdd extends EventType("GUILD_BAN_ADD")
    case object GuildBanRemove extends EventType("GUILD_BAN_REMOVE")
    case object GuildEmojisUpdate extends EventType("GUILD_EMOJIS_UPDATE")
    case object GuildIntegrationUpdate extends EventType("GUILD_INTEGRATION_UPDATE")
    case object GuildMemberAdd extends EventType("GUILD_MEMBER_ADD")
    case object GuildMemberRemove extends EventType("GUILD_MEMBER_REMOVE")
    case object GuildMemberUpdate extends EventType("GUILD_MEMBER_UPDATE")
    case object GuildMemberChunk extends EventType("GUILD_MEMBERS_CHUNK")
    case object GuildRoleCreate extends EventType("GUILD_ROLE_CREATE")
    case object GuildRoleUpdate extends EventType("GUILD_ROLE_UPDATE")
    case object GuildRoleDelete extends EventType("GUILD_ROLE_DELETE")
    case object MessageCreate extends EventType("MESSAGE_CREATE")
    case object MessageUpdate extends EventType("MESSAGE_UPDATE")
    case object MessageDelete extends EventType("MESSAGE_DELETE")
    case object MessageDeleteBulk extends EventType("MESSAGE_DELETE_BULK")
    case object MessageReactionAdd extends EventType("MESSAGE_REACTION_ADD")
    case object MessageReactionRemove extends EventType("MESSAGE_REACTION_REMOVE")
    case object MessageReactionRemoveAll extends EventType("MESSAGE_REACTION_REMOVE_ALL")
    case object PresenceUpdate extends EventType("PRESENCE_UPDATE")
    case object TypingStart extends EventType("TYPING_START")
    case object UserUpdate extends EventType("USER_UPDATE")
    case object VoiceStateUpdate extends EventType("VOICE_STATE_UPDATE")
    case object VoiceServerUpdate extends EventType("VOICE_SERVER_UPDATE")
    case object Unknown extends EventType("Unknown")
  }

  type CoManifest[+T] = Manifest[T @scala.annotation.unchecked.uncheckedVariance]
  case class GatewayEvent(tpe: EventType, payload: () => DynJValueSelector) {
    def payloadAs_![T: JsonUtils.Reads: CoManifest]: T = {
      val m = manifest[T]
      val p = payload()
      p.d.jv.validate[T] match {
        case JsSuccess(res, _) => res
        case e: JsError => throw new GatewayEventParsingError(p.jv, m, e)
      }
    }
  }
  class GatewayEventParsingError(payload: JsLookupResult, expectedType: Manifest[_], parseError: JsError) extends RuntimeException(
    s"Failed to parse $expectedType from payload \n${Json prettyPrint payload.get}\n")
  
  case class ReadState(id: Snowflake, lastMessageId: Option[Snowflake] = None, mentionCount: Option[Int] = None)
  case class Ready(
    v: Int,
    user: User,
    privateChannels: Array[Channel] = Array.empty,
    guilds: Array[UnavailableGuild Either Guild] = Array.empty,
    userSettings: Option[UserSettings] = None,
    readState: Array[ReadState] = Array.empty,
    /* presences: Array[Any],
     relationships: Array[Any], */
    _trace: Array[String] = Array.empty
  )
  object ReadyEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.Ready) Some(ge.payloadAs_![Ready]) else None }
  
  case object Resumed { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.Resumed) Some(()) else None }

  case class ChannelCreate(channel: Channel)
  object ChannelCreateEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.ChannelCreate) Some(ChannelCreate(ge.payloadAs_!)) else None }
  case class ChannelUpdate(channel: Channel)
  object ChannelUpdateEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.ChannelUpdate) Some(ChannelUpdate(ge.payloadAs_!)) else None }
  case class ChannelDelete(channel: Channel)
  object ChannelDeleteEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.ChannelDelete) Some(ChannelDelete(ge.payloadAs_!)) else None }

  case class Guild(
    id: Snowflake,
    name: String,
    icon: Option[String] = None,
    splash: Option[String] = None,
    ownerId: Snowflake,
    region: String,
    afkChannelId: Option[Snowflake] = None,
    afkTimeout: Int,
    embedEnabled: Option[Boolean] = None,
    embedChannelId: Option[Snowflake] = None,
    verificationLevel: Int,
    defaultMessageNotifications: NotificationLevel,
    explicitContentFilter: ExplicitContentFilterLevel,
    roles: Array[Role] = Array.empty,
    emojis: Array[Emoji] = Array.empty,
    features: Array[String] = Array.empty,
    mfaLevel: Int,
    applicationId: Option[Snowflake] = None,
    widgetEnabled: Option[Boolean] = None,
    widgetChannelId: Option[Snowflake] = None,
    joinedAt: Option[Instant] = None,
    large: Boolean = false,
    unavailable: Boolean = false,
    memberCount: Int,
    members: Array[GuildMember] = Array.empty,
    voiceStates: Array[VoiceState] = Array.empty,
    channels: Array[Channel] = Array.empty,
    presences: Array[GuildPresence] = Array.empty
  ) extends GuildDef

  case class GuildCreate(guild: Guild)
  object GuildCreateEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildCreate) Some(GuildCreate(ge.payloadAs_!)) else None }
  case class GuildUpdate(guild: Guild)
  object GuildUpdateEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildUpdate) Some(GuildUpdate(ge.payloadAs_!)) else None }
  case class GuildDelete(guild: UnavailableGuild)
  object GuildDeleteEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildDelete) Some(GuildDelete(ge.payloadAs_!)) else None }
  case class GuildBanAdd(guildId: Snowflake, user: User)
  object GuildBanAddEvent {
    def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildBanAdd) {
      val event = ge.payload()
      Some(GuildBanAdd(event.d.guild_id.extract, event.d.extract))
    } else None
  }
  case class GuildBanRemove(guildId: Snowflake, user: User)
  object GuildBanRemoveEvent {
    def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildBanRemove) {
      val event = ge.payload()
      Some(GuildBanRemove(event.d.guild_id.extract, event.d.extract))
    } else None
  }
  case class GuildEmojisUpdate(guildId: Snowflake, emojis: Array[Emoji])
  object GuildEmojisUpdateEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildEmojisUpdate) Some(ge.payloadAs_![GuildEmojisUpdate]) else None }
  case class GuildIntegrationUpdate(guildId: Snowflake)
  object GuildIntegrationUpdateEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildIntegrationUpdate) Some(ge.payloadAs_![GuildIntegrationUpdate]) else None }
  case class GuildMemberAdd(guildId: Snowflake, user: User)
  object GuildMemberAddEvent {
    def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildMemberAdd) {
      val event = ge.payload()
      Some(GuildMemberAdd(event.d.guild_id.extract, event.d.extract))
    } else None
  }
  case class GuildMemberRemove(guildId: Snowflake, user: User)
  object GuildMemberRemoveEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildMemberRemove) Some(ge.payloadAs_![GuildMemberRemove]) else None }
  case class GuildMemberUpdate(guildId: Snowflake, user: User, roles: Array[Snowflake], nick: Option[String])
  object GuildMemberUpdateEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildMemberUpdate) Some(ge.payloadAs_![GuildMemberUpdate]) else None }
  case class GuildMemberChunk(guildId: Snowflake, members: Array[GuildMember])
  object GuildMemberChunkEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildMemberChunk) Some(ge.payloadAs_![GuildMemberChunk]) else None }
  case class GuildRoleCreate(guildId: Snowflake, role: Role)
  object GuildRoleCreateEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildRoleCreate) Some(ge.payloadAs_![GuildRoleCreate]) else None }
  case class GuildRoleUpdate(guildId: Snowflake, role: Role)
  object GuildRoleUpdateEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildRoleUpdate) Some(ge.payloadAs_![GuildRoleUpdate]) else None }
  case class GuildRoleDelete(guildId: Snowflake, roleId: Snowflake)
  object GuildRoleDeleteEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildRoleDelete) Some(ge.payloadAs_![GuildRoleDelete]) else None }

  case class MessageCreate(message: Message)
  object MessageCreateEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.MessageCreate) Some(MessageCreate(ge.payloadAs_!)) else None }
  case class MessageUpdate(message: headache.MessageUpdate)
  object MessageUpdateEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.MessageUpdate) Some(GatewayEvents.MessageUpdate(ge.payloadAs_!)) else None }
  case class MessageDelete(channelId: Snowflake, id: String)
  object MessageDeleteEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.MessageDelete) Some(ge.payloadAs_![MessageDelete]) else None }
  case class MessageDeleteBulk(channelId: Snowflake, ids: Array[String])
  object MessageDeleteBulkEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.MessageDeleteBulk) Some(ge.payloadAs_![MessageDeleteBulk]) else None }
  
  case class EmojiReference(id: Option[Snowflake], name: String)
  case class MessageReaction(userId: Snowflake, channelId: Snowflake, messageId: Snowflake, emoji: EmojiReference)
  object MessageReactionAddEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.MessageReactionAdd) Some(ge.payloadAs_![MessageReaction]) else None }
  object MessageReactionRemoveEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.MessageReactionRemove) Some(ge.payloadAs_![MessageReaction]) else None }
  case class MessageReactionRemoveAll(channelId: Snowflake, messageId: Snowflake)
  object MessageReactionRemoveAllEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.MessageReactionRemoveAll) Some(ge.payloadAs_![MessageReactionRemoveAll]) else None }

  case class PresenceUpdate(
    guildId: Snowflake,
    user: PresenceUser,
    nick: Option[String] = None,
    roles: Array[Snowflake] = Array.empty,
    game: Option[GameStatus] = None,
    status: String
  )
  object PresenceUpdateEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.PresenceUpdate) Some(ge.payloadAs_![PresenceUpdate]) else None }
  case class PresenceUser(
    id: Snowflake,
    userName: Option[String] = None,
    discriminator: Option[String] = None,
    avatar: Option[String] = None,
    bot: Option[Boolean] = None,
    mfaEnabled: Option[Boolean] = None,
    verified: Option[Boolean] = None,
    email: Option[String] = None
  )

  case class TypingStart(channelId: Snowflake, userId: Snowflake, timestamp: Long)
  object TypingStartEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.TypingStart) Some(ge.payloadAs_![TypingStart]) else None }

  case class UserUpdate(user: User)
  object UserUpdateEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.UserUpdate) Some(UserUpdate(ge.payloadAs_!)) else None }
  case class VoiceStateUpdate(userId: Snowflake, sessionId: String, voiceState: VoiceState)
  object VoiceStateUpdateEvent {
    def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.VoiceStateUpdate) {
      val event = ge.payload()
      Some(VoiceStateUpdate(event.d.user_id.extract, event.d.session_id.extract, event.d.extract))
    } else None
  }
  case class VoiceServerUpdate(guildId: Snowflake, token: String, endpoint: Option[String] = None)
  object VoiceServerUpdateEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.VoiceServerUpdate) Some(ge.payloadAs_![VoiceServerUpdate]) else None }


  sealed trait Intent extends EnumEntry
  object Intent extends Enum[Intent] {
    val values = findValues
    case object Guilds extends Intent
    case object GuildMembers extends Intent
    case object GuildBans extends Intent
    case object GuildEmojis extends Intent
    case object GuildIntegrations extends Intent
    case object GuildWebhooks extends Intent
    case object GuildInvites extends Intent
    case object GuildVoiceStates extends Intent
    case object GuildPresences extends Intent
    case object GuildMessages extends Intent
    case object GuildMessageReactions extends Intent
    case object GuildMessageTypings extends Intent
    case object DirectMessageReactions extends Intent
    case object DirectMessageTypings extends Intent
  }
}
