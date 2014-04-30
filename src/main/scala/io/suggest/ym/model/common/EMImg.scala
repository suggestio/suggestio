package io.suggest.ym.model.common

import io.suggest.util.SioEsUtil._
import io.suggest.model.{MPict, EsModelT, EsModelStaticT}
import io.suggest.model.EsModel.FieldsJsonAcc
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import com.typesafe.scalalogging.slf4j.Logger

/**
 * Suggest.io
 * User: Konstantin обязательной Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 17:20
 * Description: Поле основной и обязательной картинки.
 */

object EMImg {
  val IMG_ESFN = "img"
  def esMappingField = FieldObject(IMG_ESFN, enabled = false, properties = Nil)


  /** Стереть картинку, указанную в поле imgOpt, если она там есть. */
  def eraseImgOpt(imgOpt: Option[MImgInfo])(implicit ec: ExecutionContext): Future[_] = {
    imgOpt match {
      case Some(img) =>
        val imgId = img.id
        val logPrefix = s"eraseLinkedImage($img): "
        MPict.deleteFully(imgId) andThen {
          case Success(_)  => LOGGER.trace(logPrefix + "Successfuly erased main picture: " + imgId)
          case Failure(ex) => LOGGER.error(logPrefix + "Failed to delete associated picture: " + imgId, ex)
        }

      case None => Future successful ()
    }
  }
}

import EMImg._


trait EMImgStatic extends EsModelStaticT {
  override type T <: EMImgMut

  def LOGGER: Logger

  abstract override def generateMappingProps: List[DocField] = {
    esMappingField :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (IMG_ESFN, value)  =>
        acc.imgOpt = Option(MImgInfo.convertFrom(value))
    }
  }

  def eraseImgOpt(impl: T)(implicit ec: ExecutionContext): Future[_] = {
    EMImg.eraseImgOpt(impl.imgOpt)
  }
}

trait ImgOpt {
  def imgOpt: Option[MImgInfo]
}

trait EMImg extends EsModelT with ImgOpt {
  override type T <: EMImg

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc0 = super.writeJsonFields(acc)
    if (imgOpt.isDefined)
      IMG_ESFN -> imgOpt.get.toPlayJson :: acc0
    else
      acc0
  }

}

trait EMImgMut extends EMImg {
  override type T <: EMImgMut
  var imgOpt: Option[MImgInfo]
}
