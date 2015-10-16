package io.suggest.model.es

import io.suggest.model.es.EsModelUtil.FieldsJsonAcc
import play.api.libs.json.{Json, JsString, JsObject}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 17:39
 * Description: Трейты для реализации десериализации через низкоуровневую аккамуляцию play-json полей.
 */

/** Интерфейс с методом сериализации в play.Json экземпляра модели данных. */
trait ToPlayJsonObj {
  def toPlayJsonAcc: FieldsJsonAcc
  /** Сериализовать экземпляр модели данных в промежуточное представление play.Json. */
  def toPlayJson = JsObject(toPlayJsonAcc)
  def toPlayJsonWithId: JsObject
}


/** Шаблон для динамических частей ES-моделей, которые очень хорошо реализуют toJson() через play.json. */
trait EsModelPlayJsonT extends EsModelCommonT with ToPlayJsonObj {
  override type T <: EsModelPlayJsonT

  override def toPlayJsonAcc = writeJsonFields(Nil)
  override def toPlayJsonWithId: JsObject = {
    var acc = toPlayJsonAcc
    val _id = id
    if (_id.isDefined)
      acc ::= "id" -> JsString(_id.get)
    JsObject(acc)
  }

  override def toJson = toPlayJson.toString()
  override def toJsonPretty: String = Json.prettyPrint(toPlayJson)

  def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc
}


/** Трейт базовой реализации экземпляра модели. Вынесен из неё из-за особенностей stackable trait pattern.
  * Он содержит stackable-методы, реализованные пустышками. */
trait EsModelPlayJsonEmpty extends EsModelPlayJsonT {
  def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    acc
  }
}

