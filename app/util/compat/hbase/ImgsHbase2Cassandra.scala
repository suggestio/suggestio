package util.compat.hbase

import java.nio.ByteBuffer
import java.util.UUID

import io.suggest.model._
import io.suggest.util.{JMXBase, UuidUtil}
import io.suggest.ym.model.common.Imgs
import models.MImgThumb2
import models.MUserImg2
import models.MUserImgMeta2
import org.joda.time.DateTime
import util.PlayLazyMacroLogsImpl
import models._
import util.img.OrigImgIdKey
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.14 19:13
 * Description: Копирование картинок из hbase-моделей в cassandra.
 * Модуль был написан в связи с переездом img-моделей на cassandra.
 * До этого использовалась hbase, которая была в кравлере siobix, но так и не дожила до продакшена.
 * Данные MInviteRequest тут пропускаются, т.к. до запуска это не актуально ни разу.
 */
object ImgsHbase2Cassandra extends PlayLazyMacroLogsImpl {

  import LOGGER._

  /**
   * Обновление содержимого модели MAd.
   * @return Фьючерс c кол-вом отработанных экземпляров модели.
   */
  def updateMAd() = {
    MAd.updateAll() { mad0 =>
      processMAd(mad0) map { imgs2 =>
        mad0.copy(
          imgs = imgs2
        )
      }
    }
  }

  /**
   * Обновление картинок модели MWelcomeAd.
   * @return Фьючерс c кол-вом отработанных экземпляров модели.
   */
  def updateWelcomeAd() = {
    MWelcomeAd.updateAll() { mwa0 =>
      processMAd(mwa0) map { imgs2 =>
        mwa0.copy(
          imgs = imgs2
        )
      }
    }
  }

  /**
   * Обновление картинок модели MAdnNode и содержимого самой модели.
   * @return Фьючерс с кол-во обновлённых узлов.
   */
  def updateMAdnNode() = {
    MAdnNode.updateAll() { adnNode =>
      val gallery2Fut = processAdnNodeGallery(adnNode.gallery)
      for {
        logo2       <- processAdnNodeLogo(adnNode.logoImgOpt)
        gallery2    <- gallery2Fut
      } yield {
        adnNode.copy(
          logoImgOpt = logo2,
          gallery = gallery2
        )
      }
    }
  }


  private def processAdnNodeLogo(logoOpt: Option[MImgInfoT]): Future[Option[MImgInfoT]] = {
    logoOpt.fold(Future successful Option.empty[MImgInfoT]) { logo =>
      processImgInfo(logo)
        .map(Some.apply)
    }
  }

  private def processAdnNodeGallery(gallery: List[String]): Future[List[String]] = {
    Future.traverse(gallery) { galFilename =>
      processImgInfo(galFilename)
        .map(_.filename)
    }
  }


  /** Обработка абстрактной карточки. */
  private def processMAd(mad0: OptStrId with Imgs): Future[Imgs_t] = {
    // Пробегаемся по карте картинок, узнаём картинки из карты,
    Future.traverse( mad0.imgs ) { case (filename, imgInfo) =>
      processImgInfo(imgInfo)
        .map { oiik2 =>
          (oiik2.filename, oiik2)
        }
    } map { imgs2l =>
      // Делаем новую карту imgs и возвращаем её.
      val imgs2: Imgs_t = imgs2l.toMap
      assert(imgs2.size == mad0.imgs.size, "Imgs map sizes does not match")
      // Заливаем новую карту моделей в карточку
      imgs2
    }
  }


  /** Копирование одной картинки, базируясь на инфе img info. */
  private def processImgInfo(imgInfo: MImgInfoT): Future[OrigImgIdKey] = {
    processImgInfo(imgInfo.filename, imgInfo.meta)
  }
  private def processImgInfo(filename: String, meta: Option[MImgInfoMeta] = None): Future[OrigImgIdKey] = {
    val oiik = OrigImgIdKey(filename, meta)
    // Помимо кропаной картинки, надо также захватывать оригинал.
    val qs = List(oiik.origQualifierOpt, None).distinct
    val newId = UUID.randomUUID()
    // Отрабатываем оригиналы и кропы:
    val saveOrigsFut = copyOrigImg(oiik, qs, newId)
    // Копируем метаданные и превьюшки картинок, если они есть.
    val saveMetaFut = copyImgMeta(oiik, qs, newId)
    val saveThumbsFut = copyImgThumbs(oiik, newId)
    // Объединяем всё в один фьючерс. Для укорачивания кода, НЕ используем for().
    saveMetaFut.flatMap { _ =>
      saveThumbsFut.flatMap { _ =>
        saveOrigsFut
      }
    }
  }


  /** Копирование картинки и её кропа между моделями. */
  private def copyOrigImg(oiik: OrigImgIdKey, qs: List[Option[String]], newId: UUID): Future[OrigImgIdKey] = {
    Future.traverse(qs) { qOpt =>
      MUserImgOrig.getById(oiik.data.rowKey, qOpt) map {
        case None =>
          throw new IllegalStateException("Image not found in v1 model: " + oiik.filename)
        case Some(origImg) =>
          // Отправляем картинку в новое хранилище, обновляем хранимые данные картинки.
          origImg
      }
    } flatMap { imgs =>
      // should never happen:
      val oldImgIds = imgs.map(_.idStr).toSet
      assert(oldImgIds.size == 1, "Extracted several same imgs with unexpectedly different ids: " + oldImgIds.mkString(", "))
      // Все картинки успешно извлечены из старой базы. Отправить их в новые модели под новым единым id'шником.
      Future.traverse(imgs) { origImg =>
        val mui2 = MUserImg2(
          id  = newId,
          q   = origImg.q,
          img = ByteBuffer.wrap(origImg.imgBytes),
          timestamp = new DateTime(origImg.timestampMs)
        )
        mui2.save
      } map { _ =>
        // Оригиналы и их кропы сохранены в новых моделях. И есть новый id'шник картинки.
        val idStr = UuidUtil.uuidToBase64(newId)
        val oidata2 = oiik.data.copy(rowKey = idStr)
        OrigImgIdKey(oidata2, oiik.meta)
      }
    }
  }


  /** Копирование метаданных картинки между моделями. */
  private def copyImgMeta(oiik: OrigImgIdKey, qs: List[Option[String]], newId: UUID): Future[_] = {
    Future.traverse(qs) { qOpt =>
      MUserImgMetadata.getById(oiik.data.rowKey, qOpt) flatMap {
        // Метаданные могут отсутствовать вообще. Это на усмотрение контроллеров.
        case None =>
          Future successful Nil
        case Some(oldMeta) =>
          val muim2 = MUserImgMeta2(id = newId, md = oldMeta.md, q = oldMeta.q)
          muim2.save
      }
    }
  }


  /** Копирование thumb'ов между моделями. */
  private def copyImgThumbs(oiik: OrigImgIdKey, newId: UUID): Future[_] = {
    MImgThumb.getFullById(oiik.data.rowKey) flatMap {
      // Thumb может отсутствовать.
      case None =>
        Future successful Nil

      case Some(oldThumb) =>
        val mit2 = MImgThumb2(
          id = newId,
          imageUrl = oldThumb.imageUrlOpt,
          img = ByteBuffer.wrap(oldThumb.thumb),
          timestamp = new DateTime( oldThumb.timestamp )
        )
        mit2.save
    }
  }

}



// JMX доступ к этому модулю

trait ImgsHbase2CassandraJmxMBean {
  def updateMAd(): String
  def updateMWelcomeAd(): String
  def updateMAdnNode(): String
}

class ImgsHbase2CassandraJmx extends JMXBaseHBase with ImgsHbase2CassandraJmxMBean {

  override def updateMAdnNode(): String = {
    ImgsHbase2Cassandra.updateMAdnNode()
      .map { result => s"Nodes updated: $result" }
  }

  override def updateMAd(): String = {
    ImgsHbase2Cassandra.updateMAd()
      .map { result => s"MAds updated: $result" }
  }

  override def updateMWelcomeAd(): String = {
    ImgsHbase2Cassandra.updateWelcomeAd()
      .map { result => s"MWelcomeAds update: $result" }
  }

}

