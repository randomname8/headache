package headache

import scala.collection.immutable.TreeSet

/**
 * Helper buffer that solves jitter in audio stream.
 * Notice that this class is not threadsafe
 */
class JitterBuffer(capacity: Int, cycleThresholdTolerance: Int) {

  private[this] implicit val audioFrameOrdering: Ordering[AudioRtpFrame] = (a, b) => a.rtpSequence - b.rtpSequence
  import audioFrameOrdering.mkOrderingOps
  private[this] var nextBuffer = new TreeSet[AudioRtpFrame]()
  private[this] var currentBuffer = new TreeSet[AudioRtpFrame]()
  private[this] var _dropped = 0
  def dropped = _dropped

  def size: Int = currentBuffer.size + nextBuffer.size

  def push(frame: AudioRtpFrame): Boolean = {
    val oldest = if (currentBuffer.isEmpty) frame else currentBuffer.firstKey
    if (frame < oldest) {
      if (currentBuffer.lastKey.rtpSequence > (0xffff - cycleThresholdTolerance) && frame.rtpSequence < cycleThresholdTolerance) {
        nextBuffer += frame
        if (nextBuffer.size + currentBuffer.size > capacity) pop() //remove oldest element and swap buffers if needed
        true
      } else {
        _dropped += 1
        false
      }
    } else {
      currentBuffer += frame
      if (currentBuffer.size > capacity) currentBuffer -= oldest
      true
    }
  }

  def pop(): Option[AudioRtpFrame] = {
    if (currentBuffer.isEmpty && nextBuffer.isEmpty) None
    else {
      if (currentBuffer.isEmpty) {
        currentBuffer = nextBuffer
        nextBuffer = new TreeSet
      }
      val res = currentBuffer.firstKey
      currentBuffer -= res
      Some(res)
    }
  }

  override def toString = "JitterBuffer(" + (currentBuffer.toSeq ++ nextBuffer.toSeq).map(_.rtpSequence).mkString("seq=(", ",", ")") + s", dropped=$dropped)"
}
