package io.suggest.model.n2.bill.tariff

import io.suggest.common.empty.{IEmpty, EmptyProduct}
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
object MNodeTariffs extends IGenEsMappingProps with IEmpty {

  override type T = MNodeTariffs

  val DAILY_FN  = "day"

  override val empty: MNodeTariffs = {
    apply()
  }

  implicit val FORMAT: Format[MNodeTariffs] = {
    (__ \ DAILY_FN).formatNullable[MDailyTf]
      .inmap[MNodeTariffs](apply, _.daily)
  }


  import io.suggest.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    List(
      FieldObject(DAILY_FN, enabled = true, properties = MDailyTf.generateMappingProps)
    )
  }

}


case class MNodeTariffs(
  daily         : Option[MDailyTf]      = None
)
  extends EmptyProduct
