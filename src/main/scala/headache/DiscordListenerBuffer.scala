package headache

import java.util.concurrent.ArrayBlockingQueue
import scala.annotation.tailrec
import scala.collection.JavaConverters._

class DiscordListenerBuffer(queueSize: Int = 1000/*, var logOutgoingGatewayMessage: Boolean = false, var logIncomingGatewayMessage: Boolean = false*/) extends DiscordClient.DiscordListener {
  private val queue = new ArrayBlockingQueue[GatewayEvents.GatewayEvent](queueSize)

  override def onGatewayEvent(connection: DiscordClient#GatewayConnection) = {
    case evt =>
      if (!queue.offer(evt)) {
        queue.synchronized {
          if (queue.remainingCapacity == 0) {
            queue.poll()
          }
          queue.put(evt)
        }
      }
  }
//  override def onGatewayData(data: => DynJValueSelector): Unit = if (logIncomingGatewayMessage) println("received: " + pretty(render(data.jv)))
//  override def onMessageBeingSent(connection: DiscordClient#Connection, msg: String): Unit = if (logOutgoingGatewayMessage) println("sending: " + msg)
//  override def onUnexpectedGatewayOp(connection: DiscordClient#GatewayConnection, op: Int, data: => DynJValueSelector): Unit = println(s"unexepected GW op $op: " + pretty(render(data.jv)))
//  override def onReconnecting(connection: DiscordClient#Connection, reason: DiscordClient.ReconnectReason): Unit = println(s"Reconnecting $connection due to $reason")
//  override def onConnectionOpened(connection: DiscordClient#Connection): Unit = println(s"$connection stablished")
//  override def onConnectionClosed(connection: DiscordClient#Connection): Unit = println(s"$connection closed")
//  override def onDisconnected(connection: DiscordClient#Connection, code: Int, reason: String): Unit = println(s"$connection lost, code: $code, reason: $reason")
//  override def onConnectionError(connection: DiscordClient#Connection, error: Throwable): Unit = println(s"$connection error: $error")

  def peek = queue.peek()
  def pop() = Option(queue.poll())
  def take() = queue.take()
  @tailrec final def takeUntil(f: GatewayEvents.GatewayEvent => Boolean): GatewayEvents.GatewayEvent = {
    val res = take()
    if (f(res)) res
    else takeUntil(f)
  }

  def remaining = queue.size()
  def iterator = queue.iterator.asScala
  def consumingIterator = Iterator.continually(pop).takeWhile(_ != null)
}
