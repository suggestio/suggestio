package models.adv.price

import io.suggest.adv.AdvConstants.PriceJson._
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.16 17:32
  * Description: Модель JSON-ответа на запрос обновления цены с формой размещения.
  */
object GetPriceResp {

  /** Конвертер из/в JSON. */
  implicit val FORMAT: OFormat[GetPriceResp] = (
    (__ \ PERIOD_REPORT_HTML_FN).format[String] and
    (__ \ PRICE_HTML_FN).format[String]
  )(apply, unlift(unapply))

}


/**
  * Класс модели ответа запроса цены размещения.
  *
  * @param periodReportHtml HTML инфы по периоду размещения.
  * @param priceHtml HTML инфы по стоимости.
  */
case class GetPriceResp(
  periodReportHtml  : String,
  priceHtml         : String
)
