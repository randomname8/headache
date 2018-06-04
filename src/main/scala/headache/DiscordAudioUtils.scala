package headache

import java.nio.ByteBuffer
import java.util.Arrays
import org.peergos.crypto.TweetNaCl

object DiscordAudioUtils {

  /**
   * Encript an audio chunk.
   * @param seq Unsigned short sequence number of the chunk, should be incremented on each packet sent.
   * @param timestamp Timestamp to which this packet corresponds, used to sync with the communication of others. Should be increased by OpusFrameSize on each packet sent.
   * @param audio The audio being sent.
   * @param secret Secret key received on voice channel negotiation.
   */
  def encrypt(seq: Char, timestamp: Int, ssrc: Int, audio: Array[Byte], secret: Array[Byte]): Array[Byte] = {
    val nonce = ByteBuffer.allocate(12).put(0x80.toByte).put(0x78.toByte).putChar(seq).putInt(timestamp).putInt(ssrc).array
    val encrypted = TweetNaCl.secretbox(audio, Arrays.copyOf(nonce, 24), secret) //encryption nonce is 24 bytes long while discord's is 12 bytes long

    val res = new Array[Byte](nonce.length + encrypted.length)
    System.arraycopy(nonce, 0, res, 0, nonce.length)
    System.arraycopy(encrypted, 0, res, 12, encrypted.length)
    nonce.array ++ encrypted
  }

  def decrypt(audio: Array[Byte], secret: Array[Byte]): AudioRtpFrame = {
    val header = Arrays.copyOfRange(audio, 0, 12)
    val content = Arrays.copyOfRange(audio, 12, audio.length)
    val nonce = header ++ new Array[Byte](12)
    val decrAudio = TweetNaCl.secretbox_open(content, nonce, secret)
    val bb = ByteBuffer.wrap(header)
    AudioRtpFrame(bb.getChar(2), bb.getInt(4), bb.getInt(8), decrAudio)
  }
}
