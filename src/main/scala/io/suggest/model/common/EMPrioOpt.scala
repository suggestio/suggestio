package io.suggest.model.common

import io.suggest.model.{EsModel, EsModelStaticT, EsModelT}
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.SioEsUtil._

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

trait EMPrioOptStatic[T <: EMPrioOptMut[T]] extends EsModelStaticT[T] {
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

trait EMPrioOpt[T <: EMPrioOpt[T]] extends EsModelT[T] {

  def prio: Option[Int]

  abstract override def writeJsonFields(acc: XContentBuilder) {
    super.writeJsonFields(acc)
    if (prio.isDefined)
      acc.field(PRIO_ESFN, prio.get)
  }
}

trait EMPrioOptMut[T <: EMPrioOptMut[T]] extends EMPrioOpt[T] {
  var prio: Option[Int]
}
