package headache
package rest

import JsonCodecs._
import play.api.libs.json.{Format, Json}

case class GuildRoleCreationParams(
  name: Option[String] = None,
  permissions: Option[Long] = None,
  color: Option[Int] = None,
  hoist: Option[Boolean] = None,
  mentionable: Option[Boolean] = None
)
object GuildRoleCreationParams {
  implicit val format: Format[GuildRoleCreationParams] = Json.format
  class Builder(val result: GuildRoleCreationParams) {
    def name(name: String) = new Builder(result.copy(name = Option(name)))
    def permissions(permissions: Long) = new Builder(result.copy(permissions = Some(permissions)))
    def color(color: Int) = new Builder(result.copy(color = Some(color)))
    def hoist(hoist: Boolean) = new Builder(result.copy(hoist = Some(hoist)))
    def mentionable(mentionable: Boolean) = new Builder(result.copy(mentionable = Some(mentionable)))
  }
  def Builder() = new Builder(GuildRoleCreationParams())
}
