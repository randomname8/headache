package headache
package rest

import JsonCodecs._
import play.api.libs.json.{Format, Json}

case class GuildChannelCreationParams(
  name: String,
  tpe: Channel.Type,
  topic: Option[String] = None,
  bitrate: Option[Int] = None,
  userLimit: Option[Int] = None,
  permissionOverwrites: Option[Array[PermissionOverwrite]] = None,
  parentId: Option[Snowflake] = None,
  nsfw: Option[Boolean] = None)
object GuildChannelCreationParams {
  implicit val format: Format[GuildChannelCreationParams] = Json.format
  class Builder(val result: GuildChannelCreationParams) {
    def topic(topic: String) = new Builder(result.copy(topic = Option(topic)))
    def bitrate(bitrate: Int) = new Builder(result.copy(bitrate = Some(bitrate)))
    def userLimit(userLimit: Int) = new Builder(result.copy(userLimit = Some(userLimit)))
    def permissionOverwrites(permissionOverwrites: Array[PermissionOverwrite]) = new Builder(result.copy(permissionOverwrites = Option(permissionOverwrites)))
    def parentId(parentId: Snowflake) = new Builder(result.copy(parentId = Some(parentId)))
    def nsfw(nsfw: Boolean) = new Builder(result.copy(nsfw = Some(nsfw)))
  }
  def Builder(name: String, tpe: Channel.Type) = new Builder(GuildChannelCreationParams(name, tpe))
}
