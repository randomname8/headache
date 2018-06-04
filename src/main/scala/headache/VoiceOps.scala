package headache

import enumeratum.values._

sealed abstract class VoiceOp(val value: Int) extends IntEnumEntry
object VoiceOp extends IntEnum[VoiceOp] {

  val values = findValues
  /**
   * Used to begin a voice websocket connection
   */
  object Identify extends VoiceOp(0)

  /**
   * Used to select the voice protocol
   */
  object SelectPayload extends VoiceOp(1)

  /**
   * Used to complete the websocket handshake
   */
  object Ready extends VoiceOp(2)

  /**
   * Used to keep the websocket connection alive
   */
  object Heartbeat extends VoiceOp(3)

  /**
   * Used to describe the session
   */
  object SessionDescription extends VoiceOp(4)

  /**
   * Used to indicate which users are speaking
   */
  object Speaking extends VoiceOp(5)
}
