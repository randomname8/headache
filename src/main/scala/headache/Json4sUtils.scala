package headache

import language.dynamics

//import org.json4s._
//import org.json4s.native.JsonMethods
//import prickle._
import scala.annotation.unchecked.uncheckedVariance
import play.api.libs.json._

object Json4sUtils {

  type Reads[+T] = play.api.libs.json.Reads[T @uncheckedVariance]
  type Writes[+T] = play.api.libs.json.Writes[T @uncheckedVariance]

  implicit class jValue2Dyn(val jv: JsValue) extends AnyVal {
    def dyn = new DynJValueSelector(JsDefined(jv))
  }
  class DynJValueSelector(val jv: JsLookupResult) extends AnyVal with Dynamic {
    def selectDynamic(field: String) = new DynJValueSelector(jv \ field)
    def extract[T](implicit read: Reads[T]): T = jv.validate[T].asEither.fold(err => throw new NoSuchElementException(err.toString), identity)
    override def toString = jv.toString
  }

  def renderJson(jv: JsValue, pretty: Boolean = false): String =
    if (pretty) Json.prettyPrint(jv)
  else Json.stringify(jv)
  def toJson[T](t: T)(implicit p: Writes[T]): JsValue = p.writes(t)
}