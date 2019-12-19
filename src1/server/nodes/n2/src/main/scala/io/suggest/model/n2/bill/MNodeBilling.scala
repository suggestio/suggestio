package io.suggest.model.n2.bill

import io.suggest.common.empty.{EmptyProduct, IEmpty}
import io.suggest.model.n2.bill.tariff.MNodeTariffs
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.common.empty.EmptyUtil._
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.es.model.IGenEsMappingProps
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.model.PrefixedFn
import monocle.macros.GenLens

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.12.15 14:09
 * Description: Billing v2 подразумевает, что узлы хранят свои тарифы внутри себя,
 */
object MNodeBilling
  extends IEsMappingProps
  with IGenEsMappingProps
  with IEmpty
{

  override type T = MNodeBilling

  object Fields {

    val CONTRACT_ID_FN = "ct"
    val TARIFFS_FN     = "tfs"

    object Tariffs extends PrefixedFn {
      override protected def _PARENT_FN = TARIFFS_FN
      def DAILY_CLAUSES_CAL_ID_FN = _fullFn( MNodeTariffs.Fields.Daily.CLAUSES_CAL_ID_FN )
      def DAILY_CURRENCY_FN       = _fullFn( MNodeTariffs.Fields.Daily.CURRENCY_FN )
    }

  }


  import Fields._

  /** Поддержка двустороннего json-маппинга. */
  implicit val FORMAT: Format[MNodeBilling] = (
    (__ \ CONTRACT_ID_FN).formatNullable[Gid_t] and
    (__ \ TARIFFS_FN).formatNullable[MNodeTariffs]
      .inmap [MNodeTariffs] (
        opt2ImplMEmptyF(MNodeTariffs),
        implEmpty2OptF
      )
  )(apply, unlift(unapply))


  /** Расшаренный между инстансами нод пустой экземпляр данных биллинга. */
  override val empty: MNodeBilling = {
    new MNodeBilling() {
      override def nonEmpty = false
    }
  }


  import io.suggest.es.util.SioEsUtil._
  override def generateMappingProps: List[DocField] = {
    List(
      FieldNumber(CONTRACT_ID_FN, fieldType = DocFieldTypes.long, index = true, include_in_all = false),
      FieldObject(TARIFFS_FN, enabled = true, properties = MNodeTariffs.generateMappingProps)
    )
  }

  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.CONTRACT_ID_FN -> FNumber(
        typ     = DocFieldTypes.Long,
        index   = someTrue,
      ),
      F.TARIFFS_FN -> FObject.plain(
        enabled     = someTrue,
        properties  = Some(MNodeTariffs.esMappingProps)
      ),
    )
  }

  val contractId = GenLens[MNodeBilling](_.contractId)
  val tariffs = GenLens[MNodeBilling](_.tariffs)

}


case class MNodeBilling(
                         contractId    : Option[Gid_t]     = None,
                         tariffs       : MNodeTariffs      = MNodeTariffs.empty
                       )
  extends EmptyProduct

