package headache

import enumeratum.values.{LongEnum, LongEnumEntry}
import scala.annotation.varargs

sealed abstract class Permission(val value: Long) extends LongEnumEntry
object Permissions extends LongEnum[Permission] {
  val values = findValues
  case object CreateInstantInvite extends Permission(0x00000001l)
  case object KickMembers         extends Permission(0x00000002l)
  case object BanMembers          extends Permission(0x00000004l)
  case object Administrator       extends Permission(0x00000008l)
  case object ManageChannels      extends Permission(0x00000010l)
  case object ManageGuilds        extends Permission(0x00000020l)
  case object AddReactions        extends Permission(0x00000040l)
  case object ViewAuditLogs       extends Permission(0x00000080l)
  case object ViewChannels        extends Permission(0x00000400l)
  case object SendMessages        extends Permission(0x00000800l)
  case object SendTtsMessages     extends Permission(0x00001000l)
  case object ManageMessages      extends Permission(0x00002000l)
  case object EmbedLinks          extends Permission(0x00004000l)
  case object AttachFiles         extends Permission(0x00008000l)
  case object ReadMessageHistory  extends Permission(0x00010000l)
  case object MentionEveryone     extends Permission(0x00020000l)
  case object UseExternalEmojis   extends Permission(0x00040000l)
  case object Connect             extends Permission(0x00100000l)
  case object Speak               extends Permission(0x00200000l)
  case object MuteMembers         extends Permission(0x00400000l)
  case object DeafenMembers       extends Permission(0x00800000l)
  case object MoveMembers         extends Permission(0x01000000l)
  case object UseVad              extends Permission(0x02000000l)
  case object ChangeNickname      extends Permission(0x04000000l)
  case object ManageNicknames     extends Permission(0x08000000l)
  case object ManageRoles         extends Permission(0x10000000l)
  case object ManageWebhooks      extends Permission(0x20000000l)
  case object ManageEmojis        extends Permission(0x40000000l)
  
  val createInstantInvite = CreateInstantInvite 
  val kickMembers         = KickMembers         
  val banMembers          = BanMembers          
  val administrator       = Administrator       
  val manageChannels      = ManageChannels      
  val manageGuilds        = ManageGuilds        
  val addReactions        = AddReactions        
  val viewAuditLogs       = ViewAuditLogs       
  val viewChannels        = ViewChannels        
  val sendMessages        = SendMessages        
  val sendTtsMessages     = SendTtsMessages     
  val manageMessages      = ManageMessages      
  val embedLinks          = EmbedLinks          
  val attachFiles         = AttachFiles         
  val readMessageHistory  = ReadMessageHistory  
  val mentionEveryone     = MentionEveryone     
  val useExternalEmojis   = UseExternalEmojis   
  val connect             = Connect             
  val speak               = Speak               
  val muteMembers         = MuteMembers         
  val deafenMembers       = DeafenMembers       
  val moveMembers         = MoveMembers         
  val useVad              = UseVad              
  val changeNickname      = ChangeNickname      
  val manageNicknames     = ManageNicknames     
  val manageRoles         = ManageRoles         
  val manageWebhooks      = ManageWebhooks      
  val manageEmojis        = ManageEmojis        
  
  
  def from(permissions: Long): Seq[Permission] = values.filter(p => (permissions & p.value) == p.value)
  @varargs def compact(permissions: Permission*): Long = permissions.foldLeft(0l)(_ | _.value)
  
  
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
