package io.suggest.ym.model.common

import io.suggest.model.{EsModelT, EsModelStaticT}
import io.suggest.util.SioEsUtil._
import io.suggest.model.EsModel.{FieldsJsonAcc, stringParser}
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.14 18:05
 * Description:
 */

object EMDisableReason {
  val DISABLE_REASON_ESFN = "disableReason"
}

import EMDisableReason._

trait EMDisableReasonStatic extends EsModelStaticT {
  override type T <: EMDisableReasonMut

  abstract override def generateMappingProps: List[DocField] = {
    val drField = FieldString(DISABLE_REASON_ESFN, index = FieldIndexingVariants.no, include_in_all = false)
    drField :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (DISABLE_REASON_ESFN, value) =>
        acc.disableReason = Option(stringParser(value))
    }
  }
}


trait EMDisableReasonI extends EsModelT {
  override type T <: EMDisableReasonI

  /** Кто является изготовителем этой рекламной карточки? */
  def disableReason: Option[String]
}


trait EMDisableReason extends EMDisableReasonI {
  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc0 = super.writeJsonFields(acc)
    if (disableReason.isDefined)
      DISABLE_REASON_ESFN -> JsString(disableReason.get) :: acc0
    else
      acc0
  }

}

trait EMDisableReasonMut extends EMProducerId {
  override type T <: EMProducerIdMut
  var disableReason: Option[String]
}