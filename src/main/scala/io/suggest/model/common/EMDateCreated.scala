package io.suggest.model.common

import io.suggest.model.es.{EsModelPlayJsonT, EsModelStaticMutAkvT, EsModelUtil}
import org.joda.time.DateTime
import io.suggest.util.SioEsUtil._
import EsModelUtil.{FieldsJsonAcc, date2JsStr, dateTimeParser}
import com.github.nscala_time.time.OrderingImplicits._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 16:42
 * Description: Поле dateCreated является часто-использьзуемым полем метаданных.
 */

object EMDateCreatedStatic {
  val DATE_CREATED_ESFN = "dateCreated"

  def fieldDate = FieldDate(DATE_CREATED_ESFN, include_in_all = false, index = FieldIndexingVariants.no)
}

import EMDateCreatedStatic._


trait EMDateCreatedStatic extends EsModelStaticMutAkvT {
  override type T <: EMDateCreatedMut

  abstract override def generateMappingProps: List[DocField] = {
    fieldDate :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (DATE_CREATED_ESFN, value) =>
        acc.dateCreated = dateTimeParser(value)
    }
  }


  /** Простой сортировщик списка по дате. */
  def sortByDateCreated(mads: List[T]) = mads.sortBy(_.dateCreated).reverse
}


trait EMDateCreatedI extends EsModelPlayJsonT {
  override type T <: EMDateCreatedI
  def dateCreated: DateTime
}


trait EMDateCreated extends EMDateCreatedI {
  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    (DATE_CREATED_ESFN, date2JsStr(dateCreated)) :: super.writeJsonFields(acc)
  }
}


trait EMDateCreatedMut extends EMDateCreated {
  override type T <: EMDateCreatedMut
  var dateCreated: DateTime

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    if (dateCreated == null)
      dateCreated = DateTime.now()
    super.writeJsonFields(acc)
  }
}
