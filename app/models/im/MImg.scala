package models.im

import java.util.UUID

import io.suggest.model.{MUserImgMeta2, MUserImg2}
import io.suggest.util.UuidUtil
import models.{ImgMetaI, MImgInfoMeta}
import play.api.cache.Cache
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.{PlayLazyMacroLogsImpl, AsyncUtil}
import play.api.Play.{current, configuration}
import util.img.ImgFormUtil

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.10.14 17:36
 * Description: Картинка, хранящаяся где-то в системе. Появилась вследствие объединения OrigImgIdKey и TmpImgIdKey.
 * MPictureTmp была заменена ан im.MLocalImg, а недослой ImgIdKey заменен на MImg, использующий MLocalImg как
 * кеш-модель для моделей MUserImg2 и MUserImgMeta2.
 * Эта модель легковесна и полностью виртуальна, пришла на замену двух IIK-моделей,
 * которые не понимали dynImg-синтаксис.
 * Все данные картинок хранятся в локальной ненадежной кеширующей модели и в постояной моделях (cassandra и др.).
 */
object MImg {

  val ORIG_META_CACHE_SECONDS: Int = configuration.getInt("m.img.org.meta.cache.ttl.seconds") getOrElse 60

}


import MImg._


case class MImg(rowKey: UUID, dynImgOps: Seq[ImOp]) extends ImgFilename with DynImgOpsString with PlayLazyMacroLogsImpl {

  import LOGGER._

  protected def toLocalInstance = MLocalImg(rowKey, dynImgOps)

  lazy val rowKeyStr = UuidUtil.uuidToBase64(rowKey)

  lazy val qOpt: Option[String] = {
    if (dynImgOps.nonEmpty) {
      val q = ImOp.unbindImOps("", dynImgOps, withOrderInx = false)
      Some(q)
    } else {
      None
    }
  }

  /** Дать экземпляр MImg на исходный немодифицированный оригинал. */
  def original: MImg = {
    if (hasImgOps) {
      copy(dynImgOps = Nil)
    } else {
      this
    }
  }

  /** Есть ли операции в dynImgOps? */
  override def hasImgOps: Boolean = dynImgOps.nonEmpty

  /** Имя файла картинки. Испрользуется как сериализованное представление данных по картинке. */
  override lazy val filename: String = super.filename

  /** Прочитать картинку из реального хранилища в файл, если ещё не прочитана. */
  lazy val toLocalPic: Future[Option[MLocalImg]] = {
    val inst = toLocalInstance
    if (inst.isExists) {
      Future successful Some(inst)
    } else {
      MUserImg2.getById(rowKey, qOpt).map { img2Opt =>
        img2Opt.map { mimg2 =>
          inst.writeIntoFile(mimg2.imgBytes)
          inst.touch(mimg2.timestampMs)
          inst
        }
      }(AsyncUtil.jdbcExecutionContext)
    }
  }


  /**
   * Узнать параметры изображения, описываемого экземпляром этой модели.
   * @return Фьючерс с пиксельным размером картинки.
   */
  lazy val getImageWH: Future[Option[MImgInfoMeta]] = {
    Cache.getOrElse(filename + ".giwh", expiration = ORIG_META_CACHE_SECONDS) {
      // Фетчим паралельно из обеих моделей. Кто первая, от той и принимаем данные.
      val mimg2Fut = MUserImgMeta2.getById(rowKey, qOpt)
        .map { imetaOpt =>
          for {
            imeta     <- imetaOpt
            widthStr  <- imeta.md.get(ImgFormUtil.IMETA_WIDTH)
            heightStr <- imeta.md.get(ImgFormUtil.IMETA_HEIGHT)
          } yield {
            MImgInfoMeta(height = heightStr.toInt, width = widthStr.toInt)
          }
        }
      val localInst = toLocalInstance
      if (localInst.isExists) {
        val localFut = localInst.metadata recoverWith {
          case ex: Exception =>
            warn(s"Failed to read local img metadata from ${localInst.filename}", ex)
            mimg2Fut
        }
        val futures = Seq(mimg2Fut, localFut)
        Future firstCompletedOf futures
      } else {
        mimg2Fut
      }
    }
  }

}
