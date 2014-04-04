package io.suggest.model.common

import io.suggest.model.{EsModelStaticT, EsModelT}
import org.joda.time.DateTime
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.model.EsModel._
import io.suggest.util.SioEsUtil.DocField
import io.suggest.util.SioEsUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 16:42
 * Description:
 */

object EMDateCreatedStatic {
  def fieldDate = FieldDate(DATE_CREATED_ESFN, include_in_all = false, index = FieldIndexingVariants.no)
}

import EMDateCreatedStatic._


trait EMDateCreatedStatic[T <: EMDateCreated[T]] extends EsModelStaticT[T] {
  abstract override def generateMappingProps: List[DocField] = {
    fieldDate :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (DATE_CREATED_ESFN, value) =>
        acc.dateCreated = dateCreatedParser(value)
    }
  }
}


trait EMDateCreated[T <: EMDateCreated[T]] extends EsModelT[T] {

  var dateCreated: DateTime

  abstract override def writeJsonFields(acc: XContentBuilder) {
    super.writeJsonFields(acc)
    acc.field(DATE_CREATED_ESFN, dateCreated)
  }
}
