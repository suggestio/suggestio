package util.img

import java.nio.ByteBuffer
import java.util.UUID

import io.suggest.util.UuidUtil
import models.im.MImg
import net.sf.jmimemagic.Magic
import org.im4java.core.Info
import play.api.mvc.QueryStringBindable
import util.img.LogoUtil.LogoOpt_t
import util.{AsyncUtil, FormUtil, PlayMacroLogsImpl}
import io.suggest.img.{ImgCropParsers, ConvertModes, ImgCrop, SioImageUtilT}
import play.api.Play.{current, configuration}
import io.suggest.model.MPict
import scala.concurrent.{Future, future}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import java.io.{File, FileNotFoundException}
import org.apache.commons.io.FileUtils
import scala.util.{Failure, Success}
import java.lang
import com.fasterxml.jackson.annotation.JsonIgnore
import models._
import play.api.cache.Cache
import io.suggest.ym.model.common.{MImgSizeT, MImgInfoT}
import play.api.Logger
import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.02.14 15:14
 * Description: Для работы с загружаемыми картинками используются эти вспомогательные функции.
 */

object ImgFormUtil extends PlayMacroLogsImpl {
  import play.api.data.Forms._
  import play.api.data.Mapping
  import LOGGER._

  private val IIK_MAXLEN = 80

  // Ключи в карте MUserImgMeta, которые хранят данные о картинке.
  val IMETA_WIDTH  = "w"
  val IMETA_HEIGHT = "h"

  /** Включение ревалидации уже сохраненных картинок при обновлении позволяет убирать картинки "дырки",
    * появившиеся в ходе ошибочной логики. */
  private val REVALIDATE_ALREADY_SAVED_IMGS = configuration.getBoolean("img.update.revalidate.already.saved") getOrElse false


  // TODO Нужна тут подпись через MAC? Или отдельными мапперами запилить?

  /** Маппер для поля с id картинки. Используется обертка над id чтобы прозрачно различать tmp и orig картинки. */
  def imgIdM: Mapping[MImg] = {
    nonEmptyText(minLength = 8, maxLength = IIK_MAXLEN)
      .transform[MImg](MImg.apply, _.fileName)
  }

  /** маппер для поля с id картинки, который может отсутствовать. */
  def imgIdOptM: Mapping[Option[MImg]] = {
    optional(text(maxLength = IIK_MAXLEN))
      .transform[Option[MImg]](
        {txtOpt =>
          try {
            txtOpt
              .filter(_.length >= 8)
              .map(MImg.apply)
          } catch {
            case ex: Exception =>
              debug("imgIdOptM.apply: Cannot parse img id key: " + txtOpt, ex)
              None
          }
        },
        { _.map(_.fileName) }
      )
  }


  /** Генератор logo-маппингов. */
  def getLogoKM(errorMsg: String, marker: String): (String, Mapping[LogoOpt_t]) = {
    "logoImgId" -> optional(imgIdM)
  }


  // Валидация значений crop'а.
  /** Минимальный размер откропанной стороны. */
  private val CROP_SIDE_MIN_PX = configuration.getInt("img.crop.side.max") getOrElse 10
  /** Максимальный размер откропанной стороны. */
  private val CROP_SIDE_MAX_PX = configuration.getInt("img.crop.side.max") getOrElse 2048
  /** Максимальный размер сдвига относительно левого верхнего узла. */
  private val CROP_OFFSET_MAX_PX = configuration.getInt("img.crop.offset.max") getOrElse 4096

  /** Проверка одной стороны кропа на соотвествие критериям. */
  private def isCropSideValid(sideVal: Int): Boolean = {
    sideVal >= CROP_SIDE_MIN_PX  &&  sideVal <= CROP_SIDE_MAX_PX
  }
  /** Проверка одного параметра сдвига кропа на соотвествие критериям. */
  private def isCropOffsetValid(offsetVal: Int): Boolean = {
    offsetVal >= 0 && offsetVal <= CROP_OFFSET_MAX_PX
  }

  /** Маппинг обязательного параметра кропа на реальность. */
  def imgCropM: Mapping[ImgCrop] = {
    nonEmptyText(minLength = 4, maxLength = 16)
      .transform[Option[ImgCrop]] (ImgCrop.maybeApply, _.map(_.toCropStr).getOrElse(""))
      .verifying("crop.invalid", _.isDefined)
      .transform[ImgCrop](_.get, Some.apply)
      .verifying("crop.height.invalid",   {crop => isCropSideValid(crop.height)} )
      .verifying("crop.width.invalid",    {crop => isCropSideValid(crop.width)} )
      .verifying("crop.offset.x.invalid", {crop => isCropOffsetValid(crop.offX)} )
      .verifying("crop.offset.y.invalid", {crop => isCropOffsetValid(crop.offY)} )
  }


  private def imgCropOptM: Mapping[Option[ImgCrop]] = {
    val txtM = text(maxLength = 16).transform(FormUtil.strTrimSanitizeLowerF, FormUtil.strIdentityF)
    optional(txtM)
      .transform[Option[String]] (_.filter(!_.isEmpty), identity)
      .transform[Option[ImgCrop]] (_.flatMap(ImgCrop.maybeApply), _.map(_.toCropStr))
  }


  def updateOrigImgId(needImgs: Seq[MImg], oldImgIds: Iterable[String]): Future[Seq[MImg]] = {
    updateOrigImgFull(needImgs, oldImgIds.map(MImg(_)))
  }

  /** Комбо из updateOrigImgFull() и уже выпиленного метода. */
  @deprecated("Use updateOrigImgFull() instead.", "2014.oct.29")
  def updateOrigImg(needImgs: Seq[MImg], oldImgs: Iterable[MImg]): Future[Option[MImg]] = {
    updateOrigImgFull(needImgs, oldImgs)
      .map { _.headOption } // TODO Надо избегать такого веселья, удалив этот метод начисто.
  }

  /**
   * Замена иллюстраций к некоей сущности.
   * @param needImgs Необходимые в результате набор картинок. Тут могут быть как уже сохранённыя картинка,
   *                 так и новая из tmp. Если Nil, то старые картинки будут удалены.
   * @param oldImgs Уже сохранённые ранее картинки, если есть.
   * @return Список id новых и уже сохранённых картинок.
   */
  def updateOrigImgFull(needImgs: Seq[MImg], oldImgs: Iterable[MImg]): Future[Seq[MImg]] = {
    // Защита от какой-либо деятельности в случае полного отсутствия входных данных.
    if (needImgs.isEmpty && oldImgs.isEmpty) {
      Future successful Nil
    } else {
      updateOrigImgFullDo(needImgs, oldImgs = oldImgs)
    }
  }
  private def updateOrigImgFullDo(needImgs: Seq[MImg], oldImgs: Iterable[MImg]): Future[Seq[MImg]] = {
    val needImgsIndexed = needImgs.zipWithIndex

    // Разделяем на картинки, которые уже были, и которые затребованы для отправки в хранилище:
    val newOldImgsMapFut = {
      Future.traverse(needImgsIndexed) { case a @ (img, i) =>
        img.existsInPermanent map { existsInPermanent =>
          (a, existsInPermanent)
        }
      } map { results =>
        results
          .groupBy(_._2)
          .mapValues(_.map(_._1))
      }
    }

    // Готовим список картинок, которые не надо пересохранять:
    val oldImgIdsSet = oldImgs
      .iterator
      .map(_.rowKey)
      .toSet
    var imgsKeepFut: Future[Seq[(MImg, Int)]] = newOldImgsMapFut.map { m =>
      m.getOrElse(true, Nil)
        // Ксакеп Вася попытается подставить id уже сохраненной где-то картинки. А потом честно запросить удаление этой картинки.
        // Нужно исключить возможность подмешивать в списки картинок левые id, используя список oldImgs:
        .filter { v =>
          val filterResult = oldImgIdsSet contains v._1.rowKey
          if (!filterResult)
            warn("Tried to keep image, that does not exists in previous imgs: " + v._1.rowKeyStr)
          filterResult
        }
    }

    // Если включена принудительная ревалидация неизменившихся картинок, запускаем параллельную ревалидацию для keeped-картинок.
    if (REVALIDATE_ALREADY_SAVED_IMGS) {
      imgsKeepFut = imgsKeepFut flatMap { res =>
        Future.traverse(res) { v =>
          v._1
            .toLocalImg
            .map { localOpt =>
            localOpt
              .filter(_.isExists)
              .map(_ => v)
            }
        } map {
          _.flatten
        }
      }
    }

    // TODO при изменении настроек кропа надо удалять все картинки из хранилищ кроме оригинала.

    // Новые загруженные картинки - копируем в постоянное хранилище:
    val imgsSaveFut: Future[Seq[(MImg, Int)]] = newOldImgsMapFut.flatMap { m =>
      // Готовим список картинок для отправки в хранилище:
      val imgs4s = m.getOrElse(false, Nil)
        .filter { v =>
          val filterResult = v._1.toLocalInstance.isExists
          if (!filterResult)
            warn("Request to save totally inexisting img: " + v._1.fileName)
          filterResult
        }
      // Сохраняем все картинки параллельно:
      Future.traverse(imgs4s) { v =>
        v._1
          .saveToPermanent
          .map { _ => v }
      }
    }

    // Объединяем фьючерсы, восстанавливаем исходный порядок картинок:
    for {
      imgsKeeped <- imgsKeepFut
      imgsSaved  <- imgsSaveFut
    } yield {
      (imgsKeeped ++ imgsSaved)
        .sortBy(_._2)
        .map(_._1)
    }
  }


  def img2imgInfo(mimg: MImg): Future[MImgInfo] = {
    mimg.getImageWH map { wh =>
      MImgInfo(mimg.fileName, wh)
    }
  }
  def img2SomeImgInfo(mimg: MImg): Future[Option[MImgInfo]] = {
    img2imgInfo(mimg)
      .map { Some.apply }
  }

  def optImg2OptImgInfo(mimgOpt: Option[MImg]): Future[Option[MImgInfo]] = {
    mimgOpt.fold [Future[Option[MImgInfo]]]
      { Future successful None }
      { img2SomeImgInfo }
  }

  /**
   * Проверить и уточнить значение кропа картинки.
   * До 2014.oct.08 была замечена проблема с присылаемым на сервер кропом, а после dyn-img это обрушивало всю красоту системы.
   * Баг был в том, что кроп уходил за пределы картинки.
   * @param crop Кроп, присланный клиентом.
   * @param targetSz Целевой размер картинки. TODO 2014.10.08 Размер не обязательно корректный, т.к. приходит с клиента.
   * @param srcSz Исходный размер картинки.
   * @return Пофикшенный crop.
   */
  def repairCrop(crop: ImgCrop, targetSz: MImgSizeT, srcSz: MImgSizeT): ImgCrop = {
    var newCrop = crop
    // Пофиксить offset'ы кропа. Кроп может по сдвигу уезжать за пределы допустимой ширины/длины.
    if (crop.offX + crop.width > srcSz.width)
      newCrop = crop.copy(offX = srcSz.width - crop.width)
    if (crop.offY + crop.height > srcSz.height)
      newCrop = crop.copy(offY = srcSz.height - crop.height)
    newCrop
  }


  def identifyInfo2md(info: Info): Map[String, String] = {
    Map(
      IMETA_WIDTH  -> info.getImageWidth.toString,
      IMETA_HEIGHT -> info.getImageHeight.toString
    )
  }


  /** Приведение выхлопа мапперов imgId к результату сохранения, минуя это самое сохранение. */
  implicit def logoOpt2imgInfo(logoOpt: LogoOpt_t): Option[MImgInfo] = {
    logoOpt.map { logo => MImgInfo(logo.fileName) }
  }

  /** Конвертер данных из готовых MImgInfo в [[OrigImgIdKey]]. */
  implicit def imgInfo2imgKey(m: MImgInfoT): OrigImgIdKey = {
    m match {
      case oiik: OrigImgIdKey =>
        oiik
      case _ =>
        OrigImgIdKey(m.filename, m.meta)
    }
  }

}

/** Базовый трейт для sioweb-реализаций image util. */
sealed trait SiowebImageUtilT extends SioImageUtilT with PlayMacroLogsImpl


/** Резайзилка картинок, используемая для генерация "оригиналов", т.е. картинок, которые затем будут кадрироваться. */
object OrigImageUtil extends SiowebImageUtilT {
  /** Если на выходе получилась слишком жирная превьюшка, то отсеять её. */
  override def MAX_OUT_FILE_SIZE_BYTES: Option[Int] = None

  /** Картинка считается слишком маленькой для обработки, если хотя бы одна сторона не превышает этот порог. */
  override val MIN_SZ_PX: Int = configuration.getInt("img.orig.sz.min.px") getOrElse 256

  /** Если исходный jpeg после стрипа больше этого размера, то сделать resize.
    * Иначе попытаться стрипануть icc-профиль по jpegtran, чтобы снизить размер без пересжатия. */
  override val MAX_SOURCE_JPEG_NORSZ_BYTES: Option[Long] = {
    val kib = configuration.getInt("img.org.preserve.src.max.len.kib").getOrElse(90)
    Some(kib * 1024L)
  }

  /** Качество сжатия jpeg. */
  override val JPEG_QUALITY_PC: Double = configuration.getDouble("img.orig.jpeg.quality") getOrElse 90.0

  /** Максимальный размер сторон будущей картинки (новая картинка должна вписываться в
    * прямоугольник с указанныыми сторонами). */
  override val DOWNSIZE_HORIZ_PX: Integer  = Integer valueOf (configuration.getInt("img.orig.maxsize.h.px") getOrElse 2048)
  override val DOWNSIZE_VERT_PX:  Integer  = configuration.getInt("img.orig.maxsize.v.px").map(Integer.valueOf) getOrElse DOWNSIZE_HORIZ_PX

  override def GAUSSIAN_BLUG: Option[lang.Double] = None
}


/** Обработка логотипов. */
object AdnLogoImageUtil extends SiowebImageUtilT {

  /** Максимальный размер сторон будущей картинки (новая картинка должна вписываться в
    * прямоугольник с указанныыми сторонами). */
  val DOWNSIZE_HORIZ_PX: Integer = Integer valueOf (configuration.getInt("img.logo.shop.maxsize.h.px") getOrElse 512)
  val DOWNSIZE_VERT_PX: Integer  = Integer valueOf (configuration.getInt("img.logo.shop.maxsize.v.px") getOrElse 128)

  /** Качество сжатия jpeg. */
  val JPEG_QUALITY_PC: Double = configuration.getDouble("img.logo.shop.jpeg.quality") getOrElse 0.95

  /** Если исходный jpeg после стрипа больше этого размера, то сделать resize.
    * Иначе попытаться стрипануть icc-профиль по jpegtran, чтобы снизить размер без пересжатия. */
  def MAX_SOURCE_JPEG_NORSZ_BYTES: Option[Long] = None

  /** Картинка считается слишком маленькой для обработки, если хотя бы одна сторона не превышает этот порог. */
  val MIN_SZ_PX: Int = configuration.getInt("img.logo.shop.side.min.px") getOrElse 30

  /** Если на выходе получилась слишком жирная превьюшка, то отсеять её. */
  def MAX_OUT_FILE_SIZE_BYTES: Option[Int] = None
}


/** Там, где допустимы квалратные логотипы, используем этот трайт. */
trait SqLogoImageUtil extends SiowebImageUtilT {

  /** Максимальный размер сторон будущей картинки (новая картинка должна вписываться в
    * прямоугольник с указанныыми сторонами). */
  val DOWNSIZE_HORIZ_PX: Integer = Integer valueOf (configuration.getInt("img.logo.mart.maxsize.h.px") getOrElse 512)
  val DOWNSIZE_VERT_PX: Integer  = configuration.getInt("img.logo.mart.maxsize.v.px").map(Integer valueOf) getOrElse DOWNSIZE_HORIZ_PX

  /** Качество сжатия jpeg. */
  val JPEG_QUALITY_PC: Double = configuration.getDouble("img.logo.mart.jpeg.quality") getOrElse 0.95

  /** Картинка считается слишком маленькой для обработки, если хотя бы одна сторона не превышает этот порог. */
  val MIN_SZ_PX: Int = configuration.getInt("img.logo.mart.side.min.px") getOrElse 70

  /** Если на выходе получилась слишком жирная превьюшка, то отсеять её. */
  val MAX_OUT_FILE_SIZE_BYTES: Option[Int] = configuration.getInt("img.logo.mart.result.size.max")

  /** Если исходный jpeg после стрипа больше этого размера, то сделать resize.
    * Иначе попытаться стрипануть icc-профиль по jpegtran, чтобы снизить размер без пересжатия. */
  def MAX_SOURCE_JPEG_NORSZ_BYTES: Option[Long] = None

}


/** Конвертор картинок в логотипы ТЦ. */
object LogoImageUtil extends SqLogoImageUtil


@deprecated("Use models.im.MImg instead", "2014.oct.27")
object ImgIdKey {
  def apply(key: String): ImgIdKey = {
    if (key startsWith MPictureTmp.KEY_PREFIX) {
      TmpImgIdKey(key)
    } else {
      OrigImgIdKey(key)
    }
  }

  def origQualifier(cropOpt: Option[ImgCrop]) = cropOpt.fold(MPict.Q_USER_IMG_ORIG) { _.toCropStr }


  /** QSB для id картинки. */
  implicit def iikQsb(implicit strB: QueryStringBindable[String]) = {
    new QueryStringBindable[ImgIdKey] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ImgIdKey]] = {
        strB.bind(key, params) map {
          _.right.flatMap { rawIik =>
            try {
              Right(ImgIdKey.apply(rawIik))
            } catch {
              case ex: Throwable =>
                Left("Img id is invalid.")
            }
          }
        }
      }

      override def unbind(key: String, value: ImgIdKey): String = {
        strB.unbind(key, value.filename)
      }
    }
  }

}

@deprecated("Use models.im.MImg instead", "2014.oct.27")
sealed trait ImgIdKey {
  def filename: String
  def isExists: Future[Boolean]
  def isValid: Boolean
  def isTmp: Boolean
  override def hashCode = filename.hashCode
  def cropOpt: Option[ImgCrop]
  def getBaseImageWH: Future[Option[MImgInfoMeta]]
  def getImageWH: Future[Option[MImgInfoMeta]]

  /** Метаданные картинки в некотором очевидном виде. */
  def getImageMeta: Future[Option[ImgMetaI]]

  /** Выдать во временный файл MPictureTmp. */
  def toTempPict: Future[MPictureTmp]

  /** Выдать оригинал во временный файл MPictureTmp. */
  def toTempPictOrig: Future[MPictureTmp]

  // Определение column qualifier для сохранения/чтения картинки по этому ключу.
  def origQualifierOpt = cropOpt.map { _.toCropStr }
  def origQualifier = ImgIdKey.origQualifier(cropOpt)

  /** Является ли эта картинка кадрированной производной?
    * @return false, если картинка оригинальная. true если откадрированная картинка.
    */
  def isCropped = cropOpt.isDefined

  def uncropped: ImgIdKey
}


@deprecated("Use models.im.MImg instead", "2014.oct.27")
object TmpImgIdKey {

  val GET_IMAGE_WH_CACHE_DURATION = configuration.getInt("tiik.getImageWH.cache.minutes").getOrElse(1).minutes

  def apply(filename: String): TmpImgIdKey = {
    val mptmp = MPictureTmp(filename)
    TmpImgIdKey(filename, mptmp)
  }
  def apply(mptmp: MPictureTmp): TmpImgIdKey = {
    val filename = mptmp.filename
    TmpImgIdKey(filename, mptmp)
  }

}

@deprecated("Use models.im.MImg instead", "2014.oct.27")
case class TmpImgIdKey(filename: String, @JsonIgnore mptmp: MPictureTmp) extends ImgIdKey with MImgInfoT {

  @JsonIgnore
  override def cropOpt = mptmp.data.cropOpt

  @JsonIgnore
  override def isTmp: Boolean = true

  @JsonIgnore
  override def isExists: Future[Boolean] = Future successful isValid

  @JsonIgnore
  override def isValid: Boolean = mptmp.isExist

  @JsonIgnore
  override def getBaseImageWH: Future[Option[MImgInfoMeta]] = {
    // TODO Нужно считать размер для неоткропанной картинки, если текущая уже откропана.
    getImageWH
  }

  @JsonIgnore
  override def meta: Option[MImgInfoMeta] = None

  @JsonIgnore
  override def getImageWH: Future[Option[MImgInfoMeta]] = {
    // Кеширование позволяет избежать замеров размеров картинки.
    Cache.getOrElse(filename + ".tgIWH", expiration = TmpImgIdKey.GET_IMAGE_WH_CACHE_DURATION.toSeconds.toInt) {
      // Для синхронного вызова identify() используем отдельный поток в отдельном пуле.
      // Распараллеливание заодно поможет сразу закинуть в кеш данный фьючерс.
      val identifyFut = Future {
        val info = OrigImageUtil.identify(mptmp.file)
        val imeta = MImgInfoMeta(
          height = info.getImageHeight,
          width = info.getImageWidth
        )
        Some(imeta)
      }(AsyncUtil.jdbcExecutionContext)
      identifyFut recover {
        case ex: org.im4java.core.InfoException =>
          Logger(getClass).info("getImageWH(): Unable to identity image " + filename, ex)
          None
      }
    }
  }

  /** Метаданные картинки в некотором очевидном виде. */
  override def getImageMeta: Future[Option[ImgMetaI]] = {
    val whOptFut = getImageWH
    val timestamp = mptmp.file.lastModified()
    whOptFut map { whOpt =>
      whOpt map { wh =>
        new ImgMetaI {
          override lazy val md: Map[String, String] = {
            Map(
              ImgFormUtil.IMETA_WIDTH  ->  wh.width.toString,
              ImgFormUtil.IMETA_HEIGHT ->  wh.height.toString
            )
          }

          override val timestampMs: Long = timestamp
        }
      }
    }
  }

  @JsonIgnore
  override def uncropped: TmpImgIdKey = {
    if (isCropped) {
      val mptmpData1 = mptmp.data.copy(cropOpt = None)
      val mptmp1 = MPictureTmp(mptmpData1)
      TmpImgIdKey(mptmp1)
    } else {
      this
    }
  }

  @JsonIgnore
  override def toTempPict: Future[MPictureTmp] = {
    Future successful mptmp
  }

  @JsonIgnore
  override def toTempPictOrig: Future[MPictureTmp] = {
    Future successful uncropped.mptmp
  }
}


@deprecated("Use models.im.MImg instead", "2014.oct.27")
object OrigImgIdKey extends ImgCropParsers {
  import ImgFormUtil.{IMETA_HEIGHT, IMETA_WIDTH}

  val FILENAME_PARSER: Parser[OrigImgData] = {
    "(?i)[0-9a-z_-]+".r ~ opt("~" ~> cropStrP) ^^ {
      case rowKey ~ cropOpt =>
        OrigImgData(rowKey, cropOpt)
    }
  }

  val IMAGE_WH_CACHE_DURATION = configuration.getInt("oiik.getImageWH.cache.minutes").getOrElse(1).minutes

  def parseFilename(filename: String) = parse(FILENAME_PARSER, filename)

  def apply(filename: String): OrigImgIdKey = {
    apply(filename, meta = None)
  }
  def apply(filename: String, meta: Option[MImgInfoMeta]): OrigImgIdKey = {
    val data = OrigImgIdKey.parseFilename(filename).get
    apply(filename, meta, data)
  }
  def apply(data: OrigImgData): OrigImgIdKey = {
    apply(data, meta = None)
  }
  def apply(data: OrigImgData, meta: Option[MImgInfoMeta]): OrigImgIdKey = {
    val filename = data.toFilename
    apply(filename, meta, data)
  }
  def apply(filename: String, data: OrigImgData): OrigImgIdKey = {
    apply(filename, meta = None, data = data)
  }

  def apply(filename: String, meta: Option[MImgInfoMeta], data: OrigImgData): OrigImgIdKey = {
    new OrigImgIdKey(filename, meta, data)
  }

  def getOrigImageMetaCache(rowKey: String, qOpt: Option[String] = None): Future[Option[MUserImgMeta2]] = {
    // Кеширование сильно ускоряет получение параметров картинки из базы на параллельных и последовательных запросах.
    Cache.getOrElse("gOIWH." + rowKey + qOpt.getOrElse(""),  expiration = IMAGE_WH_CACHE_DURATION.toSeconds.toInt) {
      MUserImgMeta2.getByStrId(rowKey, qOpt)
    }
  }

  /** Прочитать ширину-длину хранимой orig-картинки по модели MUserImgMetadata.
    * Метод довольно статичен, но private чтобы не допускать логических ошибок при передаче параметров
    * (ведь можно ошибочно передать [[OrigImgIdKey.filename]] например -- функция будет вести себя ошибочно при crop).
    * @param rowKey Чистый ключ картинки. Доступен через [[OrigImgIdKey.data.rowKey]].
    * @return Асинхронные метаданные по ширине-высоте картинки.
    */
  def getOrigImageWH(rowKey: String, qOpt: Option[String] = None): Future[Option[MImgInfoMeta]] = {
    getOrigImageMetaCache(rowKey, qOpt)
      .map { imetaOpt =>
        for {
          imeta     <- imetaOpt
          widthStr  <- imeta.md.get(IMETA_WIDTH)
          heightStr <- imeta.md.get(IMETA_HEIGHT)
        } yield {
          MImgInfoMeta(height = heightStr.toInt, width = widthStr.toInt)
        }
      }
      // TODO Можно попытаться прочитать картинку из хранилища, если метаданные по картинке не найдены.
  }

}

// TODO cropOpt нужно убрать отсюда, т.к. кропы старого формата больше не хранятся.
@deprecated("Use Seq[ImOp] instead", "2014.oct.27")
case class OrigImgData(rowKey: String, cropOpt: Option[ImgCrop]) {
  def toFilename: String = {
    var sbSz: Int = rowKey.length
    if (cropOpt.isDefined)
      sbSz += 22
    val sb = new StringBuilder(sbSz, rowKey)
    if (cropOpt.isDefined)
      sb.append('~').append(cropOpt.get.toUrlSafeStr)
    sb.toString()
  }
}

/**
 * Экземпляр ключа картинки, которая лежит в хранилище на постоянном хранении.
 * 2014.oct.08: отключен case-class, т.к. вызов copy() приводил к противоречиям в состоянии класса.
 * @param filename Имя файла. Описывает содержимое data.
 * @param meta Метаданные
 * @param data Данные хранения. Ключ ряда например.
 */
@deprecated("Use models.im.MImg instead", "2014.oct.27")
class OrigImgIdKey(val filename: String, val meta: Option[MImgInfoMeta], val data: OrigImgData)
  extends MImgInfoT with ImgIdKey
{
  @JsonIgnore
  override def isTmp: Boolean = false

  @JsonIgnore
  override def isExists: Future[Boolean] = {
    MUserImg2.getByStrId(data.rowKey, q = None).map(_.isDefined)
  }

  @JsonIgnore
  override def isValid: Boolean = UuidUtil.isUuidStrValid(filename)

  @JsonIgnore
  override def equals(obj: scala.Any): Boolean = {
    // Сравниваем с MImgInfo без учёта поля meta.
    obj match {
      case mii: MImgInfoT => (mii eq this) || (mii.filename == this.filename)
      case _ => false
    }
  }

  /** Метаданные картинки в некотором очевидном виде. */
  override def getImageMeta = OrigImgIdKey.getOrigImageMetaCache(data.rowKey, origQualifierOpt)

  @JsonIgnore
  override def cropOpt = data.cropOpt

  @JsonIgnore
  override def getBaseImageWH = OrigImgIdKey.getOrigImageWH(data.rowKey)

  override def getImageWH = OrigImgIdKey.getOrigImageWH(data.rowKey, origQualifierOpt)

  override def uncropped: OrigImgIdKey = {
    if (isCropped) {
      val data1 = data.copy(cropOpt = None)
      val fn1 = data1.toFilename
      OrigImgIdKey(fn1, meta, data1)
    } else {
      this
    }
  }

  protected def _toTempPict(qOpt: Option[String]): Future[MPictureTmp] = {
    // Кеширование позволяет не выкачивать одну и ту же картинку снова, когда она уже лежит на диске.
    // TODO Следует использовать кеш прямо на диске, имена файлов должны состоять из rowKeyStr и qOpt.
    //      Это разгрузит RAM ценой небольшого iowait. В остальном всё должно быть также, как и с mptmp.
    val ck = filename + ".tTP"
    Cache.getAs[Future[MPictureTmp]](ck) match {
      case Some(resultFut) =>
        // TODO Следует ли отрабатывать ситуацию, когда приходит неправильный фьючерс (файл внезапно уже удалён)?
        // При чтении из кеша можно делать touch на файле и продлевать ttl кеша.
        resultFut onSuccess { case mptmp =>
          mptmp.touch()
          Cache.set(ck, resultFut, expiration = MPictureTmp.DELETE_AFTER)
        }
        resultFut

      case None =>
        // Нужно выкачать из модели оригинальную картинку во временный файл.
        val resultFut = MUserImg2.getByStrId(this.data.rowKey, qOpt) map { oimgOpt =>
          val oimg = oimgOpt.get
          val magicMatch = Magic.getMagicMatch(oimg.imgBytes)
          val oif = OutImgFmts.forImageMime(magicMatch.getMimeType).get
          val mptmp = MPictureTmp.mkNew(None, cropOpt = None, oif)
          mptmp.writeIntoFile(oimg.imgBytes)
          mptmp
        }
        Cache.set(ck, resultFut, expiration = MPictureTmp.DELETE_AFTER)
        resultFut
    }
  }

  override def toTempPict: Future[MPictureTmp] = {
    _toTempPict(origQualifierOpt)
  }

  override def toTempPictOrig: Future[MPictureTmp] = {
    _toTempPict(None)
  }

}


/** Выходные форматы картинок. */
object OutImgFmts extends Enumeration {

  protected class Val(val name: String, val mime: String) extends super.Val(name)

  type OutImgFmt = Val

  val JPEG: OutImgFmt = new Val("jpeg", "image/jpeg")
  val PNG: OutImgFmt  = new Val("png", "image/png")
  val GIF: OutImgFmt  = new Val("gif", "image/gif")
  val SVG: OutImgFmt  = new Val("svg", "image/svg+xml")

  implicit def value2val(x: Value): OutImgFmt = x.asInstanceOf[OutImgFmt]

  /**
   * Предложить формат для mime-типа.
   * @param mime Строка mime-типа.
   * @return OutImgFmt. Если не-image тип, то будет IllegalArgumentException.
   */
  def forImageMime(mime: String): Option[OutImgFmt] = {
    values
      .find(_.mime equalsIgnoreCase mime)
      .asInstanceOf[Option[OutImgFmt]]
  }

}


/**
 * Класс для объединения кропа и id картинки (чтобы не использовать Tuple2 с числовыми названиями полей)
 * @param iik Указатель на картинку.
 * @param withThumb Генерить ли превьюшку? (она будет сохранена рядом).
 * @param withId Использовать указанный id. Если None, то будет сгенерен новый рандомный id.
 * @tparam T Реальный тип iik.
 */
@deprecated("Use models.im.MImg instead", "2014.oct.27")
case class ImgInfo4Save[+T <: ImgIdKey](
  iik       : T,
  withThumb : Boolean = true,
  withId    : Option[String] = None
)


@deprecated("Use models.im.MImg instead", "2014.oct.27")
case class SavedTmpImg(origData: OrigImgData, tii4s: ImgInfo4Save[TmpImgIdKey], meta: MImgInfoMeta) {
  def idStr: String = origData.toFilename
  def oiik = OrigImgIdKey(origData, Some(meta))

  def tmpImgFile = tii4s.iik.mptmp.file
}
