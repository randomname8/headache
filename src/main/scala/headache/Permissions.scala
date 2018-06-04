package headache

import enumeratum.values.{LongEnum, LongEnumEntry}

sealed abstract class Permission(val value: Long) extends LongEnumEntry
object Permissions extends LongEnum[Permission] {
  val values = findValues
  object CreateInstantInvite extends Permission(0x00000001l)
  object KickMembers         extends Permission(0x00000002l)
  object BanMembers          extends Permission(0x00000004l)
  object Administrator       extends Permission(0x00000008l)
  object ManageChannels      extends Permission(0x00000010l)
  object ManageGuilds        extends Permission(0x00000020l)
  object AddReactions        extends Permission(0x00000040l)
  object ViewAuditLogs       extends Permission(0x00000080l)
  object ViewChannels        extends Permission(0x00000400l)
  object SendMessages        extends Permission(0x00000800l)
  object SendTtsMessages     extends Permission(0x00001000l)
  object ManageMessages      extends Permission(0x00002000l)
  object EmbedLinks          extends Permission(0x00004000l)
  object AttachFiles         extends Permission(0x00008000l)
  object ReadMessageHistory  extends Permission(0x00010000l)
  object MentionEveryone     extends Permission(0x00020000l)
  object UseExternalEmojis   extends Permission(0x00040000l)
  object Connect             extends Permission(0x00100000l)
  object Speak               extends Permission(0x00200000l)
  object MuteMembers         extends Permission(0x00400000l)
  object DeafenMembers       extends Permission(0x00800000l)
  object MoveMembers         extends Permission(0x01000000l)
  object UseVad              extends Permission(0x02000000l)
  object ChangeNickname      extends Permission(0x04000000l)
  object ManageNicknames     extends Permission(0x08000000l)
  object ManageRoles         extends Permission(0x10000000l)
  object ManageWebhooks      extends Permission(0x20000000l)
  object ManageEmojis        extends Permission(0x40000000l)
  
  
  def from(permissions: Long): Seq[Permission] = values.filter(p => (permissions & p.value) == p.value)
}
