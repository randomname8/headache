package headache

import better.files._
import scala.io.AnsiColor
import scala.concurrent.ExecutionContext.Implicits._
import JsonUtils._
import GatewayEvents._

object TestBot {

  def main(args: Array[String]): Unit = {
    val token = file"test-token".contentAsString()
    
    val client = new DiscordClient(token, new DiscordClient.DiscordListener {
        def prettyPrint(js: DynJValueSelector) = JsonUtils.renderJson(js.jv.result.get, true)
        override def onGatewayOp(connection: DiscordClient#GatewayConnection, op: GatewayOp, data: => DynJValueSelector): Unit = 
          println(AnsiColor.BLUE + s"op: $op data: ${prettyPrint(data)}" + AnsiColor.RESET)
        override def onVoiceOp(connection: DiscordClient#VoiceConnection, op: VoiceOp, data: => DynJValueSelector): Unit =
          println(AnsiColor.MAGENTA + s"voice op: $op data: ${prettyPrint(data)}" + AnsiColor.RESET)
        override def onUnexpectedGatewayOp(connection: DiscordClient#GatewayConnection, op: Int, data: => DynJValueSelector): Unit =
          println(AnsiColor.RED + s"unexpected op: $op data: ${prettyPrint(data)}" + AnsiColor.RESET)
        override def onUnexpectedVoiceOp(connection: DiscordClient#VoiceConnection, op: Int, data: => DynJValueSelector): Unit =
          println(AnsiColor.RED + s"unexpected voice op: $op data: ${prettyPrint(data)}" + AnsiColor.RESET)
        override def onMessageBeingSent(connection: DiscordClient#Connection, msg: String): Unit =
          println(AnsiColor.CYAN + s"sending: $msg" + AnsiColor.RESET)

        override def onReconnecting(connection: DiscordClient#Connection, reason: DiscordClient.ReconnectReason): Unit =
          println(AnsiColor.RED + s"reconnecting $connection due to $reason" + AnsiColor.RESET)
        override def onConnectionOpened(connection: DiscordClient#Connection): Unit =
          println(s"connected $connection")
        override def onConnectionClosed(connection: DiscordClient#Connection): Unit =
          println(s"connected closed $connection")
        override def onDisconnected(connection: DiscordClient#Connection, code: Int, reason: String): Unit =
          println(AnsiColor.RED + s"disconnected $connection code: $code reason: $reason" + AnsiColor.RESET)
        override def onConnectionError(connection: DiscordClient#Connection, error: Throwable): Unit = {
          val stacktrace = Iterator.iterate(error)(_.getCause).takeWhile(_ != null).foldLeft(new StringBuilder){ (sb, ex) =>
            sb.append("caused by: ").append(ex)
            ex.getStackTrace foreach (t => sb.append("\n  ").append(t))
            sb
          }.result
          println(AnsiColor.RED_B + AnsiColor.WHITE + s"connection error $connection $stacktrace" + AnsiColor.RESET)
        }
      
        override def onGatewayEvent(connection: DiscordClient#GatewayConnection) = {
          case ReadyEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case Resumed(()) => println(AnsiColor.YELLOW + "resumed" + AnsiColor.RESET)
          case ChannelCreateEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case ChannelUpdateEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case ChannelDeleteEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case GuildCreateEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case GuildUpdateEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case GuildDeleteEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case GuildBanAddEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case GuildBanRemoveEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case GuildEmojisUpdateEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case GuildIntegrationUpdateEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case GuildMemberRemoveEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case GuildMemberUpdateEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case GuildMemberChunkEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case GuildRoleCreateEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case GuildRoleUpdateEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case GuildRoleDeleteEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case MessageCreateEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case MessageUpdateEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case MessageDeleteEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case MessageDeleteBulkEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case MessageReactionAddEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case MessageReactionRemoveEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case MessageReactionRemoveAllEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case PresenceUpdateEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case TypingStartEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case UserUpdateEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case VoiceStateUpdateEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
          case VoiceServerUpdateEvent(evt) => println(AnsiColor.YELLOW + evt + AnsiColor.RESET)
        }
      })
    client.login().onComplete(println)
  }
}
