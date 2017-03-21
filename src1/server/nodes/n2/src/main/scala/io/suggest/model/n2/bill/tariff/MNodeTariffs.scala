package io.suggest.model.n2.bill.tariff

import io.suggest.common.empty.{EmptyProduct, IEmpty}
import io.suggest.es.model.IGenEsMappingProps
import io.suggest.model.PrefixedFn
import io.suggest.model.n2.bill.tariff.daily.MTfDaily
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

  object Fields {

    val DAILY_FN = "day"

    object Daily extends PrefixedFn {
      override protected def _PARENT_FN = DAILY_FN
      def CLAUSES_CAL_ID_FN = _fullFn( MTfDaily.Fields.Clauses.CAL_ID_FN )
      def CURRENCY_FN       = _fullFn( MTfDaily.Fields.CLAUSES_FN )
    }

  }


  import Fields._

  override val empty: MNodeTariffs = {
    apply()
  }

  implicit val FORMAT: Format[MNodeTariffs] = {
    (__ \ DAILY_FN).formatNullable[MTfDaily]
      .inmap[MNodeTariffs](apply, _.daily)
  }


  import io.suggest.es.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    List(
      FieldObject(DAILY_FN, enabled = true, properties = MTfDaily.generateMappingProps)
    )
  }

}


case class MNodeTariffs(
  daily         : Option[MTfDaily]      = None
)
  extends EmptyProduct
