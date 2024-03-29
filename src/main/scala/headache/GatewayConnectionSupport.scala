package headache

import enumeratum.values.IntEnumEntry
import java.io.{ByteArrayInputStream, BufferedReader, InputStreamReader, ByteArrayOutputStream}
import java.time.Instant
import java.util.Arrays
import java.util.zip.{Inflater, InflaterOutputStream}
import org.asynchttpclient.ws
import org.json4s.native.JsonParser
import play.api.libs.json.{Json, JsValue, JsNull}
import scala.annotation.tailrec
import scala.concurrent._, duration._, ExecutionContext.Implicits._
import scala.util.control.NoStackTrace
import scala.util.control.NonFatal
import JsonUtils._

private[headache] trait GatewayConnectionSupport { self: DiscordClient =>
  import DiscordClient._

  protected def startShard(gw: String, shard: Int, totalShards: Int, desiredEvents: Set[GatewayEvents.Intent], lastSession: Option[LastSessionState] = None): Future[GatewayConnection] = try {
    val res = new GatewayConnectionImpl(gw, shard, totalShards, desiredEvents, lastSession)
    val websocketFuture = ahc.prepareGet(gw).
      addHeader("Origin", null: String).
      execute(new ws.WebSocketUpgradeHandler(Arrays.asList(res)))

    val ready = Promise[GatewayConnection]()
    websocketFuture.toCompletableFuture.handle[Unit] {
      case (_, null) =>
        ready.success(res); ()
      case (_, ex) => ready.failure(ex); ()
    }
    ready.future
  } catch {
    case NonFatal(ex) => Future.failed(ex)
  }

  protected case class SessionData(id: String, gatewayProtocol: Int)
  protected case class LastSessionState(data: SessionData, seq: Long, connectionAttempt: Int)
  protected final class GatewayConnectionImpl(val gateway: String, val shardNumber: Int, val totalShards: Int,
      desiredEvents: Set[GatewayEvents.Intent], lastSession: Option[LastSessionState]) extends GatewayConnection with ws.WebSocketListener {
    @volatile private[this] var active = true
    @volatile private[this] var websocket: ws.WebSocket = _
    @volatile private[this] var seq = 0L
    @volatile private[this] var session: Option[SessionData] = None
    private[this] var missedHeartbeats = 0

    private[this] def listenerReporting(f: => Unit) = 
      try f
      catch { 
        case NonFatal(e) => reportError(e)
      }
    private[this] def reportError(e: Throwable) = try listener.onConnectionError(this, e) catch { case NonFatal(e2) => 
      val baos = new java.io.ByteArrayOutputStream(1024*5)
      val ps = new java.io.PrintStream(baos, true, "utf-8")
      ps.println("Error reporting to listener\n")
      e.addSuppressed(e2)
      e.printStackTrace(ps)
      ps.flush()
      System.out.write(baos.toByteArray)
    }
    
    override def isActive = active
    override def close() = if (active && websocket != null) {
      active = false
      websocket.sendCloseFrame()
    }

    override def onOpen(w: ws.WebSocket): Unit = {
      websocket = w
      listenerReporting(listener.onConnectionOpened(this))
    }
//    override def onClose(w: ws.WebSocket): Unit = if (isActive) {
//      websocket = null
//      active = false
//      listener.onConnectionClosed(this)
//    }
    override def onClose(w: ws.WebSocket, code: Int, reason: String): Unit = if (isActive) { //after a break down, netty will eventually realize that the socket broke, and even though we already called websocket.close(), it will eventually invoke this method.
      websocket = null
      if (active) { //if we are no longer active, it means the close() method was called, and so we must not reconnect
        listenerReporting(listener.onDisconnected(this, code, reason))
        reconnect(DisconnectedByServer)
      }
      active = false //we are not active anymore after this step
    }
    override def toString = s"GatewayConnection(gw=$gateway, shardNumber=$shardNumber, totalShards=$totalShards, seq=$seq, session=${session.map(_.id)})"

    /**
     * **************************************************
     * Definition of possible states for the gateway    *
     * **************************************************
     */

    case class GatewayMessage(op: GatewayOp, tpe: Option[String], payload: () => DynJValueSelector)
    val stateMachine = new StateMachine[GatewayMessage] {
      private[this] val identityMsg = renderJson {
        gatewayMessage(
          GatewayOp.Identify,
          Json.obj(
            "token" -> token,
            "properties" -> Json.obj(
              "$os" -> clientIdentity.os,
              "$browser" -> clientIdentity.browser,
              "$device" -> clientIdentity.device
            ),
//                                        ("$referring_domain" -> "") ~
//                                        ("$referrer" -> "")*/
            "compress" -> true,
            "large_threshold" -> 50,
            "intents" -> desiredEvents.map(e => 1 << GatewayEvents.Intent.indexOf(e)).reduce(_|_)
          ) ++ ( if (totalShards > 1) Json.obj("shard" -> Seq(shardNumber, totalShards)) else Json.obj())
        )
      }

      def initState = hello
      def hello = transition {
        case GatewayMessage(GatewayOp.Hello, _, payload) =>
          nextHeartbeat(payload().d.heartbeat_interval.extract)
          lastSession match {
            case None =>
              send(identityMsg)
              handshake
            case Some(lastSession) =>
              send(renderJson(gatewayMessage(GatewayOp.Resume, Json.obj("token" -> token,
                "session_id" -> lastSession.data.id,
                "seq" -> lastSession.seq))))
              resume
          }
      }

      def handshake = transition {
        case evt @ GatewayMessage(GatewayOp.Dispatch, Some("READY"), payload) =>
          session = Some(SessionData(payload().d.session_id.extract, payload().d.v.extract))
          dispatcher(evt)
          dispatcher

        case GatewayMessage(GatewayOp.InvalidSession, _, _) =>
          reportError(new IllegalStateException("Received an invalid session after sending identification.") with NoStackTrace)
          close()
          done
      }

      def resume: Transition = transition {
        case GatewayMessage(GatewayOp.InvalidSession, _, _) =>
          send(identityMsg)
          handshake

        case evt @ GatewayMessage(GatewayOp.Dispatch, Some("RESUMED"), _) =>
          session = lastSession.map(_.data) //only session needs to be assgined, seq is obtained from the resumed message
          dispatcher(evt)
          dispatcher

        case evt @ GatewayMessage(GatewayOp.Dispatch, _, _) => //replayed messages
          dispatcher(evt)
          resume //continue resuming
      }

      def dispatcher: Transition = transition {
        case GatewayMessage(GatewayOp.Dispatch, Some(tpe), payload) =>
          listenerReporting {
            listener.onGatewayEvent(GatewayConnectionImpl.this)(GatewayEvents.GatewayEvent(
              GatewayEvents.EventType.withValueOpt(tpe).getOrElse(GatewayEvents.EventType.Unknown), payload
            ))
          }
          dispatcher
        case GatewayMessage(GatewayOp.Reconnect, _, _) =>
          reconnect(RequestedByServer)
          done
      }
    }

    override def onError(ex: Throwable): Unit = reportError(Option(ex) getOrElse new RuntimeException("Something went wrong but we got null throwable?"))
    override def onTextFrame(msg: String, finalFragment: Boolean, rsv: Int): Unit = {
      //do basic stream parsing to obtain general headers, the idea is to avoid computation as much as possible here.

      val parser = (p: JsonParser.Parser) => {
        import JsonParser._
        @tailrec def parse(seq: Option[Long] = null, op: Int = -1, tpe: Option[String] = null): (Option[Long], Int, Option[String]) = {
          if (seq != null && op != -1 && tpe != null) (seq, op, tpe)
          else p.nextToken match {
            case End => (if (seq eq null) None else seq, op, if (tpe eq null) None else tpe)
            case FieldStart("t") => p.nextToken match {
              case StringVal(tpe) => parse(seq, op, Some(tpe))
              case NullVal => parse(seq, op, None)
              case _ => p.fail("event type not a string")
            }
            case FieldStart("s") => p.nextToken match {
              case LongVal(seq) => parse(Some(seq), op, tpe)
              case IntVal(seq) => parse(Some(seq.longValue), op, tpe)
              case NullVal => parse(None, op, tpe)
              case _ => p.fail("event type not a long")
            }
            case FieldStart("op") => p.nextToken match {
              case LongVal(op) => parse(seq, op.toInt, tpe)
              case IntVal(op) => parse(seq, op.intValue, tpe)
              case _ => p.fail("op type not a long")
            }
            case _ => parse(seq, op, tpe)
          }
        }
        parse()
      }

      try {
        val (s, op, tpe) = JsonParser.parse(msg, parser)
        if (op == -1) throw new IllegalStateException("no option found in discord message?\n" + msg)
        lazy val payload = Json.parse(msg).dyn

        s foreach (seq = _)
        GatewayOp.withValueOpt(op).fold {
          listenerReporting(listener.onUnexpectedGatewayOp(this, op, payload))
        } { op =>
          listenerReporting(listener.onGatewayOp(this, op, payload))
          stateMachine.orElse[GatewayMessage, Unit] {
            case GatewayMessage(GatewayOp.Heartbeat, _, _) => send(renderJson(Json.obj("op" -> GatewayOp.Heartbeat.value, "d" -> seq)))
            case GatewayMessage(GatewayOp.Dispatch, _, _) =>
            case _ =>
          }.apply(GatewayMessage(op, tpe, () => payload))
        }

      } catch { case NonFatal(e) => reportError(e) }
    }
    
    private[this] val accumulatedCompressedChunks = new collection.mutable.ArrayBuffer[Array[Byte]](10)
    private[this] val compressedMessage = new ByteArrayOutputStream()
    private[this] val uncompressedMessage = new ByteArrayOutputStream()
    private[this] val inflater = new Inflater
    override def onBinaryFrame(bytes: Array[Byte], finalFragment: Boolean, rsv: Int): Unit = {
      accumulatedCompressedChunks += bytes
      val l = bytes.length
      if (l >= 4 && bytes(l - 4) == 0 && bytes(l - 3) == 0 && bytes(l - 2) == -1 && bytes(l - 1) == -1) { //ZLIB suffix == 0x0000ffff
        compressedMessage.reset()
        uncompressedMessage.reset()
        accumulatedCompressedChunks foreach compressedMessage.write
        accumulatedCompressedChunks.clear()
        compressedMessage.writeTo(new InflaterOutputStream(uncompressedMessage, inflater))
        val reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(uncompressedMessage.toByteArray)))
        val msg = reader.lines.collect(java.util.stream.Collectors.joining())
        onTextFrame(msg, finalFragment, rsv)
      }
    }

    def send(msg: String) = {
      if (!active) throw new IllegalStateException(s"Shard $shardNumber is closed!")
      listenerReporting(listener.onMessageBeingSent(this, msg))
      websocket.sendTextFrame(msg)
    }

    def gatewayMessage(op: IntEnumEntry, data: JsValue): JsValue = Json.obj(
      "op" -> op.value,
      "d" -> data
    )

    override def sendStatusUpdate(idleSince: Option[Instant], status: PresenceState, game: Activity, afk: Boolean): Unit = {
      import JsonCodecs.activityFormat
      send(renderJson(
          gatewayMessage(
            GatewayOp.StatusUpdate,
            Json.obj(
              "since" -> idleSince.map(e => Json toJsFieldJsValueWrapper e.toEpochMilli).getOrElse(JsNull),
              "game" -> game,
              "status" -> status.value,
              "afk" -> afk
            )
          )
        ))
    }
    override def sendRequestGuildMembers(guildId: Snowflake, query: String = "", limit: Int = 0): Unit = {
      send(renderJson(
        gatewayMessage(GatewayOp.RequestGuildMembers, Json.obj("guild_id" -> guildId.snowflakeString, "query" -> query, "limit" -> limit))
      ))
    }
    override def sendVoiceStateUpdate(guildId: Snowflake, channelId: Option[Snowflake], selfMute: Boolean, selfDeaf: Boolean): Unit = {
      send(renderJson(
          gatewayMessage(GatewayOp.VoiceStateUpdate, Json.obj("guild_id" -> guildId.snowflakeString, "self_mute" -> selfMute, "self_deaf" -> selfDeaf,
                                                              "channel_id" -> channelId.map(_.snowflakeString)))
      ))
    }

    def nextHeartbeat(interval: Int): Unit = {
      timer.newTimeout(timeout => if (isActive) { // don't hog the timer thread
        Future {
          send(renderJson(Json.obj("op" -> GatewayOp.Heartbeat.value, "d" -> seq)))
          //after sending the heartbeat, change the current behaviour to detect the answer
          //if no answer is received, reconnect.
          val prevBehaviour = stateMachine.current

          val heartbeatTimeout = (interval * 0.8).toInt.millis.toSeconds
          val timeout = timer.newTimeout({ timeout =>
            if (!timeout.isCancelled && isActive) {
              missedHeartbeats += 1
              listenerReporting(listener.onHeartbeatMissed(this, missedHeartbeats))
              reconnect(HeartbeatMissed)
            }
          }, heartbeatTimeout, SECONDS)

          lazy val detectHeartbeatAck: stateMachine.Transition = stateMachine.transition {
            case GatewayMessage(GatewayOp.HeartbeatAck, _, _) =>
              timeout.cancel()
              prevBehaviour
            case other if prevBehaviour.isDefinedAt(other) =>
              prevBehaviour(other)
              detectHeartbeatAck
          }

          stateMachine.switchTo(detectHeartbeatAck)
          nextHeartbeat(interval)
        }
      }, interval.toLong, MILLISECONDS)
    }

    def reconnect(reason: ReconnectReason): Unit = {
      if (websocket != null && active) websocket.sendCloseFrame()
      active = false //this gatewayconnection is no longer valid
      listenerReporting(listener.onReconnecting(this, reason))
      val reconnectInstance = if (session.isDefined) 0 else lastSession.map(_.connectionAttempt + 1).getOrElse(0)
      val newLastSession = session.map(s => LastSessionState(s, seq, reconnectInstance))
      def reconnectAttempt(duration: FiniteDuration): Unit = {
        timer.newTimeout(_ =>
          startShard(gateway, shardNumber, totalShards, desiredEvents, newLastSession).failed.foreach(_ => reconnectAttempt(5.seconds)),
          duration.length, duration.unit
        )
      }
      if (reconnectInstance > 0) reconnectAttempt(5.seconds)
      else reconnectAttempt(0.seconds)
    }
  }
}
