package util.img

import java.nio.ByteBuffer
import java.util.UUID

import io.suggest.util.UuidUtil
import models.im.{MAnyImgT, MImg}
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
        img.original.existsInPermanent map { existsInPermanent =>
          (a, existsInPermanent)
        }
      } map { results =>
        results
          .groupBy(_._2)
          .mapValues(_.map(_._1))
      }
    }

    // Готовим список картинок, которые НЕ надо пересохранять:
    val oldImgIdsSet = mimgs2rkSet(oldImgs)

    // В списке old-картинок могут быть картинки, которые отсутствуют в new-выборке. Нужно их найти и удалить:
    val newImgIdsSet = mimgs2rkSet(needImgs)
    val imgIds4del = oldImgIdsSet -- newImgIdsSet     //oldImgs.filter { img => !(newImgIdsSet contains img.rowKey) }
    val delOldUnusedFut = Future.traverse(imgIds4del) { mimg =>
      val fut = MImg.deleteAllFor(mimg)
      info("updateOrigImgFullDo(): delOldUnusedImgs: deleting img " + UuidUtil.uuidToBase64(mimg))
      fut
    }

    var imgsKeepFut: Future[Seq[(MImg, Int)]] = newOldImgsMapFut.map { m =>
      m.getOrElse(true, Nil)
        // Ксакеп Вася попытается подставить id уже сохраненной где-то картинки. А потом честно запросить удаление этой картинки.
        // Нужно исключить возможность подмешивать в списки картинок левые id, используя список oldImgs:
        .filter { v =>
          val filterResult = oldImgIdsSet contains v._1.rowKey
          if (!filterResult)
            warn("Tried to keep image, that does not exists in previous imgs: " + v._1.rowKeyStr)
          else
            trace(s"Keeping old img[${v._2}]: ${v._1.rowKeyStr}")
          filterResult
        }
    }

    // Если включена принудительная ревалидация неизменившихся картинок, запускаем параллельную ревалидацию для keeped-картинок.
    if (REVALIDATE_ALREADY_SAVED_IMGS) {
      imgsKeepFut = imgsKeepFut flatMap { res =>
        Future.traverse(res) { v =>
          v._1
            .original
            .toLocalImg
            .map { localOpt =>
              localOpt
                .filter { loc =>
                  val filterResult = loc.isExists
                  if (!filterResult)
                    warn("REVALIDATE_ALREADY_SAVED: keeped image not exists: " + loc.fileName)
                  filterResult
                }
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
          val filterResult = v._1
            .toLocalInstance
            .original
            .isExists
          if (!filterResult)
            warn("Request to save totally inexisting img: " + v._1.fileName)
          else
            trace(s"Saving new img[${v._2}]: ${v._1.rowKeyStr}")
          filterResult
        }
      // Сохраняем все картинки параллельно:
      Future.traverse(imgs4s) { v =>
        v._1
          .original
          .saveToPermanent
          .map { _ => v }
      }
    }

    // Объединяем фьючерсы сохранения, восстанавливаем исходный порядок картинок:
    val resultFut = for {
      imgsKeeped <- imgsKeepFut
      imgsSaved  <- imgsSaveFut
    } yield {
      (imgsKeeped ++ imgsSaved)
        .sortBy(_._2)
        .map(_._1)
    }

    // Дожидаемся завершения удаления ненужных картинок:
    delOldUnusedFut flatMap { _ =>
      resultFut
    }
  }

  /** Из коллекции MImg-указателей сделать множество uuid картинок.
    * @param imgs Коллекция mimg или итератор.
    * @return
    */
  private def mimgs2rkSet(imgs: TraversableOnce[MAnyImgT]): Set[UUID] = {
    imgs
      .toIterator
      .map(_.rowKey)
      .toSet
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

