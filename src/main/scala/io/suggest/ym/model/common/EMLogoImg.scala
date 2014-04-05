package io.suggest.ym.model.common

import io.suggest.model._
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.SioEsUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.14 18:54
 * Description: Аддон для поддержки поля LogoImg, хранящий json object по логотипу с метаданными.
 */

object EMLogoImg {
  val LOGO_IMG_ESFN = "logoImg"

  def esMappingField = FieldObject(LOGO_IMG_ESFN, enabled = false, properties = Nil)
}

import EMLogoImg._

trait EMLogoImgStatic[T <: EMLogoImgMut[T]] extends EsModelStaticT[T] {
  abstract override def generateMappingProps: List[DocField] = {
    esMappingField :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (LOGO_IMG_ESFN, value)  =>
        acc.logoImgOpt = Option(MImgInfo.convertFrom(value))
    }
  }
}

trait EMLogoImg[T <: EMLogoImg[T]] extends EsModelT[T] {

  def logoImgOpt: Option[MImgInfo]

  abstract override def writeJsonFields(acc: XContentBuilder) {
    super.writeJsonFields(acc)
    if (logoImgOpt.isDefined)
      acc.rawField(LOGO_IMG_ESFN, logoImgOpt.get.toJson.getBytes)
  }
}

trait EMLogoImgMut[T <: EMLogoImgMut[T]] extends EMLogoImg[T] {
  var logoImgOpt: Option[MImgInfo]
}
