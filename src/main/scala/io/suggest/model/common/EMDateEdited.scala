package io.suggest.model.common

import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.model.{EsModelPlayJsonT, EsModelStaticMutAkvT}
import io.suggest.util.SioEsUtil._
import org.joda.time.DateTime
import io.suggest.model.EsModel.{date2JsStr, dateTimeParser}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.11.14 10:50
 * Description: Поле с датой последнего редактирования. Эта дата НЕ обязательно является датой последнего изменения
 * документа в модели.
 */
object EMDateEdited {
  val DATE_EDITED_ESFN = "daEd"

  def fieldDate = FieldDate(DATE_EDITED_ESFN, include_in_all = false, index = FieldIndexingVariants.no)
}


import EMDateEdited._


/** Аддон для статических частей моделей, содержащих поле dateEdited. */
trait EMDateEditedStatic extends EsModelStaticMutAkvT {
  override type T <: EMDateEditedMut

  abstract override def generateMappingProps: List[DocField] = {
    fieldDate :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (DATE_EDITED_ESFN, deRaw) =>
        acc.dateEdited = Option(deRaw).map(dateTimeParser)
    }
  }
}



/** Голый интерфейс для поля dateEdited. */
trait EMDateEditedI {
  def dateEdited: Option[DateTime]
}


/** Аддон для экземпляра класса, имеющего поле dateEdited. */
trait EMDateEdited extends EsModelPlayJsonT with EMDateEditedI {
  override type T <: EMDateEdited

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc0 = super.writeJsonFields(acc)
    if (dateEdited.isDefined) {
      (DATE_EDITED_ESFN, date2JsStr(dateEdited.get)) :: acc0
    } else {
      acc0
    }
  }

}


/** mutable-версия трейта [[EMDateEdited]]. */
trait EMDateEditedMut extends EMDateEdited {
  var dateEdited: Option[DateTime]
}
