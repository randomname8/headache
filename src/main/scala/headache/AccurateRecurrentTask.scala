package headache

import java.util.concurrent.locks.LockSupport
import scala.concurrent.SyncVar

class AccurateRecurrentTask(task: SyncVar[Unit] => Unit, everyMillis: Int) extends Thread(null, null, "AccurateRecurrentTask", 200 * 1024) {
  private[this] final val sleepInterval = everyMillis * 1000000
  private[this] val cancelled = new SyncVar[Unit]()
  def cancel(): Unit = cancelled.put(())
  override def run(): Unit = {
    while (!cancelled.isSet) {
      val now = System.nanoTime()
      task(cancelled)
      val total = System.nanoTime() - now
      val nextTarget = now + sleepInterval - total
      sleepFor(sleepInterval - total - 100000) //sleep until 100 us before the target deadline
      while (nextTarget > System.nanoTime()) {} //consume cycles
    }
  }

  private def sleepFor(nanos: Long): Unit = {
    if (nanos > 0) {
      var elapsed = 0l
      while (elapsed < nanos) {
        val t0 = System.nanoTime()
        LockSupport.parkNanos(nanos - elapsed)
        elapsed += System.nanoTime() - t0
      }
    }
  }
}
