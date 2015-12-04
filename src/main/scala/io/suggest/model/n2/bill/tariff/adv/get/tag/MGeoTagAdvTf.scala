package io.suggest.model.n2.bill.tariff.adv.get.tag

import io.suggest.model.es.IGenEsMappingProps
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.12.15 14:36
 * Description: Тариф на размещение в данном теге.
 */

object MGeoTagAdvTf extends IGenEsMappingProps {

  val CURRENCY_CODE_FN = "cc"
  val DAILY_AMOUNT_FN  = "da"

  implicit val FORMAT: Format[MGeoTagAdvTf] = (
    (__ \ CURRENCY_CODE_FN).format[String] and
    (__ \ DAILY_AMOUNT_FN).format[Double]
  )(apply, unlift(unapply))


  import io.suggest.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    List(
      // Данные тарифа индексируются, чтобы можно было строить статистику.
      FieldString(CURRENCY_CODE_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldNumber(DAILY_AMOUNT_FN, fieldType = DocFieldTypes.double, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
    )
  }

}


case class MGeoTagAdvTf(
  currencyCode  : String,
  dailyAmount   : Double
)
