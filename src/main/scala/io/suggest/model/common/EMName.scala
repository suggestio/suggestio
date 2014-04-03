package io.suggest.model.common

import io.suggest.model._
import org.elasticsearch.common.xcontent.XContentBuilder
import EsModel.{NAME_ESFN, nameParser}
import io.suggest.util.SioEsUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 16:25
 * Description: Аддон для es-моделей с полем name.
 */

trait EMNameStatic[T <: EMName[T]] extends EsModelStaticT[T] {

  abstract override def generateMappingProps: List[DocField] = {
    FieldString(NAME_ESFN, FieldIndexingVariants.not_analyzed, include_in_all = false) ::
      super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (NAME_ESFN, value)  => acc.name = nameParser(value)
    }
  }
}


trait EMName[T <: EMName[T]] extends EsModelT[T] {

  var name: String

  abstract override def writeJsonFields(acc: XContentBuilder) {
    super.writeJsonFields(acc)
    acc.field(name)
  }
}
