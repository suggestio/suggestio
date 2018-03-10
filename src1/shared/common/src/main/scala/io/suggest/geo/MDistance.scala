package io.suggest.geo

import io.suggest.common.html.HtmlConstants
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.03.18 13:48
  * Description: На замену сложной и ES-зависимой Distance приехал код упрощённой модели, которая живёт внутри Double.
  * Это модель расстояния в метрах, совместимая с Elasticsearch.
  */
object MDistance {

  /** JSON-поддержка для поля расстояния в метрах.
    * На стороне JVM в ES есть поддержка единиц измерения, а на клиенте - нет, да и тащить их неохота.
   */
  val MDISTANCE_FORMAT: Format[Double] = {
    val metersSuffix = "m"

    val r = Reads[Double] {
      case jsNumber: JsNumber =>
        JsSuccess(jsNumber.value.toDouble)
      case jsString: JsString =>
        val s = jsString.value
        // строка обычно имеет вид "100m", но может быть и "100"
        try {
          val metersPos = s.indexOf(metersSuffix)
          val digits = if (metersPos > 0)
            s.substring(0, metersPos)
          else
            s
          JsSuccess( digits.toDouble )
        } catch { case ex: Throwable =>
          JsError( s + HtmlConstants.SPACE + ex.getMessage )
        }
      case other =>
        JsError( other.toString() + "?" )
    }

    val w = Writes[Double] { distanceM =>
      JsString( distanceM.toString + metersSuffix )
    }

    Format(r, w)
  }

}
