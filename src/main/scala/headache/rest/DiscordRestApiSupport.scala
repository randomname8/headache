package headache
package rest

import io.netty.util.HashedWheelTimer
import java.nio.charset.Charset
import java.util.Arrays
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.Nullable
import org.asynchttpclient.{AsyncHttpClient, Param, RequestBuilder}
import play.api.libs.json.{Json, JsValue}
import scala.collection.JavaConverters._
import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._
import scala.util.control.{ControlThrowable, NoStackTrace}
import JsonUtils._, JsonCodecs._

private[headache] trait DiscordRestApiSupport {

  protected def ahc: AsyncHttpClient
  protected def timer:HashedWheelTimer
  protected def token: String
  
  @volatile var maxQueuedRequests: Int = 100
  
  def optSnowflake(s: Snowflake) = if (s == NoSnowflake) None else Some(s)
  object channels extends Endpoint {
    def baseRequest(channelId: String) = s"https://discordapp.com/api/channels/${channelId}"
    
    def get(channelId: Snowflake)(implicit s: BackPressureStrategy): Future[Channel] = request(channelId.snowflakeString)(Json.parse(_).dyn.extract[Channel])
    def modify(channel: Channel)(implicit s: BackPressureStrategy): Future[Unit] = request(channel.id.snowflakeString, method = "PATCH", body = toJson(channel), expectedStatus = 201)(_ => ())
    def delete(channelId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] = request(channelId.snowflakeString, method = "DELETE")(_ => ())
    
    def getMessage(channelId: Snowflake, messageId: Snowflake)(implicit s: BackPressureStrategy): Future[Message] = 
      request(channelId.snowflakeString, extraPath = s"/messages/${messageId.snowflakeString}")(Json.parse(_).dyn.extract[Message])
    
    def getMessages(channelId: Snowflake, around: Snowflake = NoSnowflake, before: Snowflake = NoSnowflake, after: Snowflake = NoSnowflake,
                    limit: Int = 100)(implicit s: BackPressureStrategy): Future[Seq[Message]] = {
      require(around != NoSnowflake || before != NoSnowflake || after != NoSnowflake, "one of 'around', 'before' or 'after' must be specified (i.e different from NoSnowflake)")
      val params = Seq("limit" -> limit.toString) ++ optSnowflake(around).map("around" -> _.snowflakeString) ++ 
      optSnowflake(before).map("before" -> _.snowflakeString) ++ optSnowflake(after).map("after" -> _.snowflakeString)
      request(channelId.snowflakeString, extraPath = "/messages", queryParams = params.toSeq)(Json.parse(_).dyn.extract[Seq[Message]])
    }
    
    def createMessage(channelId: Snowflake, message: String, @Nullable embed: Embed = null, tts: Boolean = false)(implicit s: BackPressureStrategy): Future[Message] = {
      val body = Json.obj("content" -> message, "nonce" -> (null: String), "tts" -> tts, "embed" -> Option(embed))
      request(channelId.snowflakeString, extraPath = "/messages", method = "POST", body = body)(Json.parse(_).dyn.extract[Message])
    }
    def editMessage(channelId: Snowflake, messageId: Snowflake, @Nullable content: String = null,
                    @Nullable embed: Embed = null)(implicit s: BackPressureStrategy): Future[Message] = {
      require(content != null || embed != null, "content and embed cant both be null")
      val body = Json.obj("content" -> Option(content), "embed" -> Option(embed))
      request(channelId.snowflakeString, extraPath = "/messages", method = "PATCH", body = body)(Json.parse(_).dyn.extract[Message])
    }
    def deleteMessage(channelId: Snowflake, messageId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] =
      request(channelId.snowflakeString, extraPath = "/messages", method = "DELETE", expectedStatus = 204)(_ => ())
    def bulkDeleteMessages(channelId: Snowflake, messageIds: Array[Snowflake])(implicit s: BackPressureStrategy): Future[Unit] =
      request(channelId.snowflakeString, extraPath = "/messages/bulk-delete", method = "POST", body = Json.toJson(messageIds), expectedStatus = 204)(_ => ())
    
    
    def createReaction(channelId: Snowflake, messageId: Snowflake, emoji: String)(implicit s: BackPressureStrategy): Future[Unit] = 
      request(channelId.snowflakeString, extraPath = s"/messages/${messageId.snowflakeString}/reactions/$emoji/@me", method = "PUT", expectedStatus = 204)(_ => ())
    def deleteOwnReaction(channelId: Snowflake, messageId: Snowflake, emoji: String)(implicit s: BackPressureStrategy): Future[Unit] = 
      request(channelId.snowflakeString, extraPath = s"/messages/${messageId.snowflakeString}/reactions/$emoji/@me", method = "DELETE", expectedStatus = 204)(_ => ())
    def deleteUserReaction(channelId: Snowflake, messageId: Snowflake, emoji: String, userId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] = 
      request(channelId.snowflakeString, extraPath = s"/messages/${messageId.snowflakeString}/reactions/$emoji/@{userId.snowflakeString}", method = "DELETE", expectedStatus = 204)(_ => ())
   
    def getReactions(channelId: Snowflake, messageId: Snowflake, emoji: String, before: Snowflake = NoSnowflake,
                     after: Snowflake = NoSnowflake, limit: Int = 100)(implicit s: BackPressureStrategy): Future[Seq[User]] = {
      val params = Seq("limit" -> limit.toString) ++ optSnowflake(before).map("before" -> _.snowflakeString) ++ optSnowflake(after).map("after" -> _.snowflakeString)
      request(channelId.snowflakeString, extraPath = s"/messages/${messageId.snowflakeString}/reactions/$emoji", queryParams = params)(Json.parse(_).dyn.extract[Seq[User]])
    }
    def deleteAllReactions(channelId: Snowflake, messageId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] = 
      request(channelId.snowflakeString, extraPath = s"/messages/${messageId.snowflakeString}/reactions", method = "DELETE", expectedStatus = 204)(_ => ())

    def editChannelPermissions(channelId: Snowflake, overwriteId: Snowflake, allow: Array[Permission],
                               deny: Array[Permission], tpe: String)(implicit s: BackPressureStrategy): Future[Unit] = {
      val body = Json.obj("allow" -> Permissions.compact(allow:_*), "deny" -> Permissions.compact(deny:_*), "type" -> tpe)
      request(channelId.snowflakeString, extraPath = s"/permissions/${overwriteId.snowflakeString}", method = "PUT", body = body, expectedStatus = 204)(_ => ())
    }
    
//    def getChannelInvites(channelId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] = 
//      request(channelId.snowflakeString, extraPath = "/invites")
    
    def deleteChannelPermission(channelId: Snowflake, overwriteId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] =
      request(channelId.snowflakeString, extraPath = s"/permissions/${overwriteId.snowflakeString}", method = "DELETE", expectedStatus = 204)(_ => ())
    
    def triggerTypingIndicator(channelId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] =
      request(channelId.snowflakeString, extraPath = "/typing", method = "POST", expectedStatus = 204)(_ => ())
    
    def getPinnedMessages(channelId: Snowflake)(implicit s: BackPressureStrategy): Future[Seq[Message]] =
      request(channelId.snowflakeString, extraPath = "/pins")(Json.parse(_).dyn.extract[Seq[Message]])
    def addPinnedChannelMessage(channelId: Snowflake, messageId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] =
      request(channelId.snowflakeString, extraPath = s"/pins/${messageId.snowflakeString}", method = "PUT", expectedStatus = 204)(_ => ())
    def deletePinnedChannelMessage(channelId: Snowflake, messageId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] =
      request(channelId.snowflakeString, extraPath = s"/pins/${messageId.snowflakeString}", method = "DELETE", expectedStatus = 204)(_ => ())
    
    def groupDmAddRecipient(channelId: Snowflake, userId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] =
      ??? //request(channelId.snowflakeString, extraPath = s"/recipients/${userId.snowflakeString}", method = "PUT", expectedStatus = 204)(_ => ())
    
    def groupDmRemoveRecipient(channelId: Snowflake, userId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] =
      request(channelId.snowflakeString, extraPath = s"/recipients/${userId.snowflakeString}", method = "DELETE", expectedStatus = 204)(_ => ())
  }
  
  private[this] val rateLimitRegistry = new RateLimitRegistry()
  protected[headache] val baseHeaders = Map[String, java.util.Collection[String]]("Authorization" -> Arrays.asList(token)).asJava
  private[DiscordRestApiSupport] trait Endpoint {
    private[this] val queuedRequests = new AtomicInteger()
    
    protected def baseRequest(token: String): String
    
    protected def request[T](token: String, extraPath: String = "", method: String = "GET", queryParams: Seq[(String, String)] = Seq.empty,
                             body: JsValue = null, expectedStatus: Int = 200)(parser: String => T)(implicit s: BackPressureStrategy): Future[T] = {
      
      val base = baseRequest(token)
      var reqBuilder = new RequestBuilder(method).setUrl(base + extraPath).
      setHeaders(baseHeaders).addHeader("Content-Type", "application/json").setCharset(Charset.forName("utf-8")).
      setQueryParams(queryParams.map(t => new Param(t._1, t._2)).asJava)
      if (body != null) reqBuilder = reqBuilder.setBody(renderJson(body))
      
      val req = reqBuilder.build()
      
      def request(s: BackPressureStrategy): Future[T] = {
        val res = rateLimitRegistry.rateLimitFor(base) match {
          case Some(deadline) => Future.failed(RateLimitException(deadline.timeLeft))
          case _ => 
            AhcUtils.request(ahc.prepareRequest(req)){ resp =>
              lazy val body = resp.getResponseBody(Charset.forName("utf-8"))
              resp.getStatusCode match {
                case `expectedStatus` => parser(body)
                case 429 =>
                  val rl = Json.parse(body).dyn
                  val deadline = rl.retry_after.extract[Int].millis
                  if (rl.global.extract) rateLimitRegistry.registerGlobalRateLimit(deadline.fromNow)
                  else rateLimitRegistry.registerGlobalRateLimit(deadline.fromNow)
                  throw RateLimitException(rl.retry_after.extract[Int].millis)

                case other => throw new RuntimeException(s"Unexpected status code $other:\n$body")
              }
            }
        }
        
        s match {
          case BackPressureStrategy.Retry(attempts) if attempts > 0 =>
            import scala.concurrent.ExecutionContext.Implicits.global //it's okay to use it for tihs here
            res.recoverWith {
              case RateLimitException(reset) => 
                if (queuedRequests.get >= maxQueuedRequests) throw TooManyQueuedRequests
                val promise = Promise[T]()
                timer.newTimeout({ timeout => 
                    promise completeWith request(BackPressureStrategy.Retry(attempts - 1))
                    queuedRequests.decrementAndGet
                  }, reset.length, reset.unit)
                promise.future
            }
            
          case _ => res
        }
      }
      request(s)
    }
  }
  
  case class RateLimitException private(retryAfter: FiniteDuration) extends ControlThrowable
  object TooManyQueuedRequests extends Exception with NoStackTrace 
}

