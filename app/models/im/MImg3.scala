package models.im

import java.util.UUID

import io.suggest.model.img.{ImgSzDated, IImgMeta}
import io.suggest.util.UuidUtil
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Enumerator
import util.PlayLazyMacroLogsImpl
import util.event.SiowebNotifier.Implicts.sn

import scala.concurrent.Future
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.09.15 17:27
 * Description: Реализация модели [[MImgT]] на базе MMedia, вместо прямого взаимодействия с кассандрой.
 */
case class MImg3(nodeId: String,
                 override val dynImgOps: Seq[ImOp])
  extends MImgT
  with PlayLazyMacroLogsImpl
{

  override lazy val rowKey: UUID = {
    UuidUtil.base64ToUuid(nodeId)
  }

  override type MImg_t = MImg3
  override def thisT: MImg_t = this
  override def toWrappedImg = this

  lazy val _mediaId = MMedia.mkId(rowKeyStr, qOpt)

  lazy val _mediaOptFut = MMedia.getById(_mediaId)
  lazy val _mediaFut = _mediaOptFut.map(_.get)

  override protected lazy val _getImgMeta: Future[Option[IImgMeta]] = {
    _mediaOptFut map { mmediaOpt =>
      mmediaOpt.map { mmedia =>
        ImgSzDated(
          sz          = mmedia.picture.get,
          dateCreated = mmedia.file.dateCreated
        )
      }
    }
  }

  override def existsInPermanent: Future[Boolean] = {
    _mediaFut
      .flatMap { _.storage.isExist }
      .recover { case ex: Throwable =>
        if (!ex.isInstanceOf[NoSuchElementException])
          LOGGER.warn("isExist() or _mediaFut failed / " + this, ex)
        false
      }
  }

  override protected def _getImgBytes2: Enumerator[Array[Byte]] = {
    val fut = _mediaFut.map { mm =>
      mm.storage.read
    }
    Enumerator.flatten(fut)
  }

  override protected def _doSaveToPermanent(loc: MLocalImgT): Future[_] = {
    _mediaFut.flatMap { mm =>
      mm.storage.write( loc.imgBytesEnumerator )
    }
  }

  override protected def _updateMetaWith(localWh: MImgSizeT, localImg: MLocalImgT): Unit = {
    // should never happen
    // Необходимость апдейта метаданных возникает, когда обнаруживается, что нет метаднных.
    // В случае N2 MMedia, метаданные без блоба существовать не могут, и необходимость не должна наступать.
    LOGGER.warn(s"_updateMetaWith($localWh, $localImg) ignored and not implemented")
  }

  override def withDynOps(dynImgOps2: Seq[ImOp]): MImg3 = {
    copy(dynImgOps = dynImgOps2)
  }

  override def delete: Future[_] = {
    _mediaOptFut flatMap {
      case Some(mm) =>
        for {
          _ <- mm.storage.delete
          _ <- mm.delete
        } yield {
          true
        }
      case None =>
        Future successful false
    }
  }

}
