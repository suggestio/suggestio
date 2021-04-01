package io.suggest.xplay.json

import play.api.libs.json._

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.07.17 18:16
  * Description: Scala.js-only утиль для play-json.
  * Утиль возникла из-за того, что play-json совсем не совместим с js.Any/js.Dynamic.
  * Т.е. очень хотелось сконвертить play-json-структуру в нативный браузерный JSON,
  * а это оказалось не предусмотрено конструкцией.
  */
object PlayJsonSjsUtil {

  // Из-за обилия методов, тут недоступна @tailrec-оптимизация.
  // Но это плевать в условиях очень ограниченных объёмов конвертации play -> native.

  /** Рекурсивная конвертация из play-json в нативный JSON.
    *
    * @param from Исходные json-данные.
    * @return
    */
  def toNativeJson(from: JsValue): js.Any = {
    from match {
      case s: JsString    => toNativeJsonStr(s)
      case n: JsNumber    => toNativeJsonNum(n)
      case b: JsBoolean   => toNativeJsonBool(b)
      case obj: JsObject  => toNativeJsonObj( obj )
      case arr: JsArray   => toNativeJsonArr( arr )
      case JsNull         => null
    }
  }

  /** Конвертация JsBoolean в нативный scala.js Boolean. */
  def toNativeJsonBool(b: JsBoolean): Boolean = {
    b.value
  }

  /** Конвертация JsString в нативный scala.js String. */
  def toNativeJsonStr(s: JsString): String = {
    s.value
  }

  /** Конвертация JsNumber в нативный scala.js Double. */
  def toNativeJsonNum(n: JsNumber): Double = {
    n.value.toDouble
  }

  /** Рекурсивная конвертация JsObject в нативный js.Dictionary с нативным наполнением. */
  def toNativeJsonObj(obj: JsObject): js.Dictionary[js.Any] = {
    val d = js.Dictionary.empty[js.Any]
    for ( (k,v) <- obj.fields ) {
      d(k) = toNativeJson(v)
    }
    d
  }

  /** Рекурсивная конвертация JsArray в нативный js.Array с нативным наполнением. */
  def toNativeJsonArr(arr: JsArray): js.Array[js.Any] = {
    arr.value
      .iterator
      .map(toNativeJson)
      .toJSArray
  }


  def init(): Unit = {
    // Выставить ошибку, при необходимости.
    PlayJsonUtil.THROW_ERRORS = scalajs.LinkingInfo.developmentMode
  }

}
