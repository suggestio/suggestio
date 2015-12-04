package io.suggest.model.n2.bill.tariff

import io.suggest.common.EmptyProduct
import io.suggest.model.es.IGenEsMappingProps
import io.suggest.model.n2.bill.tariff.adv.get.tag.MGeoTagAdvTf
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.12.15 14:12
 * Description: Тарифы узла для каких-то платных действий.
 */
object MNodeTariffs extends IGenEsMappingProps {

  val GEO_TAG_TF_FN = "gt"

  val empty: MNodeTariffs = {
    apply()
  }

  implicit val FORMAT: Format[MNodeTariffs] = {
    (__ \ GEO_TAG_TF_FN).formatNullable[MGeoTagAdvTf]
      .inmap[MNodeTariffs](apply, _.geoTagTf)
  }


  import io.suggest.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    List(
      FieldObject(GEO_TAG_TF_FN, enabled = true, properties = MGeoTagAdvTf.generateMappingProps)
    )
  }

}

case class MNodeTariffs(
  geoTagTf    : Option[MGeoTagAdvTf]    = None
  // TODO Тариф посуточный, тариф геотегов, тариф покупки
)
  extends EmptyProduct
