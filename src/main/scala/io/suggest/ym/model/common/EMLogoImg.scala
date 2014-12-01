package io.suggest.ym.model.common

import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model._
import io.suggest.util.SioEsUtil._
import io.suggest.model.EsModel.FieldsJsonAcc
import org.elasticsearch.client.Client

import scala.concurrent.{ExecutionContext, Future}

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


trait EMLogoImgStatic extends EsModelStaticMutAkvT {
  override type T <: EMLogoImgMut

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


trait LogoImgOptI {
  def logoImgOpt: Option[MImgInfoT]
}

trait EMLogoImgI extends EsModelPlayJsonT with LogoImgOptI {
  override type T <: EMLogoImgI
}

trait EMLogoImg extends EMLogoImgI {
  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc0 = super.writeJsonFields(acc)
    if (logoImgOpt.isDefined)
      LOGO_IMG_ESFN -> logoImgOpt.get.toPlayJson :: acc0
    else
      acc0
  }

  override def doEraseResources(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    val fut = super.doEraseResources
    logoImgOpt.fold(fut) { img =>
      MImg2Util.deleteFully(img.filename)
        .flatMap { _ => fut }
    }
  }
}

trait EMLogoImgMut extends EMLogoImg {
  override type T <: EMLogoImgMut
  var logoImgOpt: Option[MImgInfoT]
}
