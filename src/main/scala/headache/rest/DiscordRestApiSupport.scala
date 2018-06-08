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
  
  private def optSnowflake(s: Snowflake) = if (s == NoSnowflake) None else Some(s)
  private val unit: String => Unit = _ => ()
  object channels extends Endpoint {
    def baseRequest(channelId: String) = s"https://discordapp.com/api/channels/${channelId}"
    
    def get(channelId: Snowflake)(implicit s: BackPressureStrategy): Future[Channel] = request(channelId.snowflakeString)(Json.parse(_).dyn.extract[Channel])
    def modify(channel: Channel)(implicit s: BackPressureStrategy): Future[Unit] = request(channel.id.snowflakeString, method = "PATCH", body = toJson(channel), expectedStatus = 201)(unit)
    def delete(channelId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] = request(channelId.snowflakeString, method = "DELETE")(unit)
    
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
      request(channelId.snowflakeString, extraPath = "/messages", method = "DELETE", expectedStatus = 204)(unit)
    def bulkDeleteMessages(channelId: Snowflake, messageIds: Array[Snowflake])(implicit s: BackPressureStrategy): Future[Unit] =
      request(channelId.snowflakeString, extraPath = "/messages/bulk-delete", method = "POST", body = Json.toJson(messageIds), expectedStatus = 204)(unit)
    
    
    def createReaction(channelId: Snowflake, messageId: Snowflake, emoji: String)(implicit s: BackPressureStrategy): Future[Unit] = 
      request(channelId.snowflakeString, extraPath = s"/messages/${messageId.snowflakeString}/reactions/$emoji/@me", method = "PUT", expectedStatus = 204)(unit)
    def deleteOwnReaction(channelId: Snowflake, messageId: Snowflake, emoji: String)(implicit s: BackPressureStrategy): Future[Unit] = 
      request(channelId.snowflakeString, extraPath = s"/messages/${messageId.snowflakeString}/reactions/$emoji/@me", method = "DELETE", expectedStatus = 204)(unit)
    def deleteUserReaction(channelId: Snowflake, messageId: Snowflake, emoji: String, userId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] = 
      request(channelId.snowflakeString, extraPath = s"/messages/${messageId.snowflakeString}/reactions/$emoji/@{userId.snowflakeString}", method = "DELETE", expectedStatus = 204)(unit)
   
    def getReactions(channelId: Snowflake, messageId: Snowflake, emoji: String, before: Snowflake = NoSnowflake,
                     after: Snowflake = NoSnowflake, limit: Int = 100)(implicit s: BackPressureStrategy): Future[Seq[User]] = {
      val params = Seq("limit" -> limit.toString) ++ optSnowflake(before).map("before" -> _.snowflakeString) ++ optSnowflake(after).map("after" -> _.snowflakeString)
      request(channelId.snowflakeString, extraPath = s"/messages/${messageId.snowflakeString}/reactions/$emoji", queryParams = params)(Json.parse(_).dyn.extract[Seq[User]])
    }
    def deleteAllReactions(channelId: Snowflake, messageId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] = 
      request(channelId.snowflakeString, extraPath = s"/messages/${messageId.snowflakeString}/reactions", method = "DELETE", expectedStatus = 204)(unit)

    def editChannelPermissions(channelId: Snowflake, overwriteId: Snowflake, allow: Array[Permission],
                               deny: Array[Permission], tpe: String)(implicit s: BackPressureStrategy): Future[Unit] = {
      val body = Json.obj("allow" -> Permissions.compact(allow:_*), "deny" -> Permissions.compact(deny:_*), "type" -> tpe)
      request(channelId.snowflakeString, extraPath = s"/permissions/${overwriteId.snowflakeString}", method = "PUT", body = body, expectedStatus = 204)(unit)
    }
    
//    def getChannelInvites(channelId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] = 
//      request(channelId.snowflakeString, extraPath = "/invites")
    
    def deleteChannelPermission(channelId: Snowflake, overwriteId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] =
      request(channelId.snowflakeString, extraPath = s"/permissions/${overwriteId.snowflakeString}", method = "DELETE", expectedStatus = 204)(unit)
    
    def triggerTypingIndicator(channelId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] =
      request(channelId.snowflakeString, extraPath = "/typing", method = "POST", expectedStatus = 204)(unit)
    
    def getPinnedMessages(channelId: Snowflake)(implicit s: BackPressureStrategy): Future[Seq[Message]] =
      request(channelId.snowflakeString, extraPath = "/pins")(Json.parse(_).dyn.extract[Seq[Message]])
    def addPinnedChannelMessage(channelId: Snowflake, messageId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] =
      request(channelId.snowflakeString, extraPath = s"/pins/${messageId.snowflakeString}", method = "PUT", expectedStatus = 204)(unit)
    def deletePinnedChannelMessage(channelId: Snowflake, messageId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] =
      request(channelId.snowflakeString, extraPath = s"/pins/${messageId.snowflakeString}", method = "DELETE", expectedStatus = 204)(unit)
    
    def groupDmAddRecipient(channelId: Snowflake, userId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] =
      ??? //request(channelId.snowflakeString, extraPath = s"/recipients/${userId.snowflakeString}", method = "PUT", expectedStatus = 204)(unit)
    
    def groupDmRemoveRecipient(channelId: Snowflake, userId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] =
      request(channelId.snowflakeString, extraPath = s"/recipients/${userId.snowflakeString}", method = "DELETE", expectedStatus = 204)(unit)
  }
  
  object guilds extends Endpoint {
    def baseRequest(guildId: String) = s"https://discordapp.com/api/guilds/${guildId}"
    
    def get(guildId: Snowflake)(implicit s: BackPressureStrategy): Future[Guild] = request(guildId.snowflakeString)(Json.parse(_).dyn.extract[Guild])
    def modify(guildId: Snowflake, modification: GuildModification)(implicit s: BackPressureStrategy): Future[Guild] = 
      request(guildId.snowflakeString, method = "PATCH", body = Json.toJson(modification))(Json.parse(_).dyn.extract[Guild])
    def delete(guildId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] =
      request(guildId.snowflakeString, method = "DELETE", expectedStatus = 204)(unit)
    
    def getChannels(guildId: Snowflake)(implicit s: BackPressureStrategy): Future[Seq[Channel]] =
      request(guildId.snowflakeString, extraPath = "/channels")(Json.parse(_).dyn.extract[Seq[Channel]])
    def createChannel(guildId: Snowflake, channel: GuildChannelCreationParams)(implicit s: BackPressureStrategy): Future[Channel] =
      request(guildId.snowflakeString, extraPath = "/channels", method = "POST", body = Json.toJson(channel))(Json.parse(_).dyn.extract[Channel])
    def modifyChannelPositions(guildId: Snowflake, channelId: Snowflake, position: Int)(implicit s: BackPressureStrategy): Future[Unit] =
      request(guildId.snowflakeString, extraPath = "/channels", method = "PATCH",
              body = Json.obj("id" -> channelId.snowflakeString, "position" -> position), expectedStatus = 204)(unit)
    
    def addMemberRole(guildId: Snowflake, userId: Snowflake, roleId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] =
      request(guildId.snowflakeString, extraPath = s"/members/${userId.snowflakeString}/roles/${roleId.snowflakeString}",
              method = "PUT", expectedStatus = 204)(unit)
    def removeMemberRole(guildId: Snowflake, userId: Snowflake, roleId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] =
      request(guildId.snowflakeString, extraPath = s"/members/${userId.snowflakeString}/roles/${roleId.snowflakeString}",
              method = "DELETE", expectedStatus = 204)(unit)
    
    def removeMember(guildId: Snowflake, userId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] =
      request(guildId.snowflakeString, extraPath = s"/members/${userId.snowflakeString}", method = "DELETE", expectedStatus = 204)(unit)
    
    
    def getBans(guildId: Snowflake)(implicit s: BackPressureStrategy): Future[Seq[Ban]] =
      request(guildId.snowflakeString, extraPath = "/bans")(Json.parse(_).dyn.extract[Seq[Ban]])
    def getBan(guildId: Snowflake, userId: Snowflake)(implicit s: BackPressureStrategy): Future[Ban] =
      request(guildId.snowflakeString, extraPath = s"/bans/${userId.snowflakeString}")(Json.parse(_).dyn.extract[Ban])
    def createBan(guildId: Snowflake, userId: Snowflake, deleteMessageDays: Int, reason: String)(implicit s: BackPressureStrategy): Future[Ban] =
      request(guildId.snowflakeString, extraPath = s"/bans/${userId.snowflakeString}",
              queryParams = Seq("delete-message-days" -> deleteMessageDays.toString, "reason" -> reason))(Json.parse(_).dyn.extract[Ban])
    def removeBan(guildId: Snowflake, userId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] =
      request(guildId.snowflakeString, extraPath = s"/bans/${userId.snowflakeString}", method = "DELETE")(unit)
    
    def getRoles(guildId: Snowflake)(implicit s: BackPressureStrategy): Future[Seq[Role]] =
      request(guildId.snowflakeString, extraPath = "/roles")(Json.parse(_).dyn.extract[Seq[Role]])
    def createRole(guildId: Snowflake, params: GuildRoleCreationParams)(implicit s: BackPressureStrategy): Future[Role] =
      request(guildId.snowflakeString, extraPath = "/roles", method = "POST", body = Json.toJson(params))(Json.parse(_).dyn.extract[Role])
    def modifyRolePositions(guildId: Snowflake, roleId: Snowflake, position: Int)(implicit s: BackPressureStrategy): Future[Role] =
      request(guildId.snowflakeString, extraPath = "/roles", method = "PATCH",
              queryParams = Seq("id" -> roleId.snowflakeString, "position" -> position.toString))(Json.parse(_).dyn.extract[Role])
    def modifyRole(guildId: Snowflake, roleId: Snowflake, params: GuildRoleCreationParams)(implicit s: BackPressureStrategy): Future[Role] =
      request(guildId.snowflakeString, extraPath = s"/roles/${roleId.snowflakeString}", method = "PATCH",
              body = Json.toJson(params))(Json.parse(_).dyn.extract[Role])
    def deleteRole(guildId: Snowflake, roleId: Snowflake)(implicit s: BackPressureStrategy): Future[Unit] =
      request(guildId.snowflakeString, extraPath = s"/roles/${roleId.snowflakeString}", method = "DELETE", expectedStatus = 204)(unit)
  }
  
  private[this] val rateLimitRegistry = new RateLimitRegistry()
  protected[headache] val baseHeaders = Map[String, java.util.Collection[String]]("Authorization" -> Arrays.asList(token)).asJava
  abstract class Endpoint {
    private[this] val queuedRequests = new AtomicInteger()
    
    protected def baseRequest(token: String): String
    
    protected def request[T](token: String, extraPath: String = "", method: String = "GET", queryParams: Seq[(String, String)] = Seq.empty,
                             body: JsValue = null, expectedStatus: Int = 200)(parser: String => T)(implicit s: BackPressureStrategy): Future[T] = {
      
      val base = baseRequest(token)
      var reqBuilder = new RequestBuilder(method).setUrl(base + extraPath).
      setHeaders(baseHeaders).addHeader("Content-Type", "application/json").setCharset(Charset.forName("utf-8")).
      setQueryParams(queryParams.map(t => new Param(t._1, t._2)).asJava)
      if (body != null) reqBuilder = reqBuilder.setBody(renderJson(body))
      else reqBuilder.setHeader("Content-Length", "0")
      
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
            import scala.concurrent.ExecutionContext.Implicits.global //it's okay to use it for this here
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

