package headache

import GatewayEvents._

/**
 * Pattern matching bridge over the gateway events for Java. Intended usage for the {{{onGatewayEvent}}} method
 */
abstract class EventVisitor extends (GatewayEvents.GatewayEvent => Any) {
  val implementedEvents = EventVisitor.getImplementedEvents(this)
  final def apply(evt: GatewayEvents.GatewayEvent): Any = {
    if (implementedEvents(evt.tpe)) {
      evt.tpe match {
        case GatewayEvents.EventType.Ready => onReadyEvent(ReadyEvent.unapply(evt).get)
        case GatewayEvents.EventType.Resumed => onResumedEvent()
        case GatewayEvents.EventType.ChannelCreate => onChannelCreateEvent(ChannelCreateEvent.unapply(evt).get)
        case GatewayEvents.EventType.ChannelUpdate => onChannelUpdateEvent(ChannelUpdateEvent.unapply(evt).get)
        case GatewayEvents.EventType.ChannelDelete => onChannelDeleteEvent(ChannelDeleteEvent.unapply(evt).get)
        case GatewayEvents.EventType.GuildCreate => onGuildCreateEvent(GuildCreateEvent.unapply(evt).get)
        case GatewayEvents.EventType.GuildUpdate => onGuildUpdateEvent(GuildUpdateEvent.unapply(evt).get)
        case GatewayEvents.EventType.GuildDelete => onGuildDeleteEvent(GuildDeleteEvent.unapply(evt).get)
        case GatewayEvents.EventType.GuildBanAdd => onGuildBanAddEvent(GuildBanAddEvent.unapply(evt).get)
        case GatewayEvents.EventType.GuildBanRemove => onGuildBanRemoveEvent(GuildBanRemoveEvent.unapply(evt).get)
        case GatewayEvents.EventType.GuildEmojisUpdate => onGuildEmojisUpdateEvent(GuildEmojisUpdateEvent.unapply(evt).get)
        case GatewayEvents.EventType.GuildIntegrationUpdate => onGuildIntegrationUpdateEvent(GuildIntegrationUpdateEvent.unapply(evt).get)
        case GatewayEvents.EventType.GuildMemberAdd => onGuildMemberAddEvent(GuildMemberAddEvent.unapply(evt).get)
        case GatewayEvents.EventType.GuildMemberRemove => onGuildMemberRemoveEvent(GuildMemberRemoveEvent.unapply(evt).get)
        case GatewayEvents.EventType.GuildMemberUpdate => onGuildMemberUpdateEvent(GuildMemberUpdateEvent.unapply(evt).get)
        case GatewayEvents.EventType.GuildMemberChunk => onGuildMemberChunkEvent(GuildMemberChunkEvent.unapply(evt).get)
        case GatewayEvents.EventType.GuildRoleCreate => onGuildRoleCreateEvent(GuildRoleCreateEvent.unapply(evt).get)
        case GatewayEvents.EventType.GuildRoleUpdate => onGuildRoleUpdateEvent(GuildRoleUpdateEvent.unapply(evt).get)
        case GatewayEvents.EventType.GuildRoleDelete => onGuildRoleDeleteEvent(GuildRoleDeleteEvent.unapply(evt).get)
        case GatewayEvents.EventType.MessageCreate => onMessageCreateEvent(MessageCreateEvent.unapply(evt).get)
        case GatewayEvents.EventType.MessageUpdate => onMessageUpdateEvent(MessageUpdateEvent.unapply(evt).get)
        case GatewayEvents.EventType.MessageDelete => onMessageDeleteEvent(MessageDeleteEvent.unapply(evt).get)
        case GatewayEvents.EventType.MessageDeleteBulk => onMessageDeleteBulkEvent(MessageDeleteBulkEvent.unapply(evt).get)
        case GatewayEvents.EventType.MessageReactionAdd => onMessageReactionAddEvent(MessageReactionAddEvent.unapply(evt).get)
        case GatewayEvents.EventType.MessageReactionRemove => onMessageReactionRemoveEvent(MessageReactionRemoveEvent.unapply(evt).get)
        case GatewayEvents.EventType.MessageReactionRemoveAll => onMessageReactionRemoveAllEvent(MessageReactionRemoveAllEvent.unapply(evt).get)
        case GatewayEvents.EventType.PresenceUpdate => onPresenceUpdateEvent(PresenceUpdateEvent.unapply(evt).get)
        case GatewayEvents.EventType.TypingStart => onTypingStartEvent(TypingStartEvent.unapply(evt).get)
        case GatewayEvents.EventType.UserUpdate => onUserUpdateEvent(UserUpdateEvent.unapply(evt).get)
        case GatewayEvents.EventType.VoiceStateUpdate => onVoiceStateUpdateEvent(VoiceStateUpdateEvent.unapply(evt).get)
        case GatewayEvents.EventType.VoiceServerUpdate => onVoiceServerUpdateEvent(VoiceServerUpdateEvent.unapply(evt).get)
        case GatewayEvents.EventType.Unknown => 
      }
    }
  }
  
  def onReadyEvent(evt: Ready): Unit = {}
  def onResumedEvent(): Unit = {}
  def onChannelCreateEvent(evt: ChannelCreate): Unit = {}
  def onChannelUpdateEvent(evt: ChannelUpdate): Unit = {}
  def onChannelDeleteEvent(evt: ChannelDelete): Unit = {}
  def onGuildCreateEvent(evt: GuildCreate): Unit = {}
  def onGuildUpdateEvent(evt: GuildUpdate): Unit = {}
  def onGuildDeleteEvent(evt: GuildDelete): Unit = {}
  def onGuildBanAddEvent(evt: GuildBanAdd): Unit = {}
  def onGuildBanRemoveEvent(evt: GuildBanRemove): Unit = {}
  def onGuildEmojisUpdateEvent(evt: GuildEmojisUpdate): Unit = {}
  def onGuildIntegrationUpdateEvent(evt: GuildIntegrationUpdate): Unit = {}
  def onGuildMemberAddEvent(evt: GuildMemberAdd): Unit = {}
  def onGuildMemberUpdateEvent(evt: GuildMemberUpdate): Unit = {}
  def onGuildMemberRemoveEvent(evt: GuildMemberRemove): Unit = {}
  def onGuildMemberChunkEvent(evt: GuildMemberChunk): Unit = {}
  def onGuildRoleCreateEvent(evt: GuildRoleCreate): Unit = {}
  def onGuildRoleUpdateEvent(evt: GuildRoleUpdate): Unit = {}
  def onGuildRoleDeleteEvent(evt: GuildRoleDelete): Unit = {}
  def onMessageCreateEvent(evt: GatewayEvents.MessageCreate): Unit = {}
  def onMessageUpdateEvent(evt: GatewayEvents.MessageUpdate): Unit = {}
  def onMessageDeleteEvent(evt: GatewayEvents.MessageDelete): Unit = {}
  def onMessageDeleteBulkEvent(evt: MessageDeleteBulk): Unit = {}
  def onMessageReactionAddEvent(evt: MessageReaction): Unit = {}
  def onMessageReactionRemoveEvent(evt: MessageReaction): Unit = {}
  def onMessageReactionRemoveAllEvent(evt: MessageReactionRemoveAll): Unit = {}
  def onPresenceUpdateEvent(evt: PresenceUpdate): Unit = {}
  def onTypingStartEvent(evt: TypingStart): Unit = {}
  def onUserUpdateEvent(evt: UserUpdate): Unit = {}
  def onVoiceStateUpdateEvent(evt: VoiceStateUpdate): Unit = {}
  def onVoiceServerUpdateEvent(evt: VoiceServerUpdate): Unit = {}
}
object EventVisitor {
  private val methods = classOf[EventVisitor].getDeclaredMethods.map(m => m.getName -> m).toMap
  
  private[EventVisitor] def getImplementedEvents(instance: EventVisitor): Set[GatewayEvents.EventType] = {
    val instanceMethods = instance.getClass.getMethods.filterNot(_.getDeclaringClass eq classOf[EventVisitor]).groupBy(_.getName)
    def testMethod(name: String, eventType: GatewayEvents.EventType): Option[GatewayEvents.EventType] = {
      for {
        alternatives <- instanceMethods.get(name)
        if alternatives.exists(possible => methods.get(possible.getName).filter(original =>
            (possible.getReturnType eq original.getReturnType) && possible.getParameterTypes.sameElements(original.getParameterTypes)).isDefined)
      } yield eventType
    }
    
    (
      testMethod("onReadyEvent", GatewayEvents.EventType.Ready) ++ 
      testMethod("onResumedEvent", GatewayEvents.EventType.Resumed) ++
      testMethod("onChannelCreateEvent", GatewayEvents.EventType.ChannelCreate) ++
      testMethod("onChannelUpdateEvent", GatewayEvents.EventType.ChannelUpdate) ++
      testMethod("onChannelDeleteEvent", GatewayEvents.EventType.ChannelDelete) ++
      testMethod("onGuildCreateEvent", GatewayEvents.EventType.GuildCreate) ++
      testMethod("onGuildUpdateEvent", GatewayEvents.EventType.GuildUpdate) ++
      testMethod("onGuildDeleteEvent", GatewayEvents.EventType.GuildDelete) ++
      testMethod("onGuildBanAddEvent", GatewayEvents.EventType.GuildBanAdd) ++
      testMethod("onGuildBanRemoveEvent", GatewayEvents.EventType.GuildBanRemove) ++
      testMethod("onGuildEmojisUpdateEvent", GatewayEvents.EventType.GuildEmojisUpdate) ++
      testMethod("onGuildIntegrationUpdateEvent", GatewayEvents.EventType.GuildIntegrationUpdate) ++
      testMethod("onGuildMemberAddEvent", GatewayEvents.EventType.GuildMemberAdd) ++
      testMethod("onGuildMemberRemoveEvent", GatewayEvents.EventType.GuildMemberRemove) ++
      testMethod("onGuildMemberUpdateEvent", GatewayEvents.EventType.GuildMemberUpdate) ++
      testMethod("onGuildMemberChunkEvent", GatewayEvents.EventType.GuildMemberChunk) ++
      testMethod("onGuildRoleCreateEvent", GatewayEvents.EventType.GuildRoleCreate) ++
      testMethod("onGuildRoleUpdateEvent", GatewayEvents.EventType.GuildRoleUpdate) ++
      testMethod("onGuildRoleDeleteEvent", GatewayEvents.EventType.GuildRoleDelete) ++
      testMethod("onMessageCreateEvent", GatewayEvents.EventType.MessageCreate) ++
      testMethod("onMessageUpdateEvent", GatewayEvents.EventType.MessageUpdate) ++
      testMethod("onMessageDeleteEvent", GatewayEvents.EventType.MessageDelete) ++
      testMethod("onMessageDeleteBulkEvent", GatewayEvents.EventType.MessageDeleteBulk) ++
      testMethod("onMessageReactionAddEvent", GatewayEvents.EventType.MessageReactionAdd) ++
      testMethod("onMessageReactionRemoveEvent", GatewayEvents.EventType.MessageReactionRemove) ++
      testMethod("onMessageReactionRemoveAllEvent", GatewayEvents.EventType.MessageReactionRemoveAll) ++
      testMethod("onPresenceUpdateEvent", GatewayEvents.EventType.PresenceUpdate) ++
      testMethod("onTypingStartEvent", GatewayEvents.EventType.TypingStart) ++
      testMethod("onUserUpdateEventEvent", GatewayEvents.EventType.UserUpdate) ++
      testMethod("onVoiceStateUpdateEvent", GatewayEvents.EventType.VoiceStateUpdate) ++
      testMethod("onVoiceServerUpdateEvent", GatewayEvents.EventType.VoiceServerUpdate) ++
      None
    ).toSet
  }
}