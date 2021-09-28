package headache
package rest

import scala.concurrent.duration._
import scala.concurrent.stm._

/**
 * Simple thread-safe store for ratelimits.
 * Outgoing calls should see if there's a ratelimit for a token (the URL they want to access that has an individual rate limit policy),
 * when there's one, they should wait the time specified by it.
 * Finally after their request completes, they should update the registry.
 */
class RateLimitRegistry {

  private[this] val globalRateLimit = Ref[Option[Deadline]](None)
  private[this] val rateLimits = TMap.empty[Any, Deadline]
  
  def rateLimitFor(token: Any): Option[Deadline] = atomic { implicit txn =>
    globalRateLimit() match {
      case None => tryRateLimit(token)
      case Some(rl) if rl.isOverdue() =>
        globalRateLimit() = None
        tryRateLimit(token)
      case other => other
    }
  }
  private[this] def tryRateLimit(token: Any)(implicit trn: InTxn) = rateLimits.get(token) match {
    case Some(rl) if rl.isOverdue() =>
      rateLimits.remove(token)
      None
    case other => other
  }
  def registerRateLimit(token: Any, until: Deadline): Unit = rateLimits.single.update(token, until)
  def registerGlobalRateLimit(until: Deadline): Unit = globalRateLimit.single() = Some(until)
}
