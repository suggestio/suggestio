package io.suggest.xplay.json

import play.api.libs.json.{JsArray, JsBoolean, JsNull, JsNumber, JsObject, JsString, JsValue}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.12.2019 17:09
  * Description: Доп.утиль для play-json (кросс-платформенная).
  */
object PlayJsonUtil {

  /** Рекурсивная конвертация из play-json в нативный JSON.
    *
    * @param from Исходные json-данные.
    * @return
    */
  def fromJsValue(from: JsValue): Any = {
    from match {
      case s: JsString    => fromJsString(s)
      case n: JsNumber    => fromJsNumber(n)
      case b: JsBoolean   => fromJsBoolean(b)
      case obj: JsObject  => fromJsObject( obj )
      case arr: JsArray   => fromJsArray( arr )
      case JsNull         => null
    }
  }

  /** Конвертация JsBoolean в нативный scala.js Boolean. */
  def fromJsBoolean(b: JsBoolean): Boolean = {
    b.value
  }

  /** Конвертация JsString в нативный scala.js String. */
  def fromJsString(s: JsString): String = {
    s.value
  }

  /** Конвертация JsNumber в нативный scala.js Double. */
  def fromJsNumber(n: JsNumber): Double = {
    // TODO Тут надо различать toInt, toDouble и т.д.
    n.value.toDouble
  }

  /** Рекурсивная конвертация JsObject в нативный js.Dictionary с нативным наполнением. */
  def fromJsObject(obj: JsObject): Map[String, Any] = {
    obj.value
      .view
      .mapValues( fromJsValue )
      .toMap
  }

  /** Рекурсивная конвертация JsArray в нативный js.Array с нативным наполнением. */
  def fromJsArray(arr: JsArray): Seq[Any] = {
    arr.value
      .iterator
      .map(fromJsValue)
      .toSeq
  }


}
