package headache

import enumeratum.values.IntEnumEntry
import java.io.{IOException}
import java.net.{DatagramSocket, InetSocketAddress, DatagramPacket, SocketTimeoutException}
import org.asynchttpclient.{ws}
import play.api.libs.json.{Json, JsValue}
import scala.concurrent._, duration._, ExecutionContext.Implicits._
import scala.util.control.NoStackTrace
import scala.util.control.NonFatal
import Json4sUtils._

private[headache] object VoiceConnectionSupport {
  val UdpKeepAlive = Array[Byte](0xC9.toByte, 0, 0, 0, 0, 0, 0, 0, 0)
  val OpusFrameSize = 960
  val MaxOpusPacketSize = OpusFrameSize * 2 // two channels
}
private[headache] trait VoiceConnectionSupport { self: DiscordClient =>
  import VoiceConnectionSupport._

  protected final class VoiceConnectionImpl(
      voiceStateUpdate: GatewayEvents.VoiceStateUpdate,
      voiceServerUpdate: GatewayEvents.VoiceServerUpdate,
      voiceConsumer: AudioRtpFrame => Unit,
      voiceProducer: () => Array[Byte]
  ) extends VoiceConnection with ws.WebSocketTextListener with ws.WebSocketCloseCodeReasonListener {
    @volatile private[this] var active = true
    @volatile private[this] var websocket: ws.WebSocket = _

    override def guildId = voiceServerUpdate.guildId
    override def isActive = active
    override def close() = if (active && websocket != null) websocket.close()

    override def onOpen(w: ws.WebSocket): Unit = {
      websocket = w
      listener.onConnectionOpened(this)
      send(renderJson(gatewayMessage(VoiceOp.Identify, Json.obj("server_id" -> voiceServerUpdate.guildId,
        "user_id" -> voiceStateUpdate.voiceState.userId,
        "session_id" -> voiceStateUpdate.sessionId,
        "token" -> voiceServerUpdate.token))))
    }
    override def onClose(w: ws.WebSocket): Unit = if (isActive) {
      websocket = null
      active = false
      listener.onConnectionClosed(this)
    }
    override def onClose(w: ws.WebSocket, code: Int, reason: String): Unit = if (isActive) {
      websocket.close()
      websocket = null
      active = false
      listener.onDisconnected(this, code, reason)
    }
    override def toString = s"VoiceConnection(voiceStateUpdate=$voiceStateUpdate, voiceServerUpdate=$voiceServerUpdate)"

    val stateMachine = new StateMachine[(DynJValueSelector, VoiceOp)] {
      def state(f: DynJValueSelector => PartialFunction[VoiceOp, Transition]): Transition = transition {
        case (payload, op) if f(payload).isDefinedAt(op) => f(payload)(op)
      }
      def initState = handshake
      def handshake = state(payload => {
        case VoiceOp.Ready =>
          val ssrc = payload.ssrc.extract[Int]
          val port = payload.port.extract[Int]
          val serverIp = payload.ip.extract[String]
          //          val modes = payload.modes.extract[Seq[String]]
          val heartbeatInterval = payload.heartbeat_interval.extract[Int]
          val socket = new DatagramSocket()
          socket.connect(new InetSocketAddress(serverIp, port))
          val ourIp = discoverIp(ssrc, socket)
          socket.setSoTimeout(5)

            send(renderJson(gatewayMessage(
                  VoiceOp.SelectPayload,
                  Json.obj(
                    "protocol" -> "udp",
                    "data" -> Json.obj(
                      "address" -> ourIp.getHostString,
                      "port" -> ourIp.getPort,
                      "mode" -> "xsalsa20_poly1305"))
                )))
            
          nextHeartbeat(heartbeatInterval)
          voiceConnected(ssrc, socket)
      })
      def voiceConnected(ssrc: Int, socket: DatagramSocket) = state(payload => {
        case VoiceOp.SessionDescription =>
          val secret = payload.secret_key.extract[Seq[Byte]].to[Array]

          /**
           * **************************************
           * audio sending and related variables. *
           * **************************************
           */
          var sendingAudio = false
          var seq: Char = 0
          def sendAudio() = {
            val audio = voiceProducer()
            if (audio.length > 0) {
              if (!sendingAudio) send(renderJson(gatewayMessage(VoiceOp.Speaking, Json.obj("delay" -> 0, "speaking" -> true))))
              val data = DiscordAudioUtils.encrypt(seq, seq * 960 /*this is opus frame size*/ , ssrc, audio, secret)

              if (seq + 1 > Char.MaxValue) seq = 0
              else seq = (seq + 1).toChar

              try socket.send(new DatagramPacket(data, data.length))
              catch {
                case e: IOException =>
                  listener.onConnectionError(VoiceConnectionImpl.this, e)
                  close()
                  onClose(websocket)
              }
              sendingAudio = true
            } else if (sendingAudio) {
              send(renderJson(gatewayMessage(VoiceOp.Speaking, Json.obj("delay" -> 0, "speaking" -> false))))
              sendingAudio = false
            }
          }
          var keepAliveCall = 0l
          def keepAliveNatPort() = {
            if (keepAliveCall % (5000 / 20) == 0) { //every 5 seconds
              try socket.send(new DatagramPacket(UdpKeepAlive, UdpKeepAlive.length))
              catch { case e: IOException => listener.onConnectionError(VoiceConnectionImpl.this, e) }
            }
            keepAliveCall += 1
          }
          val receiveBuffer = new Array[Byte](MaxOpusPacketSize + 12) //header size
          def receiveAudio() = {
            //keep receiving audio for 10ms
            val start = System.nanoTime()
            while (System.nanoTime() - start < 10.millis.toNanos) {
              //the while logic is so that we can handle bursts satisfactorily
              try {
                val in = new DatagramPacket(receiveBuffer, receiveBuffer.length)
                socket.receive(in)
                voiceConsumer(DiscordAudioUtils.decrypt(receiveBuffer.take(in.getLength), secret))
                
                  //TODO maybe someday handle the new RTP format as done here https://github.com/austinv11/Discord4J/blob/430c826377ad0aeca563267ed429648b8d59c5de/src/main/java/sx/blah/discord/api/internal/OpusPacket.java
              } catch {
                case _: SocketTimeoutException | _: IllegalStateException =>
                case e: IOException => listener.onConnectionError(VoiceConnectionImpl.this, e)
              }
            }
          }

          val senderTask = new AccurateRecurrentTask({ cancelTask =>
            if (isActive) {
              sendAudio()
              keepAliveNatPort()
              receiveAudio()
            } else {
              cancelTask.put(())
              scala.util.Try(socket.close())
            }

          }, 20)
          senderTask.setName("DiscordAudio-" + voiceStateUpdate.voiceState.channelId)
          senderTask.start()

          done
      })
    }

    override def onError(ex: Throwable): Unit = listener.onConnectionError(this, ex)
    override def onMessage(msg: String): Unit = {
      val payload = Json.parse(msg).dyn

      try {
        VoiceOp.withValueOpt(payload.op.extract).fold {
          listener.onUnexpectedVoiceOp(this, payload.op.extract, payload)
        } { op =>
          listener.onVoiceOp(this, op, payload)
          stateMachine.applyIfDefined(payload.d -> op)
        }
      } catch { case NonFatal(e) => listener.onConnectionError(this, e) }
    }

    def send(msg: String) = {
      if (!active) throw new IllegalStateException(s"$this is closed!")
      listener.onMessageBeingSent(this, msg)
      websocket.sendMessage(msg)
    }
    def gatewayMessage(op: IntEnumEntry, data: JsValue, eventType: Option[String] = None): JsValue = Json.obj(
      "op" -> op.value,
      "d" -> data
    ) ++ eventType.fold(Json.obj())(t => Json.obj("t" -> t))

    def nextHeartbeat(interval: Int): Unit = {
      timer.newTimeout(timeout => if (isActive) {
        Future { // don't hog the timer thread
          send(renderJson(gatewayMessage(VoiceOp.Heartbeat, Json toJson System.currentTimeMillis)))
          //after sending the heartbeat, change the current behaviour to detect the answer
          //if no answer is received in 5 seconds, reconnect.
          val prevBehaviour = stateMachine.current

          val timeout = timer.newTimeout({ timeout =>
            if (!timeout.isCancelled && isActive) {
              listener.onConnectionError(this, new RuntimeException("Did not receive a HeartbeatAck in 5 seconds!") with NoStackTrace)
              close()
              onClose(websocket)
            }
          }, (interval * 0.9).toLong, MILLISECONDS)

          lazy val detectHeartbeatAck: stateMachine.Transition = stateMachine.transition {
            case (_, VoiceOp.Heartbeat) =>
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
    def discoverIp(ssrc: Int, socket: DatagramSocket): InetSocketAddress = {
      val data = java.nio.ByteBuffer.allocate(70).putInt(ssrc).array()
      socket.send(new DatagramPacket(data, data.length))
      val response = new DatagramPacket(new Array[Byte](70), 70)
      socket.receive(response)
      val receivedData = response.getData
      val ip = new String(receivedData.slice(4, 68)).trim()
      val port = (receivedData(69).toInt & 0xff) << 8 | (receivedData(68).toInt & 0xff)
      InetSocketAddress.createUnresolved(ip, port)
    }
  }
}
