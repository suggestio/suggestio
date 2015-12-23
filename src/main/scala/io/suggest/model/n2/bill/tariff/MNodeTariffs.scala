package io.suggest.model.n2.bill.tariff

import io.suggest.common.EmptyProduct
import io.suggest.model.es.IGenEsMappingProps
import io.suggest.model.n2.bill.tariff.daily.MDailyTf
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.12.15 14:12
 * Description: Тарифы узла для каких-то платных действий.
 */
object MNodeTariffs extends IGenEsMappingProps {

  val DAILY_FN  = "day"
  val GEO_TAG_TF_FN = "gt"

  val empty: MNodeTariffs = {
    apply()
  }

  implicit val FORMAT: Format[MNodeTariffs] = {
    (__ \ DAILY_FN).formatNullable[MDailyTf]
      .inmap [MNodeTariffs] (apply, _.daily)
  }


  import io.suggest.util.SioEsUtil._

  private def _tfField(id: String, model: IGenEsMappingProps) = {
    FieldObject(id, enabled = true, properties = model.generateMappingProps)
  }

  override def generateMappingProps: List[DocField] = {
    List(
      _tfField(DAILY_FN,  MDailyTf)
    )
  }

}


case class MNodeTariffs(
  daily        : Option[MDailyTf]      = None
)
  extends EmptyProduct
