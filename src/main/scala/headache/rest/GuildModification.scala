package headache
package rest

import JsonCodecs._
import play.api.libs.json.{Format, Json}

case class GuildModification(
  name: Option[String] = None,
  region: Option[String] = None,
  verificationLevel: Option[Int] = None,
  defaultMessageNotification: Option[Int] = None,
  explicitContentFilter: Option[Int] = None,
  afkChannelId: Option[Snowflake] = None,
  icon: Option[String] = None,
  ownerId: Option[Snowflake] = None,
  splash: Option[String] = None,
  systemChannelId: Option[Snowflake] = None,
)
object GuildModification {
  implicit val format: Format[GuildModification] = Json.format
  class Builder(val result: GuildModification) {
    def name(name: String) = new Builder(result.copy(name = Option(name)))
    def region(region: String) = new Builder(result.copy(region = Option(region)))
    def verificationLevel(verificationLevel: Int) = new Builder(result.copy(verificationLevel = Option(verificationLevel)))
    def defaultMessageNotification(defaultMessageNotification: Int) = new Builder(result.copy(defaultMessageNotification = Some(defaultMessageNotification)))
    def explicitContentFilter(explicitContentFilter: Int) = new Builder(result.copy(explicitContentFilter = Some(explicitContentFilter)))
    def afkChannelId(afkChannelId: Snowflake) = new Builder(result.copy(afkChannelId = Option(afkChannelId)))
    def icon(icon: String) = new Builder(result.copy(icon = Option(icon)))
    def ownerId(ownerId: Snowflake) = new Builder(result.copy(ownerId = Option(ownerId)))
    def splash(splash: String) = new Builder(result.copy(splash = Option(splash)))
    def systemChannelId(systemChannelId: Snowflake) = new Builder(result.copy(systemChannelId = Option(systemChannelId)))
  }
  def Builder() = new Builder(GuildModification())
}
