package io.suggest.model.common

import io.suggest.model._
import org.elasticsearch.common.xcontent.XContentBuilder
import EsModel._
import io.suggest.util.SioEsUtil._
import io.suggest.util.JacksonWrapper

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 17:12
 * Description: Аддон для ES-моделей, имеющих поле person_id во множественном числе.
 */

trait EMPersonIdsStatic[T <: EMPersonIds[T]] extends EsModelStaticT[T] {

  abstract override def generateMappingProps: List[DocField] = {
    FieldString(PERSON_ID_ESFN, include_in_all = false, index = FieldIndexingVariants.not_analyzed) ::
    super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = super.applyKeyValue(acc) orElse {
    case (PERSON_ID_ESFN, value)      => acc.personIds   = JacksonWrapper.convert[Set[String]](value)
  }
}

trait EMPersonIds[T <: EMPersonIds[T]] extends EsModelT[T] {

  def personIds: Set[String]
  def personIds_=(personIds: Set[String])

  abstract override def writeJsonFields(acc: XContentBuilder) {
    super.writeJsonFields(acc)
    if (!personIds.isEmpty)
      acc.array(PERSON_ID_ESFN, personIds.toSeq : _*)
  }
}
