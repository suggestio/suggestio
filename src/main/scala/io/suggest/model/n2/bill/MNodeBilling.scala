package io.suggest.model.n2.bill

import io.suggest.common.EmptyProduct
import io.suggest.model.es.IGenEsMappingProps
import io.suggest.model.n2.bill.tariff.MNodeTariffs
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.12.15 14:09
 * Description: Billing v2 подразумевает, что узлы хранят свои тарифы внутри себя,
 */
object MNodeBilling extends IGenEsMappingProps {

  val CONTRACT_ID_FN = "ct"
  val TARIFFS_FN     = "tfs"

  /** Поддержка двустороннего json-маппинга. */
  implicit val FORMAT: Format[MNodeBilling] = (
    (__ \ CONTRACT_ID_FN).formatNullable[Long] and
    (__ \ TARIFFS_FN).formatNullable[MNodeTariffs]
      .inmap [MNodeTariffs] (
        _.getOrElse(MNodeTariffs.empty),
        { tf => if (tf.nonEmpty) Some(tf) else None }
      )
  )(apply, unlift(unapply))


  /** Расшаренный между инстансами нод пустой экземпляр данных биллинга. */
  val empty: MNodeBilling = {
    new MNodeBilling() {
      override def nonEmpty = false
    }
  }


  import io.suggest.util.SioEsUtil._
  override def generateMappingProps: List[DocField] = {
    List(
      FieldNumber(CONTRACT_ID_FN, fieldType = DocFieldTypes.long, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldObject(TARIFFS_FN, enabled = true, properties = MNodeTariffs.generateMappingProps)
    )
  }

}


case class MNodeBilling(
  contractId    : Option[Long] = None,
  tariffs       : MNodeTariffs = MNodeTariffs.empty
)
  extends EmptyProduct
