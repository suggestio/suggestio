package io.suggest.model.common

import io.suggest.model._
import org.elasticsearch.common.xcontent.XContentBuilder
import EsModel._
import io.suggest.util.SioEsUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 17:16
 * Description: Аддон для Es-моделей, имеющих необязательное поле для id логотипа: магазины, тц и т.д.
 */

trait EMLogoImgIdStatic[T <: EMLogoImgId[T]] extends EsModelStaticT[T] {
  abstract override def generateMappingProps: List[DocField] = {
    FieldString(LOGO_IMG_ID_ESFN, include_in_all = false, index = FieldIndexingVariants.no) ::
    super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = super.applyKeyValue(acc) orElse {
    case (LOGO_IMG_ID_ESFN, value)         => acc.logoImgId   = Option(stringParser(value))
  }
}


trait EMLogoImgId[T <: EMLogoImgId[T]] extends EsModelT[T] {
  var logoImgId: Option[String]

  abstract override def writeJsonFields(acc: XContentBuilder) {
    super.writeJsonFields(acc)
    if (logoImgId.isDefined)
      acc.field(LOGO_IMG_ID_ESFN, logoImgId.get)
  }
}
