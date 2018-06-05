package headache

import enumeratum.values._
import java.time.Instant
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
    case object GuildMemberChunk extends EventType("GUILD_MEMBER_CHUNK")
    case object GuildRoleCreate extends EventType("GUILD_ROLE_CREATE")
    case object GuildRoleUpdate extends EventType("GUILD_ROLE_UPDATE")
    case object GuildRoleDelete extends EventType("GUILD_ROLE_DELETE")
    case object MessageCreate extends EventType("MESSAGE_CREATE")
    case object MessageUpdate extends EventType("MESSAGE_UPDATE")
    case object MessageDelete extends EventType("MESSAGE_DELETE")
    case object MessageDeleteBulk extends EventType("MESSAGE_DELETE_BULK")
    case object PresenceUpdate extends EventType("PRESENCE_UPDATE")
    case object TypingStart extends EventType("TYPING_START")
    case object UserUpdate extends EventType("USER_UPDATE")
    case object VoiceStateUpdate extends EventType("VOICE_STATE_UPDATE")
    case object VoiceServerUpdate extends EventType("VOICE_SERVER_UPDATE")
    case object Unknown extends EventType("Unknown")
  }

  case class GatewayEvent(tpe: EventType, payload: () => DynJValueSelector)
  
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
  object ReadyEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.Ready) Some(ge.payload().d.extract[Ready]) else None }
  
  case object Resumed { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.Resumed) Some(()) else None }

  case class ChannelCreate(channel: Channel)
  object ChannelCreate { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.ChannelCreate) Some(ChannelCreate(ge.payload().d.extract)) else None }
  case class ChannelUpdate(channel: Channel)
  object ChannelUpdate { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.ChannelUpdate) Some(ChannelUpdate(ge.payload().d.extract)) else None }
  case class ChannelDelete(channel: Channel)
  object ChannelDelete { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.ChannelDelete) Some(ChannelDelete(ge.payload().d.extract)) else None }

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
  object GuildCreate { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildCreate) Some(GuildCreate(ge.payload().d.extract)) else None }
  case class GuildUpdate(guild: Guild)
  object GuildUpdate { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildUpdate) Some(GuildUpdate(ge.payload().d.extract)) else None }
  case class GuildDelete(guild: UnavailableGuild)
  object GuildDelete { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildDelete) Some(GuildDelete(ge.payload().d.extract)) else None }
  case class GuildBanAdd(guildId: Snowflake, user: User)
  object GuildBanAdd {
    def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildBanAdd) {
      val event = ge.payload()
      Some(GuildBanAdd(event.d.guild_id.extract, event.d.extract))
    } else None
  }
  case class GuildBanRemove(guildId: Snowflake, user: User)
  object GuildBanRemove {
    def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildBanRemove) {
      val event = ge.payload()
      Some(GuildBanRemove(event.d.guild_id.extract, event.d.extract))
    } else None
  }
  case class GuildEmojisUpdate(guildId: Snowflake, emojis: Array[Emoji])
  object GuildEmojisUpdateEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildEmojisUpdate) Some(ge.payload().d.extract[GuildEmojisUpdate]) else None }
  case class GuildIntegrationUpdate(guildId: Snowflake)
  object GuildIntegrationUpdateEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildIntegrationUpdate) Some(ge.payload().d.extract[GuildIntegrationUpdate]) else None }
  case class GuildMemberAdd(guildId: Snowflake, user: User)
  object GuildMemberAddEvent {
    def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildMemberAdd) {
      val event = ge.payload()
      Some(GuildMemberAdd(event.d.guild_id.extract, event.d.extract))
    } else None
  }
  case class GuildMemberRemove(guildId: Snowflake, user: User)
  object GuildMemberRemoveEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildMemberRemove) Some(ge.payload().d.extract[GuildMemberRemove]) else None }
  case class GuildMemberUpdate(guildId: Snowflake, user: User, roles: Array[Role])
  object GuildMemberUpdateEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildMemberUpdate) Some(ge.payload().d.extract[GuildMemberUpdate]) else None }
  case class GuildMemberChunk(guildId: Snowflake, members: Array[GuildMember])
  object GuildMemberChunkEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildMemberChunk) Some(ge.payload().d.extract[GuildMemberChunk]) else None }
  case class GuildRoleCreate(guildId: Snowflake, role: Role)
  object GuildRoleCreateEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildRoleCreate) Some(ge.payload().d.extract[GuildRoleCreate]) else None }
  case class GuildRoleUpdate(guildId: Snowflake, role: Role)
  object GuildRoleUpdateEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildRoleUpdate) Some(ge.payload().d.extract[GuildRoleUpdate]) else None }
  case class GuildRoleDelete(guildId: Snowflake, roleId: Snowflake)
  object GuildRoleDeleteEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.GuildRoleDelete) Some(ge.payload().d.extract[GuildRoleDelete]) else None }

  case class MessageCreate(message: Message)
  object MessageCreateEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.MessageCreate) Some(MessageCreate(ge.payload().d.extract)) else None }
  case class MessageUpdate(message: headache.MessageUpdate)
  object MessageUpdateEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.MessageUpdate) Some(GatewayEvents.MessageUpdate(ge.payload().d.extract)) else None }
  case class MessageDelete(channelId: Snowflake, id: String)
  object MessageDeleteEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.MessageDelete) Some(ge.payload().d.extract[MessageDelete]) else None }
  case class MessageDeleteBulk(channelId: Snowflake, ids: Array[String])
  object MessageDeleteBulkEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.MessageDeleteBulk) Some(ge.payload().d.extract[MessageDeleteBulk]) else None }

  case class PresenceUpdate(
    guildId: Snowflake,
    user: PresenceUser,
    nick: String,
    roles: Array[Snowflake] = Array.empty,
    game: Option[GameStatus] = None,
    status: String
  )
  object PresenceUpdateEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.PresenceUpdate) Some(ge.payload().d.extract[PresenceUpdate]) else None }
  case class PresenceUser(
    id: String,
    userName: Option[String] = None,
    discriminator: Option[String] = None,
    avatar: Option[String] = None,
    bot: Option[Boolean] = None,
    mfaEnabled: Option[Boolean] = None,
    verified: Option[Boolean] = None,
    email: Option[String] = None
  )

  case class TypingStart(channelId: Snowflake, userId: Snowflake, timestamp: Long)
  object TypingStartEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.TypingStart) Some(ge.payload().d.extract[TypingStart]) else None }

  case class UserUpdate(user: User)
  object UserUpdateEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.UserUpdate) Some(UserUpdate(ge.payload().d.extract)) else None }
  case class VoiceStateUpdate(userId: Snowflake, sessionId: String, voiceState: VoiceState)
  object VoiceStateUpdateEvent {
    def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.VoiceStateUpdate) {
      val event = ge.payload()
      Some(VoiceStateUpdate(event.d.user_id.extract, event.d.session_id.extract, event.d.extract))
    } else None
  }
  case class VoiceServerUpdate(guildId: Snowflake, token: String, endpoint: String)
  object VoiceServerUpdateEvent { def unapply(ge: GatewayEvent) = if (ge.tpe == EventType.VoiceServerUpdate) Some(ge.payload().d.extract[VoiceServerUpdate]) else None }

}
