package io.suggest.model.common

import io.suggest.model.{EsModelStaticT, EsModelT}
import org.joda.time.DateTime
import io.suggest.model.EsModel._
import io.suggest.util.SioEsUtil._
import io.suggest.model.EsModel.date2JsStr
import com.github.nscala_time.time.OrderingImplicits._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 16:42
 * Description: Поле dateCreated является часто-использьзуемым полем метаданных.
 */

object EMDateCreatedStatic {
  def fieldDate = FieldDate(DATE_CREATED_ESFN, include_in_all = false, index = FieldIndexingVariants.no)
}

import EMDateCreatedStatic._


trait EMDateCreatedStatic extends EsModelStaticT {
  override type T <: EMDateCreatedMut

  abstract override def generateMappingProps: List[DocField] = {
    fieldDate :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (DATE_CREATED_ESFN, value) =>
        acc.dateCreated = dateCreatedParser(value)
    }
  }


  /** Простой сортировщик списка по дате. */
  def sortByDateCreated(mads: List[T]) = mads.sortBy(_.dateCreated).reverse
}


trait EMDateCreatedI extends EsModelT {
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
