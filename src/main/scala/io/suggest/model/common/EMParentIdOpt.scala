package io.suggest.model.common

import io.suggest.model.EsModel._
import io.suggest.model.{EsModelStaticT, EsModelT}
import play.api.libs.json.JsString
import io.suggest.util.SioEsUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.14 11:14
 * Description: Поле с необязательным parentId: Option[String].
 */

trait EMParentIdOptStatic[T <: EMParentIdOpt[T]] extends EsModelStaticT[T] {
  abstract override def generateMappingProps: List[DocField] = {
    FieldString(PARENT_ID_ESFN, index = FieldIndexingVariants.no, include_in_all = false) ::
    super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (PARENT_ID_ESFN, value)    => acc.parentId = Option(stringParser(value))
    }
  }
}


trait EMParentIdOpt[T <: EMParentIdOpt[T]] extends EsModelT[T] {
  var parentId: Option[String]

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc0 = super.writeJsonFields(acc)
    if (parentId.isDefined)
      PARENT_ID_ESFN -> JsString(parentId.get) :: acc0
    else
      acc0
  }
}
