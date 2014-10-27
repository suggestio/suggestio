package models.im

import java.util.UUID

import io.suggest.model.{MUserImgMeta2, MUserImg2}
import io.suggest.util.UuidUtil
import models.{ImgMetaI, MImgInfoMeta}
import play.api.cache.Cache
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.QueryStringBindable
import util.qsb.QsbSigner
import util.{PlayLazyMacroLogsImpl, AsyncUtil}
import play.api.Play.{current, configuration}
import util.img.ImgFormUtil

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.util.Random

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
object MImg extends PlayLazyMacroLogsImpl {

  import LOGGER._

  val ORIG_META_CACHE_SECONDS: Int = configuration.getInt("m.img.org.meta.cache.ttl.seconds") getOrElse 60

  val SIGN_SUF   = ".sig"
  val IMG_ID_SUF = ".id"


  /** Статический секретный ключ для подписывания запросов к dyn-картинкам. */
  private[models] val SIGN_SECRET: String = {
    val confKey = "dynimg.sign.key"
    configuration.getString(confKey) getOrElse {
      if (play.api.Play.isProd) {
        // В продакшене без ключа нельзя. Генерить его и в логи писать его тоже писать не стоит наверное.
        throw new IllegalStateException(s"""Production mode without dyn-img signature key defined is impossible. Please define '$confKey = ' like 'application.secret' property with 64 length.""")
      } else {
        // В devel/test-режимах допускается использование рандомного ключа.
        val rnd = new Random()
        val len = 64
        val sb = new StringBuilder(len)
        // Избегаем двойной ковычи в ключе, дабы не нарываться на проблемы при копипасте ключа в конфиг.
        @tailrec def nextPrintableCharNonQuote: Char = {
          val next = rnd.nextPrintableChar()
          if (next == '"' || next == '\\')
            nextPrintableCharNonQuote
          else
            next
        }
        for(i <- 1 to len) {
          sb append nextPrintableCharNonQuote
        }
        val result = sb.toString()
        warn(s"""Please define secret key for dyn-img cryto-signing in application.conf:\n  $confKey = "$result" """)
        result
      }
    }
  }

  /** routes-биндер для query-string. */
  implicit def qsb(implicit uuidB: QueryStringBindable[UUID],  imOpsOptB: QueryStringBindable[Option[Seq[ImOp]]]) = {
    new QueryStringBindable[MImg] {

      /** Создать подписывалку для qs. */
      def getQsbSigner(key: String) = new QsbSigner(SIGN_SECRET, s"$key$SIGN_SUF")

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MImg]] = {
        // Собираем результат
        val keyDotted = s"$key."
        for {
          // TODO Надо бы возвращать invalid signature при ошибке, а не not found.
          params2         <- getQsbSigner(key).signedOrNone(keyDotted, params)
          maybeImgId      <- uuidB.bind(key + IMG_ID_SUF, params2)
          maybeImOpsOpt   <- imOpsOptB.bind(keyDotted, params2)
        } yield {
          maybeImgId.right.flatMap { imgId =>
            maybeImOpsOpt.right.map { imOpsOpt =>
              val imOps = imOpsOpt.getOrElse(Nil)
              MImg(imgId, imOps)
            }
          }
        }
      }

      override def unbind(key: String, value: MImg): String = {
        val imgIdRaw = uuidB.unbind(key + IMG_ID_SUF, value.rowKey)
        val imgOpsOpt = if (value.hasImgOps) Some(value.dynImgOps) else None
        val imOpsUnbinded = imOpsOptB.unbind(s"$key.", imgOpsOpt)
        val unsignedResult = if (imOpsUnbinded.isEmpty) {
          imgIdRaw
        } else {
          imgIdRaw + "&" + imOpsUnbinded
        }
        getQsbSigner(key).mkSigned(key, unsignedResult)
      }
    }
  }

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
        val localFut = localInst.metadata
        mimg2Fut
          .filter(_.isDefined)
          .recoverWith {
            case ex: Exception =>
              warn("Unable to read img info meta from remote storage: " + filename, ex)
              localFut
          }
          .recover {
            case ex: Exception =>
              warn("Unable to read img info meta from all models: " + filename, ex)
              None
          }
      } else {
        mimg2Fut
      }
    }
  }

}
