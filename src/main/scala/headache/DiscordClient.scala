package headache

import io.netty.util.HashedWheelTimer
import java.time.Instant
import java.util.Arrays
import org.asynchttpclient.{AsyncHttpClient, DefaultAsyncHttpClient, DefaultAsyncHttpClientConfig, ws}
import scala.concurrent._, duration._, ExecutionContext.Implicits._
import Json4sUtils._
import AhcUtils._

class DiscordClient(val token: String, val listener: DiscordClient.DiscordListener, val ahc: AsyncHttpClient = new DefaultAsyncHttpClient(
  new DefaultAsyncHttpClientConfig.Builder().setWebSocketMaxBufferSize(Int.MaxValue).
    setWebSocketMaxFrameSize(Int.MaxValue).build()
)) extends GatewayConnectionSupport with VoiceConnectionSupport with rest.DiscordRestApiSupport {
  import DiscordClient._, DiscordConstants._
  
  protected[headache] val timer = new HashedWheelTimer(
    { r =>
      val t = new Thread(null, r, "HashedWheelTimer", 48 * 1024)
      t.setDaemon(true)
      t
    },
    5, MILLISECONDS
  )
  def login(preferredShards: Option[Int] = None): Future[Seq[GatewayConnection]] = {
    fetchGateway flatMap {
      case (gw, shards) =>
        val expectedShards = preferredShards getOrElse shards

        Future sequence (for (s <- 0 until expectedShards) yield startShard(gw, s, expectedShards))
    }
  }
  def close(): Unit = {
    timer.stop()
  }

  def fetchGateway(): Future[(String, Int)] = request(ahc.prepareGet(GATEWAY).setHeaders(baseHeaders))(
    asDynJson.andThen { jv =>
      listener.onGatewayData(jv)
      (jv.url.extract[String] + "?encoding=json&v=6&compress=zlib-stream", jv.shards.extract)
    }
  )

  def connectToVoiceChannel(
    voiceStateUpdate: GatewayEvents.VoiceStateUpdate,
    voiceServerUpdate: GatewayEvents.VoiceServerUpdate,
    voiceConsumer: AudioRtpFrame => Unit,
    voiceProducer: () => Array[Byte]
  ): Future[VoiceConnection] = {
    val res = new VoiceConnectionImpl(voiceStateUpdate, voiceServerUpdate, voiceConsumer, voiceProducer)
    val endpoint = voiceServerUpdate.endpoint.split(":", 2)(0)
    val websocketFuture = ahc.prepareGet(s"wss://$endpoint").execute(new ws.WebSocketUpgradeHandler(Arrays.asList(res)))
    val ready = Promise[VoiceConnection]()
    websocketFuture.toCompletableFuture.handle[Unit] {
      case (_, null) =>
        ready.success(res); ()
      case (_, ex) => ready.failure(ex); ()
    }
    ready.future
  }

  /**
   * Represents an active connection to Discord
   */
  trait Connection {
    def isActive: Boolean
    def close(): Unit
    def client = DiscordClient.this
  }
  trait GatewayConnection extends Connection {
    def shardNumber: Int
    def totalShards: Int

    def sendStatusUpdate(idleSince: Option[Instant], status: Status): Unit
    def sendVoiceStateUpdate(guildId: String, channelId: Option[String], selfMute: Boolean, selfDeaf: Boolean): Unit
    def sendRequestGuildMembers(guildId: String, query: String = "", limit: Int = 0): Unit
  }

  trait VoiceConnection extends Connection {
    def guildId: String
  }
}
object DiscordClient {

  object DiscordConstants {
    /**
     * The base URL.
     */
    final val BASE = "https://discordapp.com/"

    /**
     * The base API location on Discord's servers.
     */
    final val APIBASE = BASE + "api"

    final val GATEWAY = APIBASE + "/gateway/bot"

    final val USERS = APIBASE + "/users/"

    /**
     * Used for logging in.
     */
    final val LOGIN = APIBASE + "/auth/login"
    /**
     * Used for logging out.
     */
    final val LOGOUT = APIBASE + "/auth/logout"

    /**
     * Guilds URL
     */
    final val GUILDS = APIBASE + "/guilds/"

    final val CHANNELS = APIBASE + "/channels/"

    /**
     * Used for accepting invites
     */
    final val INVITE = APIBASE + "/invite/"

    /**
     * Formatted string for getting avatar URLs.
     */
    final val AVATARS = "https://cdn.discordapp.com/avatars/%s/%s.jpg"

    /**
     * Formatted string for getting guild icon URLs.
     */
    final val ICONS = "https://cdn.discordapp.com/icons/%s/%s.jpg"

    /**
     * Formatted string for getting api metric information.
     */
    final val METRICS = "https://srhpyqt94yxb.statuspage.io/metrics-display/d5cggll8phl5/%s.json"

    /**
     * Formatted string for getting maintenance information.
     */
    final val STATUS = "https://status.discordapp.com/api/v2/scheduled-maintenances/%s.json"

    /**
     * Voice url.
     */
    final val VOICE = APIBASE + "/voice/"

    /**
     * The OAuth2 url.
     */
    final val OAUTH = APIBASE + "/oauth2/"

    /**
     * The applications url.
     */
    final val APPLICATIONS = OAUTH + "applications"

    /**
     * Application icon url.
     */
    final val APPLICATION_ICON = "https://cdn.discordapp.com/app-icons/%s/%s.jpg"

    /**
     * The OAuth2 authorization url.
     */
    final val AUTHORIZE = "https://discordapp.com/oauth2/authorize"

    /**
     * The emoji image URL.
     */
    final val EMOJI_IMAGE = "https://cdn.discordapp.com/emojis/%s.png"
  }

  sealed trait ReconnectReason
  case object HeartbeatMissed extends ReconnectReason
  case object DisconnectedByServer extends ReconnectReason
  case object RequestedByServer extends ReconnectReason

  trait DiscordListener {
    def onGatewayEvent(connection: DiscordClient#GatewayConnection): GatewayEvents.GatewayEvent => Any

    def onGatewayData(data: => DynJValueSelector): Unit = {}
    def onGatewayOp(connection: DiscordClient#GatewayConnection, op: GatewayOp, data: => DynJValueSelector): Unit = {}
    def onVoiceOp(connection: DiscordClient#VoiceConnection, op: VoiceOp, data: => DynJValueSelector): Unit = {}
    def onUnexpectedGatewayOp(connection: DiscordClient#GatewayConnection, op: Int, data: => DynJValueSelector): Unit = {}
    def onUnexpectedVoiceOp(connection: DiscordClient#VoiceConnection, op: Int, data: => DynJValueSelector): Unit = {}
    def onMessageBeingSent(connection: DiscordClient#Connection, msg: String): Unit = {}

    def onReconnecting(connection: DiscordClient#Connection, reason: ReconnectReason): Unit = {}
    def onConnectionOpened(connection: DiscordClient#Connection): Unit = {}
    def onConnectionClosed(connection: DiscordClient#Connection): Unit = {}
    def onDisconnected(connection: DiscordClient#Connection, code: Int, reason: String): Unit = {}
    def onConnectionError(connection: DiscordClient#Connection, error: Throwable): Unit = {}
  }

  object DiscordListenerStateMachine {
    sealed trait Event
  }
  trait DiscordListenerStateMachine[E >: DiscordListenerStateMachine.Event] extends DiscordListener with StateMachine[E] {
    import DiscordListenerStateMachine.Event
    case class GatewayEvent(connection: DiscordClient#GatewayConnection, event: GatewayEvents.GatewayEvent) extends Event
    case class GatewayData(data: DynJValueSelector) extends Event
    case class GatewayOp(connection: DiscordClient#GatewayConnection, op: headache.GatewayOp, data: () => DynJValueSelector) extends Event
    case class VoiceOp(connection: DiscordClient#VoiceConnection, op: headache.VoiceOp, data: () => DynJValueSelector) extends Event
    case class UnexpectedGatewayOp(connection: DiscordClient#GatewayConnection, op: Int, data: () => DynJValueSelector) extends Event
    case class UnexpectedVoiceOp(connection: DiscordClient#VoiceConnection, op: Int, data: () => DynJValueSelector) extends Event
    case class MessageBeingSent(connection: DiscordClient#Connection, msg: String) extends Event
    case class Reconnecting(connection: DiscordClient#Connection, reason: ReconnectReason) extends Event
    case class ConnectionOpened(connection: DiscordClient#Connection) extends Event
    case class ConnectionClosed(connection: DiscordClient#Connection) extends Event
    case class Disconnected(connection: DiscordClient#Connection, code: Int, reason: String) extends Event
    case class ConnectionError(connection: DiscordClient#Connection, error: Throwable) extends Event

    override def apply(a: E): Unit = synchronized { super.apply(a) }
    private def run(evt: Event): Unit = this.orElse[Event, Unit] {
      case evt => undefHandler(evt)
    }.apply(evt)
    def undefHandler(evt: Event): Unit = {}

    override def onGatewayEvent(connection: DiscordClient#GatewayConnection) = evt => run(GatewayEvent(connection, evt))
    override def onGatewayData(data: => DynJValueSelector): Unit = run(GatewayData(data))
    override def onGatewayOp(connection: DiscordClient#GatewayConnection, op: headache.GatewayOp, data: => DynJValueSelector): Unit = run(GatewayOp(connection, op, () => data))
    override def onVoiceOp(connection: DiscordClient#VoiceConnection, op: headache.VoiceOp, data: => DynJValueSelector): Unit = run(VoiceOp(connection, op, () => data))
    override def onUnexpectedGatewayOp(connection: DiscordClient#GatewayConnection, op: Int, data: => DynJValueSelector): Unit = run(UnexpectedGatewayOp(connection, op, () => data))
    override def onUnexpectedVoiceOp(connection: DiscordClient#VoiceConnection, op: Int, data: => DynJValueSelector): Unit = run(UnexpectedVoiceOp(connection, op, () => data))
    override def onMessageBeingSent(connection: DiscordClient#Connection, msg: String): Unit = run(MessageBeingSent(connection, msg))
    override def onReconnecting(connection: DiscordClient#Connection, reason: ReconnectReason): Unit = run(Reconnecting(connection, reason))
    override def onConnectionOpened(connection: DiscordClient#Connection): Unit = run(ConnectionOpened(connection))
    override def onConnectionClosed(connection: DiscordClient#Connection): Unit = run(ConnectionClosed(connection))
    override def onDisconnected(connection: DiscordClient#Connection, code: Int, reason: String): Unit = run(Disconnected(connection, code, reason))
    override def onConnectionError(connection: DiscordClient#Connection, error: Throwable): Unit = run(ConnectionError(connection, error))

  }
}
