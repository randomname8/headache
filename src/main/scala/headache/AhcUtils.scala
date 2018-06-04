package headache

import org.asynchttpclient._
import scala.concurrent._
import play.api.libs.json.Json
import Json4sUtils._

object AhcUtils {
  /**
   * general purpose AsyncCompletionHandler implementation with integration with futures and its parts being definable thru functions.
   */
  def request[T](reqBuilder: BoundRequestBuilder)(
    onCompletedF: Response => T,
    onBodyPartReceivedF: HttpResponseBodyPart => AsyncHandler.State = null,
    onStatusReceivedF: HttpResponseStatus => AsyncHandler.State = null,
    onHeadersReceivedF: HttpResponseHeaders => AsyncHandler.State = null,
    onHeadersWrittenF: () => AsyncHandler.State = null,
    onContentWrittenF: () => AsyncHandler.State = null,
    onContentWriteProgressF: (Long, Long, Long) => AsyncHandler.State = null
  ): Future[T] = {
    val promise = Promise[T]()
    reqBuilder execute new AsyncCompletionHandler[T] {
      override def onCompleted(r: Response) = {
        val res = onCompletedF(r)
        promise.success(res)
        res
      }
      override def onThrowable(t: Throwable) = promise.failure(t)
      override def onBodyPartReceived(header: HttpResponseBodyPart) = {
        val r = super.onBodyPartReceived(header)
        if (onBodyPartReceivedF != null) onBodyPartReceivedF(header) else r
      }
      override def onStatusReceived(header: HttpResponseStatus) = {
        val r = super.onStatusReceived(header)
        if (onStatusReceivedF != null) onStatusReceivedF(header) else r
      }
      override def onHeadersReceived(header: HttpResponseHeaders) = {
        val r = super.onHeadersReceived(header)
        if (onHeadersReceivedF != null) onHeadersReceivedF(header) else r
      }
      override def onHeadersWritten() = {
        val r = super.onHeadersWritten()
        if (onHeadersWrittenF != null) onHeadersWrittenF() else r
      }
      override def onContentWritten() = {
        val r = super.onContentWritten()
        if (onContentWrittenF != null) onContentWrittenF() else r
      }
      override def onContentWriteProgress(amount: Long, current: Long, total: Long) = {
        val r = super.onContentWriteProgress(amount, current, total)
        if (onContentWriteProgressF != null) onContentWriteProgressF(amount, current, total) else r
      }
    }
    promise.future
  }

  val asDynJson: Response => DynJValueSelector = r => {
    if ((200 until 299).contains(r.getStatusCode)) Json.parse(r.getResponseBody()).dyn
    else throw new RuntimeException(s"Unexpected status ${r.getStatusCode}${if (r.getStatusText != null) " - " + r.getStatusText else ""}.\n" + r.getResponseBody())
  }
}
