package headache.rest

sealed trait BackPressureStrategy
object BackPressureStrategy {
  object Fail extends BackPressureStrategy
  case class Retry(maxAttempts: Int) extends BackPressureStrategy
}
