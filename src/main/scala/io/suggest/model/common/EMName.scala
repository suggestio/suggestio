package io.suggest.model.common

import io.suggest.model._
import io.suggest.model.EsModel.{FieldsJsonAcc, stringParser}
import io.suggest.util.SioEsUtil._
import play.api.libs.json.JsString

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 16:25
 * Description: Аддон для es-моделей с полем name.
 */

object EMName {
  val NAME_ESFN = "name"
}

import EMName._

trait EMNameStatic extends EsModelStaticT {
  override type T <: EMName

  abstract override def generateMappingProps: List[DocField] = {
    FieldString(NAME_ESFN, FieldIndexingVariants.not_analyzed, include_in_all = false) ::
      super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (NAME_ESFN, value)  => acc.name = stringParser(value)
    }
  }
}


trait EMName extends EsModelT {
  override type T <: EMName

  var name: String

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    (NAME_ESFN, JsString(name)) :: super.writeJsonFields(acc)
  }

}
