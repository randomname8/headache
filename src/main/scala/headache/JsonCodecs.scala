package headache

import enumeratum.values.{IntEnum, IntEnumEntry, StringEnumEntry, StringEnum}
import play.api.libs.json._
import java.time.Instant
import play.api.libs.functional.syntax._

object JsonCodecs {

  val NamingMapping: JsonNaming = {
    case "tpe" => "type"
    case "userName" => "username"
    case other => JsonNaming.SnakeCase(other)
  }
  implicit val jsonConf = JsonConfiguration[Json.WithDefaultValues](NamingMapping)
  
  implicit object BooleanReads extends Reads[Boolean] {
    override def reads(jv: JsValue) = jv match {
      case JsNull => JsSuccess(false)
      case JsBoolean(b) => JsSuccess(b)
      case _ => JsError(Seq(JsPath -> Seq(JsonValidationError("error.expected.jsboolean"))))
    }
  }
  implicit object SnowflakeReads extends Reads[Snowflake] {
    override def reads(jv: JsValue) = jv match {
      case JsString(s) => JsSuccess(Snowflake(s))
      case JsNumber(l) if l.isValidLong => JsSuccess(Snowflake(l.toLong))
      case other => JsError(JsonValidationError("error.expected.snowflake"))
    }
  }
//  implicit def seqReads[T: Reads]: Reads[Seq[T]] = new Reads[Seq[T]] {
//    val inner = Reads.seq[T]
//    override def reads(jv: JsValue) = jv match {
//      case JsNull => JsSuccess(Seq.empty)
//      case other => inner.reads(other)
//    }
//  }
//  implicit def mapReads[V: Reads]: Reads[Map[String, V]] = new Reads[Map[String, V]] {
//    val inner = Reads.map[V]
//    override def reads(jv: JsValue) = jv match {
//      case JsNull => JsSuccess(Map.empty)
//      case other => inner.reads(other)
//    }
//  }
//  implicit def optionReads[T: Reads]: Reads[Option[T]] = Reads.optionWithNull[T]
  implicit def eitherFormat[L: Format, R: Format]: Format[L Either R] = new Format[L Either R] {
    override def reads(jv: JsValue) = Reads.of[R].reads(jv).map(Right.apply) match {
      case errRight: JsError => Reads.of[L].reads(jv).map(Left.apply) match {
          case errLeft: JsError => JsError("Unable to read either") ++ errRight ++ errLeft
          case other => other
        }
      case other => other
    }
    override def writes(e: L Either R) = e.fold(Writes.of[L].writes, Writes.of[R].writes)
  }
  implicit def intEnumPickler[T <: IntEnumEntry](implicit enum: IntEnum[T]): Format[T] = new Format[T] {
    override def reads(jv: JsValue) = Reads.of[Int].map(enum.withValue).reads(jv)
    override def writes(e: T) = JsNumber(e.value)
  }
  implicit def stringEnumPickler[T <: StringEnumEntry](implicit enum: StringEnum[T]): Format[T] = new Format[T] {
    override def reads(jv: JsValue) = Reads.of[String].map(enum.withValue).reads(jv)
    override def writes(e: T) = JsString(e.value)
  }
  
  implicit object UserSettingsFormat extends Format[Option[UserSettings]] {
    private val rawFormat: Format[UserSettings] = Json.format
    override def reads(jv: JsValue) = jv match {
      case JsNull => JsSuccess(None)
      case JsObject(entries) if entries.isEmpty => JsSuccess(None)
      case other => rawFormat.reads(other).map(Some.apply)
    }
    override def writes(e: Option[UserSettings]) = e.fold[JsValue](Json.obj())(rawFormat.writes)
  }
  
  
  //cached formats
  implicit val userFormat: Format[User] = Json.format
  implicit val guildMemberFormat: Format[GuildMember] = Json.format
  implicit val gameStatusTimestampsFormat: Format[GameStatus.Timestamps] = Json.format
  implicit val gameStatusAssetsFormat: Format[GameStatus.Assets] = Json.format
  implicit val gameStatusFormat: Format[GameStatus] = Json.format
  implicit val guildUserFormat: Format[PresenceUser] = Json.format
  implicit val guildPresenceFormat: Format[GuildPresence] = Json.format
  implicit val roleFormat: Format[Role] = Json.format
  implicit val emojiFormat: Format[Emoji] = Json.format
  implicit val voiceStateFormat: Format[VoiceState] = Json.format
  implicit val embedThumbnailFormat: Format[EmbedThumbnail] = Json.format
  implicit val embedVideoFormat: Format[EmbedVideo] = Json.format
  implicit val embedImageFormat: Format[EmbedImage] = Json.format
  implicit val embedProviderFormat: Format[EmbedProvider] = Json.format
  implicit val embedAuthorFormat: Format[EmbedAuthor] = Json.format
  implicit val embedFooterFormat: Format[EmbedFooter] = Json.format
  implicit val embedFieldFormat: Format[EmbedField] = Json.format
  implicit val attachmentFormat: Format[Attachment] = Json.format
  implicit val banFormat: Format[Ban] = Json.format
  implicit val embedFormat: Format[Embed] = Json.format
  implicit val messageFormat: Format[Message] = Json.format
  implicit val messageUpdateFormat: Format[MessageUpdate] = Json.format
  implicit val guildFormat: Format[Guild] = Json.format
  implicit val overwriteFormat: Format[PermissionOverwrite] = Json.format
  implicit val channelStateFormat: Format[Channel] = Json.format
  implicit val unavailableGuildFormat: Format[UnavailableGuild] = Json.format
  
  //cached gateway events
  implicit val readStateFormat: Format[GatewayEvents.ReadState] = Json.format
  implicit val gatewayEventGuildFormat: Format[GatewayEvents.Guild] = new Format[GatewayEvents.Guild] {
    val id = (__ \ "id").format[Snowflake]
    val name = (__ \ "name").format[String]
    val icon = (__ \ "icon").formatNullable[String]
    val splash = (__ \ "splash").formatNullable[String]
    val ownerId = (__ \ "owner_id").format[Snowflake]
    val region = (__ \ "region").format[String]
    val afkChannelId = (__ \ "afk_channel_id").formatNullable[Snowflake]
    val afkTimeout = (__ \ "afk_timeout").format[Int]
    val embedEnabled = (__ \ "embed_enabled").formatNullable[Boolean]
    val embedChannelId = (__ \ "embed_channel_id").formatNullable[Snowflake]
    val verificationLevel = (__ \ "verification_level").format[Int]
    val defaultMessageNotifications = (__ \ "default_message_notifications").format[NotificationLevel]
    val explicitContentFilter = (__ \ "explicit_content_filter").format[ExplicitContentFilterLevel]
    val roles = (__ \ "roles").formatWithDefault[Array[Role]](Array.empty)
    val emojis = (__ \ "emojis").formatWithDefault[Array[Emoji]](Array.empty)
    val features = (__ \ "features").formatWithDefault[Array[String]](Array.empty)
    val mfaLevel = (__ \ "mfa_level").format[Int]
    val applicationId = (__ \ "application_id").formatNullable[Snowflake]
    val widgetEnabled = (__ \ "widget_enabled").formatNullable[Boolean]
    val widgetChannelId = (__ \ "widget_channel_id").formatNullable[Snowflake]
    val joinedAt = (__ \ "joined_at").formatNullable[Instant]
    val large = (__ \ "large").formatWithDefault[Boolean](false)
    val unavailable = (__ \ "unavailable").formatWithDefault[Boolean](false)
    val memberCount = (__ \ "member_count").format[Int]
    val members = (__ \ "members").formatWithDefault[Array[GuildMember]](Array.empty)
    val voiceStates = (__ \ "voice_states").formatWithDefault[Array[VoiceState]](Array.empty)
    val channels = (__ \ "channels").formatWithDefault[Array[Channel]](Array.empty)
    val presences = (__ \ "presences").formatWithDefault[Array[GuildPresence]](Array.empty)
    override def reads(js: JsValue) = for {
      id <- id.reads(js)
      name <- name.reads(js)
      icon <- icon.reads(js)
      splash <- splash.reads(js)
      ownerId <- ownerId.reads(js)
      region <- region.reads(js)
      afkChannelId <- afkChannelId.reads(js)
      afkTimeout <- afkTimeout.reads(js)
      embedEnabled <- embedEnabled.reads(js)
      embedChannelId <- embedChannelId.reads(js)
      verificationLevel <- verificationLevel.reads(js)
      defaultMessageNotifications <- defaultMessageNotifications.reads(js)
      explicitContentFilter <- explicitContentFilter.reads(js)
      roles <- roles.reads(js)
      emojis <- emojis.reads(js)
      features <- features.reads(js)
      mfaLevel <- mfaLevel.reads(js)
      applicationId <- applicationId.reads(js)
      widgetEnabled <- widgetEnabled.reads(js)
      widgetChannelId <- widgetChannelId.reads(js)
      joinedAt <- joinedAt.reads(js)
      large <- large.reads(js)
      unavailable <- unavailable.reads(js)
      memberCount <- memberCount.reads(js)
      members <- members.reads(js)
      voiceStates <- voiceStates.reads(js)
      channels <- channels.reads(js)
      presences <- presences.reads(js)
    } yield GatewayEvents.Guild(id, name, icon, splash, ownerId, region, afkChannelId, afkTimeout, embedEnabled, embedChannelId, verificationLevel, defaultMessageNotifications, explicitContentFilter, roles, emojis, features, mfaLevel, applicationId, widgetEnabled, widgetChannelId, joinedAt, large, unavailable, memberCount, members, voiceStates, channels, presences)
    override def writes(g: GatewayEvents.Guild) = id.writes(g.id) ++ name.writes(g.name) ++ icon.writes(g.icon) ++ splash.writes(g.splash) ++ ownerId.writes(g.ownerId) ++ region.writes(g.region) ++ afkChannelId.writes(g.afkChannelId) ++ afkTimeout.writes(g.afkTimeout) ++ embedEnabled.writes(g.embedEnabled) ++ embedChannelId.writes(g.embedChannelId) ++ verificationLevel.writes(g.verificationLevel) ++ defaultMessageNotifications.writes(g.defaultMessageNotifications) ++ explicitContentFilter.writes(g.explicitContentFilter) ++ roles.writes(g.roles) ++ emojis.writes(g.emojis) ++ features.writes(g.features) ++ mfaLevel.writes(g.mfaLevel) ++ applicationId.writes(g.applicationId) ++ widgetEnabled.writes(g.widgetEnabled) ++ widgetChannelId.writes(g.widgetChannelId) ++ joinedAt.writes(g.joinedAt) ++ large.writes(g.large) ++ unavailable.writes(g.unavailable) ++ memberCount.writes(g.memberCount) ++ members.writes(g.members) ++ voiceStates.writes(g.voiceStates) ++ channels.writes(g.channels) ++ presences.writes(g.presences)
  }
  implicit val readyGuildFormat: Format[UnavailableGuild Either GatewayEvents.Guild] = eitherFormat
  implicit val readyFormat: Format[GatewayEvents.Ready] = (
    (__ \ "v").format[Int] and
    (__ \ "user").format[User] and
    (__ \ "private_channels").format[Array[Channel]] and
    (__ \ "guilds").format[Array[UnavailableGuild Either GatewayEvents.Guild]] and
    (__ \ "user_settings").format[Option[UserSettings]] and
    (__ \ "read_state").formatWithDefault[Array[GatewayEvents.ReadState]](Array.empty) and
    (__ \ "_trace").format[Array[String]]
  )(GatewayEvents.Ready.apply, unlift(GatewayEvents.Ready.unapply))
  
  implicit val guildEmojisUpdateFormat: Format[GatewayEvents.GuildEmojisUpdate] = Json.format
  implicit val guildIntegrationUpdateFormat: Format[GatewayEvents.GuildIntegrationUpdate] = Json.format
  implicit val guildMemberRemoveFormat: Format[GatewayEvents.GuildMemberRemove] = Json.format
  implicit val guildMemberUpdateFormat: Format[GatewayEvents.GuildMemberUpdate] = Json.format
  implicit val guildMemberChunkFormat: Format[GatewayEvents.GuildMemberChunk] = Json.format
  implicit val guildRoleCreateFormat: Format[GatewayEvents.GuildRoleCreate] = Json.format
  implicit val guildRoleUpdateFormat: Format[GatewayEvents.GuildRoleUpdate] = Json.format
  implicit val guildRoleDeleteFormat: Format[GatewayEvents.GuildRoleDelete] = Json.format
  implicit val messageDeleteFormat: Format[GatewayEvents.MessageDelete] = Json.format
  implicit val messageDeleteBulkFormat: Format[GatewayEvents.MessageDeleteBulk] = Json.format
  implicit val emojiReferenceFormat: Format[GatewayEvents.EmojiReference] = Json.format
  implicit val messageReactionFormat: Format[GatewayEvents.MessageReaction] = Json.format
  implicit val messageReactionRemoveAllFormat: Format[GatewayEvents.MessageReactionRemoveAll] = Json.format
  implicit val presenceUserFormat: Format[GatewayEvents.PresenceUser] = Json.format
  implicit val presenceUpdateFormat: Format[GatewayEvents.PresenceUpdate] = Json.format
  implicit val typingStartFormat: Format[GatewayEvents.TypingStart] = Json.format
  implicit val voiceServerUpdateFormat: Format[GatewayEvents.VoiceServerUpdate] = Json.format
  
}