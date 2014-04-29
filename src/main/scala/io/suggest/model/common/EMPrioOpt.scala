package io.suggest.model.common

import io.suggest.model.{EsModel, EsModelStaticT, EsModelT}
import io.suggest.util.SioEsUtil._
import io.suggest.model.EsModel.FieldsJsonAcc
import play.api.libs.json.JsNumber

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 18:11
 * Description: Необязательное поле prio в EsModel'ях.
 */
object EMPrioOpt {
  val PRIO_ESFN = "prio"
}

import EMPrioOpt._

trait EMPrioOptStatic extends EsModelStaticT {
  override type T <: EMPrioOptMut

  abstract override def generateMappingProps: List[DocField] = {
    FieldNumber(PRIO_ESFN,  fieldType = DocFieldTypes.integer,  index = FieldIndexingVariants.not_analyzed,  include_in_all = false) ::
    super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (PRIO_ESFN, value) =>
        acc.prio = Option(EsModel.intParser(value))
    }
  }
}

trait EMPrioOpt extends EsModelT {
  override type T <: EMPrioOpt

  def prio: Option[Int]

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc0 = super.writeJsonFields(acc)
    if (prio.isDefined)
      PRIO_ESFN -> JsNumber(prio.get) :: acc0
    else
      acc0
  }
}

trait EMPrioOptMut extends EMPrioOpt {
  override type T <: EMPrioOptMut
  var prio: Option[Int]
}
