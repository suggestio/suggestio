package io.suggest.model.es

import io.suggest.model.es.EsModelUtil.FieldsJsonAcc
import play.api.libs.json.{Json, JsString, JsObject}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 17:39
 * Description: Трейты для реализации десериализации через низкоуровневую аккамуляцию play-json полей.
 */

trait EsModelPlayJsonStaticT extends EsModelCommonStaticT {

  def writeJsonFields(m: T, acc: FieldsJsonAcc): FieldsJsonAcc

  def toPlayJson(m: T) = JsObject(toPlayJsonAcc(m))
  def toPlayJsonAcc(m: T) = writeJsonFields(m, Nil)
  def toPlayJsonWithId(m: T): JsObject = {
    var acc = toPlayJsonAcc(m)
    val _id = m.id
    if (_id.isDefined)
      acc ::= "id" -> JsString(_id.get)
    JsObject(acc)
  }

  override def toJson(m: T) = toPlayJson(m).toString()
  override def toJsonPretty(m: T): String = Json.prettyPrint(toPlayJson(m))

}


/** Шаблон для динамических частей ES-моделей, которые очень хорошо реализуют toJson() через play.json. */
@deprecated("", "")
trait EsModelPlayJsonT extends EsModelCommonT {
}
