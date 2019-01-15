package io.suggest.es.model

import io.suggest.util.JacksonParsing.FieldsJsonAcc
import play.api.libs.json.{JsObject, JsString, Json}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 17:39
 * Description: Трейты для реализации десериализации через низкоуровневую аккамуляцию play-json полей.
 */

trait EsModelPlayJsonStaticT extends EsModelCommonStaticT {

  def writeJsonFields(m: T, acc: FieldsJsonAcc): FieldsJsonAcc

  final def toPlayJson(m: T) = JsObject(toPlayJsonAcc(m))
  final def toPlayJsonAcc(m: T) = writeJsonFields(m, Nil)
  final def toPlayJsonWithId(m: T): JsObject = {
    var acc = toPlayJsonAcc(m)
    val _id = m.id
    if (_id.isDefined)
      acc ::= "id" -> JsString(_id.get)
    JsObject(acc)
  }

  override def toJson(m: T) = toPlayJson(m).toString()
  override def toJsonPretty(m: T): String = Json.prettyPrint(toPlayJson(m))

}
