package headache

import enumeratum.values.{IntEnum, IntEnumEntry, StringEnum, StringEnumEntry}
import java.time.Instant

case class User(
  id: Snowflake,
  userName: String,
  discriminator: String,
  avatar: Option[String],
  bot: Boolean = false,
  mfaEnabled: Option[Boolean],
  verified: Option[Boolean],
  email: Option[String],
  premium: Option[Boolean]
)

case class UserSettings(
  timezoneOffset: Int, //minutes
  theme: String,
  status: String,
  showCurrentGame: Boolean,
  renderReactions: Boolean,
  renderEmbeds: Boolean,
  messageDisplayCompact: Boolean,
  locale: String,
  inlineEmbedMedia: Boolean,
  inlineAttachmentMedia: Boolean,
  gifAutoPlay: Boolean,
  guildPositions: Array[Snowflake],
  explicitContentFilter: Int,
  convertEmoticons: Boolean,
  animateEmoji: Boolean,
  afkTimeout: Int
)

sealed abstract class ExplicitContentFilterLevel(val value: Int) extends IntEnumEntry
object ExplicitContentFilterLevel extends IntEnum[ExplicitContentFilterLevel] {
  val values = findValues
  case object Disabled extends ExplicitContentFilterLevel(0)
  case object MembersWithoutRoles extends ExplicitContentFilterLevel(1)
  case object AllMembers extends ExplicitContentFilterLevel(2)
  case class Unk(override val value: Int) extends ExplicitContentFilterLevel(value)
}
sealed abstract class NotificationLevel(val value: Int) extends IntEnumEntry
object NotificationLevel extends IntEnum[NotificationLevel] {
  val values = findValues
  case object AllMessages extends NotificationLevel(0)
  case object OnlyMentions extends NotificationLevel(1)
  case class Unk(override val value: Int) extends NotificationLevel(value)
}
trait GuildDef {
  def id: Snowflake
  def name: String
  def icon: Option[String]
  def splash: Option[String]
  def ownerId: Snowflake
  def region: String
  def afkChannelId: Option[Snowflake]
  def afkTimeout: Int
  def embedEnabled: Option[Boolean]
  def embedChannelId: Option[Snowflake]
  def verificationLevel: Int
  def defaultMessageNotifications: NotificationLevel
  def explicitContentFilter: ExplicitContentFilterLevel
  def roles: Array[Role]
  def emojis: Array[Emoji]
  def features: Array[String]
  def mfaLevel: Int
  def applicationId: Option[Snowflake]
  def widgetEnabled: Option[Boolean]
  def widgetChannelId: Option[Snowflake]
}
case class Guild(
  id: Snowflake,
  name: String,
  icon: Option[String],
  splash: Option[String],
  ownerId: Snowflake,
  region: String,
  afkChannelId: Option[Snowflake],
  afkTimeout: Int,
  embedEnabled: Option[Boolean],
  embedChannelId: Option[Snowflake],
  verificationLevel: Int,
  defaultMessageNotifications: NotificationLevel,
  explicitContentFilter: ExplicitContentFilterLevel,
  roles: Array[Role],
  emojis: Array[Emoji],
  features: Array[String],
  mfaLevel: Int,
  applicationId: Option[Snowflake],
  widgetEnabled: Option[Boolean],
  widgetChannelId: Option[Snowflake]
) extends GuildDef

case class UnavailableGuild(id: String, unavailable: Boolean)

case class GuildMember(
  user: User,
  nick: Option[String],
  roles: Array[Snowflake],
  joinedAt: Instant,
  deaf: Boolean,
  mute: Boolean
)

object GameStatus {
  sealed abstract class Type(val value: Int) extends IntEnumEntry
  object Type extends IntEnum[Type] {
    val values = findValues
    case object Playing extends Type(0)
    case object Streaming extends Type(1)
    case object Listening extends Type(2)
    case object Watching extends Type(3)
    case class Unk(override val value: Int) extends Type(value)
  }
  case class Timestamps(start: Option[Instant], end: Option[Instant])
  case class Assets(largeImage: Option[String], largeText: Option[String], smallImage: Option[String], smallText: Option[String])
}
case class GameStatus(
  name: String,
  tpe: Option[GameStatus.Type],
  url: Option[String],
  timestamps: Option[GameStatus.Timestamps],
  applicationId: Option[Snowflake],
  state: Option[String],
  details: Option[String],
  assets: Option[GameStatus.Assets],
)

case class GuildPresence(
  user: PresenceUser,
  game: Option[GameStatus],
  nick: Option[String],
  status: String
)
case class PresenceUser(val id: String)

case class Role(
  id: Snowflake,
  name: String,
  color: Int,
  hoist: Boolean,
  position: Int,
  permissions: Long,
  managed: Boolean,
  mentionable: Boolean
)

case class Emoji(
  id: String,
  name: String,
  roles: Array[Role],
  requireColons: Boolean,
  managed: Boolean
)

case class VoiceState(
  guildId: Option[Snowflake],
  channelId: Option[Snowflake],
  userId: Snowflake,
  deaf: Boolean,
  mute: Boolean,
  selfDeaf: Boolean,
  selfMute: Boolean,
  suppress: Boolean
)

object Channel {
  sealed abstract class Type(val value: Int) extends IntEnumEntry
  object Type extends IntEnum[Type] {
    val values = findValues
    case object GuildText extends Type(0)
    case object Dm extends Type(1)
    case object GuildVoice extends Type(2)
    case object GroupDm extends Type(3)
    case object GuildCategory extends Type(4)
    case class Unk(override val value: Int) extends Type(value)
  }
}

case class Channel(
  id: Snowflake,
  tpe: Channel.Type,
  guildId: Option[Snowflake] = None,
  name: Option[String] = None,
  position: Option[Int] = None,
  permissionOverwrites: Array[PermissionOverwrite] = Array.empty,
  topic: Option[String] = None,
  nsfw: Boolean = false,
  lastMessageId: Option[Snowflake],
  bitrate: Option[Int] = None,
  userLimit: Option[Int] = None,
  recipients: Array[User] = Array.empty,
  icon: Option[String] = None, //icon hash they say
  ownerId: Option[Snowflake] = None,
  applicationId: Option[Snowflake] = None,
  lastPinTimestamp: Option[String] = None,
)

case class PermissionOverwrite(id: Snowflake, tpe: PermissionOverwrite.Type, allow: Long, deny: Long)
object PermissionOverwrite {
  sealed abstract class Type(val value: String) extends StringEnumEntry
  object Type extends StringEnum[Type] {
    val values = findValues
    object Role extends Type("role")
    object Member extends Type("member")
    case class Unk(override val value: String) extends Type(value)
  }
}

case class Message(
  id: Snowflake,
  channelId: Snowflake,
  author: User,
  content: String,
  timestamp: Instant,
  editedTimestamp: Option[Instant] = None,
  tts: Boolean,
  mentionEveryone: Boolean,
  mentions: Array[User],
  mentionRoles: Array[String],
  attachments: Array[Attachment],
  embeds: Array[Embed],
  nonce: Option[String] = None,
  pinned: Boolean,
  webhookId: Option[Snowflake] = None
)
case class MessageUpdate(
  id: Snowflake,
  channelId: Snowflake,
  content: Option[String] = None,
  editedTimestamp: Option[Instant] = None,
  tts: Option[Boolean] = None,
  mentionEveryone: Option[Boolean] = None,
  mentions: Array[User] = Array.empty,
  mentionRoles: Array[String] = Array.empty,
  attachments: Array[Attachment] = Array.empty,
  embeds: Array[Embed] = Array.empty,
  nonce: Option[String] = None,
  pinned: Option[Boolean] = None,
  webhookId: Option[String] = None
)

case class Embed(
  tpe: String,
  title: Option[String] = None,
  description: Option[String] = None,
  url: Option[String] = None,
  timestamp: Option[Instant] = None,
  color: Option[Int] = None,
  footer: Option[EmbedFooter] = None,
  image: Option[EmbedImage] = None,
  thumbnail: Option[EmbedThumbnail] = None,
  video: Option[EmbedVideo] = None,
  provider: Option[EmbedProvider] = None,
  author: Option[EmbedAuthor] = None,
  fields: Array[EmbedField] = Array.empty
)
case class EmbedThumbnail(
  url: String,
  proxyUrl: String,
  height: Int,
  width: Int
)
case class EmbedVideo(
  url: String,
  height: Int,
  width: Int
)
case class EmbedImage(
  url: String,
  proxyUrl: String,
  height: Int,
  width: Int
)
case class EmbedProvider(name: String, url: Option[String] = None)
case class EmbedAuthor(
  name: String,
  url: Option[String] = None,
  iconUrl: Option[String] = None,
  proxyIconUrl: Option[String] = None
)
case class EmbedFooter(
  text: String,
  iconUrl: Option[String] = None,
  proxyIconUrl: Option[String] = None
)
case class EmbedField(
  name: String,
  value: String,
  inline: Boolean
)

case class Attachment(
  id: String,
  filename: String,
  size: Int,
  url: String,
  proxyUrl: String,
  height: Option[Int] = None,
  width: Option[Int] = None
)

case class Ban(
  reason: Option[String] = None,
  user: User
)

sealed trait Status
object Status {
  case class PlayingGame(game: String) extends Status
  case class Streaming(name: String, url: String) extends Status
  case object Empty extends Status
}

case class AudioRtpFrame(rtpSequence: Int, rtpTimestamp: Int, rtpSsrc: Int, audio: Array[Byte])