package headache

import java.util.concurrent.locks.LockSupport

object AccurateRecurrentTask {
  class Cancellable {
    @volatile var cancelled = false
  }
}
import AccurateRecurrentTask._
class AccurateRecurrentTask(task: Cancellable => Unit, everyMillis: Int) extends Thread(null, null, "AccurateRecurrentTask", 200 * 1024) {
  private[this] final val sleepInterval = everyMillis * 1000000
  private[this] val cancelled = new Cancellable()
  def cancel(): Unit = cancelled.cancelled = true
  override def run(): Unit = {
    while (!cancelled.cancelled) {
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
      var elapsed = 0L
      while (elapsed < nanos) {
        val t0 = System.nanoTime()
        LockSupport.parkNanos(nanos - elapsed)
        elapsed += System.nanoTime() - t0
      }
    }
  }
}
