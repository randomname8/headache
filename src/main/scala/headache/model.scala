package headache

import enumeratum.values.{IntEnum, IntEnumEntry, StringEnum, StringEnumEntry}
import java.time.Instant

object Snowflake {
  def apply(s: String) = java.lang.Long.parseUnsignedLong(s).asInstanceOf[Snowflake]
  def apply(s: Long) = s.asInstanceOf[Snowflake]
  final val NoSnowflake = Snowflake(-1)
}

object PermissionBits {
  def apply(s: String) = java.lang.Long.parseUnsignedLong(s).asInstanceOf[PermissionBits]
  def apply(s: Long) = s.asInstanceOf[PermissionBits]
  final val NoPermissions = PermissionBits(0)
}


case class User(
  id: Snowflake,
  userName: Option[String],
  discriminator: Option[String],
  avatar: Option[String],
  bot: Boolean = false,
  system: Boolean = false,
  mfaEnabled: Option[Boolean],
  locale: Option[String],
  verified: Option[Boolean],
  email: Option[String],
  flags: Option[Int],
  premiumType: Option[Int],
  publicFlags: Option[Int],
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
  user: Option[User],
  nick: Option[String],
  roles: Array[Snowflake],
  joinedAt: Instant,
  premiumSince: Option[Instant],
  deaf: Boolean,
  mute: Boolean,
  pending: Option[Boolean],
  permissions: Option[String]
)

object Activity {
  sealed abstract class Type(val value: Int) extends IntEnumEntry
  object Type extends IntEnum[Type] {
    val values = findValues
    case object Playing extends Type(0)
    case object Streaming extends Type(1)
    case object Listening extends Type(2)
    case object Watching extends Type(3)
    case class Unk(override val value: Int) extends Type(value)
  }
  case class Timestamps(
    start: Option[Instant] = None,
    end: Option[Instant] = None
  )
  case class Assets(
    largeImage: Option[String] = None,
    largeText: Option[String] = None,
    smallImage: Option[String] = None,
    smallText: Option[String]
  )
  case class Party(
    id: Option[String] = None,
    size: Option[Array[Int]] = None
  )
  case class Emoji(name: String, id: Option[Snowflake] = None, animated: Option[Boolean] = None)
  case class Secrets(join: Option[String] = None, spectate: Option[String] = None, `match`: Option[String] = None)
}
case class Activity(
  name: String,
  tpe: Option[Activity.Type] = None,
  url: Option[String] = None,
  createdAt: Long = System.currentTimeMillis() / 1000,
  timestamps: Option[Activity.Timestamps] = None,
  applicationId: Option[Snowflake] = None,
  state: Option[String] = None,
  details: Option[String] = None,
  assets: Option[Activity.Assets] = None,
  emoji: Option[Activity.Emoji] = None,
  party: Option[Activity.Party] = None,
  secrets: Option[Activity.Secrets] = None,
  flags: Option[Long] = None
)

case class GuildPresence(
  user: User,
  roles: Option[Array[Snowflake]],
  game: Option[Activity],
  guildId: Option[Snowflake],
  nick: Option[String],
  activities: Array[Activity],
  clientStatus: ClientStatus,
  premiumSince: Option[Instant],
  status: String
)
// case class PresenceUser(val id: String)
case class ClientStatus(desktop: Option[String], mobile: Option[String], web: Option[String])

case class Role(
  id: Snowflake,
  name: String,
  color: Int,
  hoist: Boolean,
  position: Int,
  permissions: PermissionBits,
  managed: Boolean,
  mentionable: Boolean,
  //icon ?
  //unicodeEmoji ?
)

case class Emoji(
  id: Option[String],
  name: Option[String],
  roles: Array[Role],
  user: Option[User],
  requireColons: Option[Boolean],
  managed: Option[Boolean],
  animated: Option[Boolean],
  avaialable: Option[Boolean],
)

case class VoiceState(
  guildId: Option[Snowflake],
  channelId: Option[Snowflake],
  userId: Snowflake,
  member: Option[GuildMember],
  sessionId: String,
  deaf: Boolean,
  mute: Boolean,
  selfDeaf: Boolean,
  selfMute: Boolean,
  selfStream: Boolean = false,
  selfVideo: Boolean = false,
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

case class PermissionOverwrite(id: Snowflake, tpe: PermissionOverwrite.Type, allow: PermissionBits, deny: PermissionBits)
object PermissionOverwrite {
  sealed abstract class Type(val value: Int) extends IntEnumEntry
  object Type extends IntEnum[Type] {
    val values = findValues
    object Role extends Type(0)
    object Member extends Type(1)
    case class Unk(override val value: Int) extends Type(value)
  }
}

case class Reaction(count: Int, me: Boolean, emoji: Emoji)

object Message {
  sealed abstract class Type(val value: Int) extends IntEnumEntry
  object Type extends IntEnum[Type] {
    val values = findValues
    case object Default extends Type(0)
    case object RecipientAdd extends Type(1)
    case object RecipientRemove extends Type(2)
    case object Call extends Type(3)
    case object ChannelNameChange extends Type(4)
    case object ChannelIconChange extends Type(5)
    case object ChannelPinnedMessage extends Type(6)
    case object GuidMemberJoin extends Type(7)
    case object UsePremiumGuildSubscription extends Type(8)
    case object UsePremiumGuildSubscriptionTier1 extends Type(9)
    case object UsePremiumGuildSubscriptionTier2 extends Type(10)
    case object UsePremiumGuildSubscriptionTier3 extends Type(11)
    case object ChannelFollowAdd extends Type(12)
    case object GuildDiscoveryDisqualified extends Type(14)
    case object GuildDiscoveryRequalified extends Type(15)
    case class Unk(override val value: Int) extends Type(value)
  }
  case class Activity(tpe: Activity.Type, partyId: Option[String])
  object Activity {
    sealed abstract class Type(val value: Int) extends IntEnumEntry
    object Type extends IntEnum[Type] {
      val values = findValues
      case object Join extends Type(1)
      case object Spectate extends Type(2)
      case object Listen extends Type(3)
      case object JoinRequest extends Type(5)
      case class Unk(override val value: Int) extends Type(value)
    }
  }
  case class Application(id: Snowflake, coverImage: Option[String], description: String, icon: Option[String], name: String)
  case class Reference(messageId: Option[Snowflake], channelId: Snowflake, guildId: Option[Snowflake])
}
case class Message(
  id: Snowflake,
  channelId: Snowflake,
  guildId: Option[Snowflake],
  author: User,
  member: Option[GuildMember],
  content: String,
  timestamp: Instant,
  editedTimestamp: Option[Instant] = None,
  tts: Boolean,
  mentionEveryone: Boolean,
  mentions: Array[User],
  mentionRoles: Array[String],
  mentionChannels: Array[Channel] = Array.empty,
  attachments: Array[Attachment] = Array.empty,
  embeds: Array[Embed] = Array.empty,
  reactions: Array[Reaction] = Array.empty,
  nonce: Option[String] = None,
  pinned: Boolean,
  webhookId: Option[Snowflake] = None,
  tpe: Message.Type,
  activity: Option[Message.Activity],
  application: Option[Message.Application],
  messageReference: Option[Message.Reference],
  fags: Option[Long]
)
// case class MessageUpdate(
//   id: Snowflake,
//   channelId: Snowflake,
//   content: Option[String] = None,
//   editedTimestamp: Option[Instant] = None,
//   tts: Option[Boolean] = None,
//   mentionEveryone: Option[Boolean] = None,
//   mentions: Array[User] = Array.empty,
//   mentionRoles: Array[String] = Array.empty,
//   attachments: Array[Attachment] = Array.empty,
//   embeds: Array[Embed] = Array.empty,
//   nonce: Option[String] = None,
//   pinned: Option[Boolean] = None,
//   webhookId: Option[String] = None
// )

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
  case class Listening(to: String) extends Status
  case object Empty extends Status
}

sealed abstract class PresenceState(val value: String) extends StringEnumEntry
object PresenceState extends StringEnum[PresenceState] {
  val values = findValues
  case object Online extends PresenceState("online")
  case object Dnd extends PresenceState("dnd")
  case object Idle extends PresenceState("idle")
  case object Offline extends PresenceState("offline")
}

case class AudioRtpFrame(rtpSequence: Int, rtpTimestamp: Int, rtpSsrc: Int, audio: Array[Byte])