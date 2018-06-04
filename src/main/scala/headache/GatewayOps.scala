package headache

import enumeratum.values._

sealed abstract class GatewayOp(val value: Int) extends IntEnumEntry
object GatewayOp extends IntEnum[GatewayOp] {
  val values = findValues
  case object Dispatch extends GatewayOp(0)
  case object Heartbeat extends GatewayOp(1)
  case object Identify extends GatewayOp(2)
  case object StatusUpdate extends GatewayOp(3)
  case object VoiceStateUpdate extends GatewayOp(4)
  case object VoiceServerPing extends GatewayOp(5)
  case object Resume extends GatewayOp(6)
  case object Reconnect extends GatewayOp(7)
  case object RequestGuildMembers extends GatewayOp(8)
  case object InvalidSession extends GatewayOp(9)
  case object Hello extends GatewayOp(10)
  case object HeartbeatAck extends GatewayOp(11)
}
