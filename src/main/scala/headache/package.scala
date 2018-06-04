package object headache {
  trait Tag[T]
  object Snowflake {
    def apply(s: String) = java.lang.Long.parseUnsignedLong(s).asInstanceOf[Snowflake]
    def apply(s: Long) = s.asInstanceOf[Snowflake]
  }
  type Snowflake = Long with Tag[Snowflake.type]
  implicit class SnowflakeOps(private val v: Snowflake) extends AnyVal {
    def snowflakeString = java.lang.Long.toUnsignedString(v)
  }
  val NoSnowflake = Snowflake(0)
}
