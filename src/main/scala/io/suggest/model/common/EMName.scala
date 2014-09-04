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

  def extractName(m: collection.Map[String, AnyRef]): String = {
    stringParser(m(NAME_ESFN))
  }

}


import EMName._

trait EMNameStatic extends EsModelCommonStaticT {
  override type T <: EMName

  abstract override def generateMappingProps: List[DocField] = {
    FieldString(NAME_ESFN, FieldIndexingVariants.not_analyzed, include_in_all = false) ::
      super.generateMappingProps
  }
}

trait EMNameStaticMut extends EsModelStaticMutAkvT with EMNameStatic {
  override type T <: EMNameMut

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (NAME_ESFN, value)  => acc.name = stringParser(value)
    }
  }
}


trait EMName extends EsModelPlayJsonT {
  override type T <: EMName
  def name: String

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    (NAME_ESFN, JsString(name)) :: super.writeJsonFields(acc)
  }

}


trait EMNameMut extends EMName {
  override type T <: EMNameMut
  var name: String
}
