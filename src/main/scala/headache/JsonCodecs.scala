package headache

import ai.x.play.{json => jsonx}
import ai.x.play.json.implicits.optionWithNull
import enumeratum.values.{IntEnum, IntEnumEntry, StringEnumEntry, StringEnum}
import java.time.Instant
import play.api.libs.json._
import play.api.libs.functional.syntax._

object JsonCodecs {

  val NamingMapping: JsonNaming = {
    case "tpe" => "type"
    case "userName" => "username"
    case other => JsonNaming.SnakeCase(other)
  }
  implicit val jsonConf = JsonConfiguration[Json.WithDefaultValues](NamingMapping)
  implicit val encoder = new jsonx.BaseNameEncoder() {
    override def encode(s: String) = NamingMapping(s)
  }
  
  implicit object BooleanReads extends Format[Boolean] {
    override def reads(jv: JsValue) = jv match {
      case JsNull => JsSuccess(false)
      case JsBoolean(b) => JsSuccess(b)
      case _ => JsError(Seq(JsPath -> Seq(JsonValidationError("error.expected.jsboolean"))))
    }
    override def writes(b: Boolean) = JsBoolean(b)
  }
  implicit object SnowflakeReads extends Format[Snowflake] {
    override def reads(jv: JsValue) = jv match {
      case JsString(s) => JsSuccess(Snowflake(s))
      case JsNumber(l) if l.isValidLong => JsSuccess(Snowflake(l.toLong))
      case other => JsError(JsonValidationError("error.expected.snowflake"))
    }
    override def writes(s: Snowflake) = JsString(s.snowflakeString)
  }
  implicit object PermissionBitsFormat extends Format[PermissionBits] {
    override def reads(jv: JsValue) = jv match {
      case JsString(s) => JsSuccess(PermissionBits(s))
      case JsNumber(l) if l.isValidLong => JsSuccess(PermissionBits(l.toLong))
      case other => JsError(JsonValidationError("error.expected.permissionBits"))
    }
    override def writes(s: PermissionBits) = JsString(s.asPermissionsString)
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
  def intEnumFormat[T <: IntEnumEntry](missingValue: Int => T)(implicit enum: IntEnum[T]): Format[T] = new Format[T] {
    override def reads(jv: JsValue) = Reads.of[Int].map(v => enum.withValueOpt(v).getOrElse(missingValue(v))).reads(jv)
    override def writes(e: T) = JsNumber(e.value)
  }
  def stringEnumFormat[T <: StringEnumEntry](missingValue: String => T)(implicit enum: StringEnum[T]): Format[T] = new Format[T] {
    override def reads(jv: JsValue) = Reads.of[String].map(v => enum.withValueOpt(v).getOrElse(missingValue(v))).reads(jv)
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
  implicit val activityTimestampsFormat: Format[Activity.Timestamps] = Json.format
  implicit val activityAssetsFormat: Format[Activity.Assets] = Json.format
  implicit val activityTypeFormat: Format[Activity.Type] = intEnumFormat(Activity.Type.Unk)
  implicit val activityPartyFormat: Format[Activity.Party] = Json.format
  implicit val activityEmojiFormat: Format[Activity.Emoji] = Json.format
  implicit val activitySecretsFormat: Format[Activity.Secrets] = Json.format
  implicit val activityFormat: Format[Activity] = Json.format
  // implicit val guildUserFormat: Format[PresenceUser] = Json.format
  implicit val clientStatusFormat: Format[ClientStatus] = Json.format
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
  implicit val reactionFormat: Format[Reaction] = Json.format
  implicit val messageTypeFormat: Format[Message.Type] = intEnumFormat(Message.Type.Unk)
  implicit val messageActivityTypeFormat: Format[Message.Activity.Type] = intEnumFormat(Message.Activity.Type.Unk)
  implicit val messageActivityFormat: Format[Message.Activity] = Json.format
  implicit val messageApplicationFormat: Format[Message.Application] = Json.format
  implicit val messageReferenceFormat: Format[Message.Reference] = Json.format
  implicit val messageFormat: Format[Message] = jsonx.Jsonx.formatCaseClassUseDefaults
  // implicit val messageUpdateFormat: Format[MessageUpdate] = Json.format
  implicit val notificationLevelFormat = intEnumFormat[NotificationLevel](NotificationLevel.Unk)
  implicit val explicitContentFilterFormat = intEnumFormat[ExplicitContentFilterLevel](ExplicitContentFilterLevel.Unk)
  implicit val guildFormat: Format[Guild] = Json.format
  implicit val permissionOverwriteType: Format[PermissionOverwrite.Type] = intEnumFormat[PermissionOverwrite.Type](PermissionOverwrite.Type.Unk)
  implicit val overwriteFormat: Format[PermissionOverwrite] = Json.format
  implicit val channelTypeFormat: Format[Channel.Type] = intEnumFormat[Channel.Type](Channel.Type.Unk)
  implicit val channelStateFormat: Format[Channel] = Json.format
  implicit val unavailableGuildFormat: Format[UnavailableGuild] = Json.format
  
  //cached gateway events
  implicit val readStateFormat: Format[GatewayEvents.ReadState] = Json.format
  implicit val gatewayEventGuildFormat: Format[GatewayEvents.Guild] = jsonx.Jsonx.formatCaseClassUseDefaults
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