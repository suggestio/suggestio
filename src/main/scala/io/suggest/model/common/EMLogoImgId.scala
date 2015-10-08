package io.suggest.model.common

import io.suggest.model._
import EsModel.{stringParser, FieldsJsonAcc}
import io.suggest.util.SioEsUtil._
import play.api.libs.json.JsString

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 17:16
 * Description: Аддон для Es-моделей, имеющих необязательное поле для id логотипа: магазины, тц и т.д.
 */

object EMLogoImgId {

  val LOGO_IMG_ID_ESFN  = "logoImgId"

}


import EMLogoImgId._


trait EMLogoImgIdStatic extends EsModelStaticMutAkvT {
  override type T <: EMLogoImgId

  abstract override def generateMappingProps: List[DocField] = {
    FieldString(LOGO_IMG_ID_ESFN, include_in_all = false, index = FieldIndexingVariants.no) ::
    super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = super.applyKeyValue(acc) orElse {
    case (LOGO_IMG_ID_ESFN, value)         => acc.logoImgId   = Option(stringParser(value))
  }
}


trait EMLogoImgId extends EsModelPlayJsonT {
  override type T <: EMLogoImgId

  var logoImgId: Option[String]

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc0 = super.writeJsonFields(acc)
    if (logoImgId.isDefined)
      (LOGO_IMG_ID_ESFN, JsString(logoImgId.get)) :: acc0
    else
      acc0
  }

}
