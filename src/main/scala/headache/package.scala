package object headache {
  trait Tag[T]
  type Snowflake = Long with Tag[Snowflake.type]
  implicit class SnowflakeOps(private val v: Snowflake) extends AnyVal {
    def snowflakeString = java.lang.Long.toUnsignedString(v)
  }
  final val NoSnowflake = Snowflake.NoSnowflake


  type PermissionBits = Long with Tag[PermissionBits.type]
  
  implicit class PermissionBitsOps(private val v: PermissionBits) extends AnyVal {
    def asPermissionsString: String = java.lang.Long.toUnsignedString(v)
  }
}
