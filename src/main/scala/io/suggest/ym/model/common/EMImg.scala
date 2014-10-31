package io.suggest.ym.model.common

import io.suggest.event.SioNotifierStaticClientI
import io.suggest.util.SioEsUtil._
import io.suggest.model._
import io.suggest.model.EsModel.FieldsJsonAcc
import org.elasticsearch.client.Client
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import com.typesafe.scalalogging.slf4j.Logger
import play.api.libs.json._
import java.{util => ju}
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin обязательной Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 17:20
 * Description: Поле основной и обязательной картинки.
 * 2014.apr.04: Примитивное поле img: MImgInfo
 * 2014.apr.30: img -> imgOpt: Option[MImgInfo]
 * 2014.apr.30: img -> Map[String, MImgInfo] для потребностей blocks.
 *              В качестве ключа используется произвольная строка. Внутри это всё хранится как JsObject.
 * 2014.may.08: MImgInfo -> MImgInfoT.
 */

object EMImg {
  val IMG_ESFN = "img"
  def esMappingField = FieldObject(IMG_ESFN, enabled = false, properties = Nil)

  type Imgs_t = Map[String, MImgInfoT]

  /** Стереть картинку, указанную в поле imgOpt, если она там есть. */
  def eraseImgs(imgs: Imgs_t)(implicit ec: ExecutionContext, sn: SioNotifierStaticClientI): Future[_] = {
    Future.traverse(imgs) { case (imgId, imgInfo) =>
      val imgId = imgInfo.filename
      lazy val logPrefix = s"eraseLinkedImage($imgId): "
      // Раньше тут был MPict.deleteFully(), который удалял из кассандры.
      val resFut = MImg2Util.deleteFully(imgId)
      resFut onComplete {
        case Success(_)  => LOGGER.trace(logPrefix + "Successfuly erased main picture: " + imgId)
        case Failure(ex) => LOGGER.error(logPrefix + "Failed to delete associated picture: " + imgId, ex)
      }
      resFut
    }
  }
}

import EMImg._


trait EMImgStatic extends EsModelStaticMutAkvT {
  override type T <: EMImgMut

  def LOGGER: Logger

  abstract override def generateMappingProps: List[DocField] = {
    esMappingField :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (IMG_ESFN, value: ju.Map[_,_]) =>
        // 2014.oct.31: Код обработки старого формата (<2014.apr.30) удалён.
        acc.imgs = value.foldLeft [List[(String, MImgInfo)]] (Nil) {
          case (mapAcc, (k, v)) =>
            k.toString -> MImgInfo.convertFrom(v) :: mapAcc
        }.toMap
    }

  }

}


trait Imgs {
  def imgs: Imgs_t
}

trait EMImgI extends EsModelPlayJsonT with Imgs {
  override type T <: EMImgI
}

trait EMImg extends EMImgI {
  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc0 = super.writeJsonFields(acc)
    if (imgs.nonEmpty) {
      // Переводим карту в JsObject стандартным образом.
      val jsonAcc1 = imgs.foldLeft [List[(String, JsValue)]] (Nil) {
        case (jsonAcc, (k, imgInfo)) =>
          k -> imgInfo.toPlayJson :: jsonAcc
      }
      IMG_ESFN -> JsObject(jsonAcc1) :: acc0
    } else {
      acc0
    }
  }

  /** Стирание ресурсов, относящихся к этой модели. */
  override def eraseResources(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    val fut = super.eraseResources
    EMImg.eraseImgs(imgs)
      .flatMap { _ => fut }
  }
}

trait EMImgMut extends EMImg {
  override type T <: EMImgMut
  var imgs: Imgs_t
}
