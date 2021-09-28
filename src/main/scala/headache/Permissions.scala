package headache

import enumeratum.values.{LongEnum, LongEnumEntry}
import scala.annotation.varargs

sealed abstract class Permission(val value: Long) extends LongEnumEntry {
  @inline def existsIn(l: Long) = (l & value) == value
}
object Permissions extends LongEnum[Permission] {
  val values = findValues
  case object CreateInstantInvite     extends Permission(0x0000000001L)
  case object KickMembers             extends Permission(0x0000000002L)
  case object BanMembers              extends Permission(0x0000000004L)
  case object Administrator           extends Permission(0x0000000008L)
  case object ManageChannels          extends Permission(0x0000000010L)
  case object ManageGuilds            extends Permission(0x0000000020L)
  case object AddReactions            extends Permission(0x0000000040L)
  case object ViewAuditLogs           extends Permission(0x0000000080L)
  case object PrioritySpeaker         extends Permission(0x0000000100L)
  case object Stream                  extends Permission(0x0000000200L)
  case object ViewChannels            extends Permission(0x0000000400L)
  case object SendMessages            extends Permission(0x0000000800L)
  case object SendTtsMessages         extends Permission(0x0000001000L)
  case object ManageMessages          extends Permission(0x0000002000L)
  case object EmbedLinks              extends Permission(0x0000004000L)
  case object AttachFiles             extends Permission(0x0000008000L)
  case object ReadMessageHistory      extends Permission(0x0000010000L)
  case object MentionEveryone         extends Permission(0x0000020000L)
  case object UseExternalEmojis       extends Permission(0x0000040000L)
  case object ViewGuildInsights       extends Permission(0x0000080000L)
  case object Connect                 extends Permission(0x0000100000L)
  case object Speak                   extends Permission(0x0000200000L)
  case object MuteMembers             extends Permission(0x0000400000L)
  case object DeafenMembers           extends Permission(0x0000800000L)
  case object MoveMembers             extends Permission(0x0001000000L)
  case object UseVad                  extends Permission(0x0002000000L)
  case object ChangeNickname          extends Permission(0x0004000000L)
  case object ManageNicknames         extends Permission(0x0008000000L)
  case object ManageRoles             extends Permission(0x0010000000L)
  case object ManageWebhooks          extends Permission(0x0020000000L)
  case object ManageEmojisAndStickers extends Permission(0x0040000000L)
  case object UseApplicationCommands  extends Permission(0x0080000000L)
  case object RequestToSpeak          extends Permission(0x0100000000L)
  case object ManageThreads           extends Permission(0x0400000000L)
  case object CreatePublicThreads     extends Permission(0x0800000000L)
  case object CreatePrivateThreads    extends Permission(0x1000000000L)
  case object UseExternalStickers     extends Permission(0x2000000000L)
  case object SendMessagesInThreads   extends Permission(0x4000000000L)
  case object StartEmbeddedActivities extends Permission(0x8000000000L)
  
  val createInstantInvite     = CreateInstantInvite 
  val kickMembers             = KickMembers
  val banMembers              = BanMembers
  val administrator           = Administrator
  val manageChannels          = ManageChannels
  val manageGuilds            = ManageGuilds
  val addReactions            = AddReactions
  val viewAuditLogs           = ViewAuditLogs
  val viewChannels            = ViewChannels
  val sendMessages            = SendMessages
  val sendTtsMessages         = SendTtsMessages
  val manageMessages          = ManageMessages
  val embedLinks              = EmbedLinks
  val attachFiles             = AttachFiles
  val readMessageHistory      = ReadMessageHistory
  val mentionEveryone         = MentionEveryone
  val useExternalEmojis       = UseExternalEmojis
  val connect                 = Connect
  val speak                   = Speak
  val muteMembers             = MuteMembers
  val deafenMembers           = DeafenMembers
  val moveMembers             = MoveMembers
  val useVad                  = UseVad
  val changeNickname          = ChangeNickname
  val manageNicknames         = ManageNicknames
  val manageRoles             = ManageRoles
  val manageWebhooks          = ManageWebhooks
  val manageEmojis            = ManageEmojisAndStickers
  val useApplicationCommands  = UseApplicationCommands
  val requestToSpeak          = RequestToSpeak
  val manageThreads           = ManageThreads
  val createPublicThreads     = CreatePublicThreads
  val createPrivateThreads    = CreatePrivateThreads
  val useExternalStickers     = UseExternalStickers
  val sendMessagesInThreads   = SendMessagesInThreads
  val startEmbeddedActivities = StartEmbeddedActivities
  
  
  @inline def from(permissions: Long): Seq[Permission] = values.filter(p => (permissions & p.value) == p.value)
  @varargs @inline def compact(permissions: Permission*): Long = permissions.foldLeft(0L)(_ | _.value)
  
  
  private val allPerms: Long = compact(values:_*)
  
  /** @see overloaded definition of this method */
  def calculateFinalPermissions(everyonesRolePermissions: Seq[Permission], appliedRolesPermissions: Seq[Seq[Permission]],
                                overrides: Seq[PermissionOverwrite]): Seq[Permission] = {
    from(calculateFinalPermissions(compact(everyonesRolePermissions:_*), appliedRolesPermissions.map(s => compact(s:_*)).toArray,
                                   overrides.toArray))
  }
  /**
   * Calculate final permissions for an entity.
   *
   * @param everyonesRolePermissions The permissions specified for everyone.
   * @param appliedRolesPermissions The particular permissions assigned via roles to the entity
   * @param overrides List of overrides for the entity in order, it should contain first the override for the everyone role (since it applies to everyone),
   *                  then the overrides for the roles the entity has, and finally, the list of overrides specifically for the entity. In pseudocode
   *                  `everyoneOverrides ++ entity.roles.map(getOverrides) ++ getOverridesFor(entity)`
   * @return the calculated final permissions for the entity
   */
  def calculateFinalPermissions(everyonesRolePermissions: Long, appliedRolesPermissions: Array[Long], overrides: Array[PermissionOverwrite]): Long = {
    val basePerms = appliedRolesPermissions.foldLeft(everyonesRolePermissions)(_ | _)
    
    if ((basePerms | Administrator.value) == Administrator.value) allPerms
    else overrides.foldLeft(basePerms)((res, overwrite) => (res & ~overwrite.deny) | overwrite.allow)
  }
}
