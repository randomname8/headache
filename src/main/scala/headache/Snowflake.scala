package headache

object Snowflake {
  def apply(s: String) = java.lang.Long.parseUnsignedLong(s).asInstanceOf[Snowflake]
  def apply(s: Long) = s.asInstanceOf[Snowflake]
  final val NoSnowflake = Snowflake(0)
}
