package util.img

import java.util.UUID

import io.suggest.common.geom.d2.ISize2di
import io.suggest.util.UuidUtil
import models.im._
import util.PlayMacroLogsImpl
import io.suggest.img.SioImageUtilT
import play.api.Play.{current, configuration}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import java.lang
import models._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.02.14 15:14
 * Description: Для работы с загружаемыми картинками используются эти вспомогательные функции.
 */

object ImgFormUtil extends PlayMacroLogsImpl {

  // TODO Реализовать это через DI к play3.
  private val mImg3 = current.injector.instanceOf( classOf[MImgs3] )


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

  /** Сборка маппингов для img-инпутов в форме. */
  def mkImgIdOptM[T1 <: MImgT](applier: IMImgCompanion { type T <: T1 }): Mapping[Option[T1]] = {
    optional( text(maxLength = IIK_MAXLEN) )
      .transform[Option[T1]](
        {txtOpt =>
          try {
            txtOpt
              .filter(_.length >= 8)
              // нельзя .map(applier.apply), ибо error: method with dependent type (fileName: String)applier.T cannot be converted to function value
              .map(applier(_))
          } catch {
            case ex: Exception =>
              debug("imgIdOptM.apply: Cannot parse img id key: " + txtOpt, ex)
              None
          }
        },
        { _.map(_.fileName) }
      )
  }

  /** Маппер для новых картинок на базе MMedia. */
  def img3IdOptM = mkImgIdOptM[MImgT](mImg3)

  /** Обязательный маппинг MImg3-картинки. */
  def img3IdM = img3IdOptM
    .verifying("error.required", _.isDefined)
    .transform [MImgT] (_.get, Some.apply)


  // Валидация значений crop'а.
  /** Минимальный размер откропанной стороны. */
  private val CROP_SIDE_MIN_PX = configuration.getInt("img.crop.side.max") getOrElse 10
  /** Максимальный размер откропанной стороны. */
  private val CROP_SIDE_MAX_PX = configuration.getInt("img.crop.side.max") getOrElse 20000
  /** Максимальный размер сдвига относительно левого верхнего узла. */
  private val CROP_OFFSET_MAX_PX = configuration.getInt("img.crop.offset.max") getOrElse 60000

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

  def updateOrigImgId(needImgs: Seq[MImgT], oldImgIds: Iterable[String]): Future[Seq[MImgT]] = {
    updateOrigImgFull(needImgs, oldImgIds.map(mImg3(_)))
  }

  /**
   * Замена иллюстраций к некоей сущности.
   * @param needImgs Необходимые в результате набор картинок. Тут могут быть как уже сохранённыя картинка,
   *                 так и новая из tmp. Если Nil, то старые картинки будут удалены.
   * @param oldImgs Уже сохранённые ранее картинки, если есть.
   * @return Список id новых и уже сохранённых картинок.
   */
  def updateOrigImgFull(needImgs: Seq[MImgT], oldImgs: Iterable[MImgT]): Future[Seq[MImgT]] = {
    // Защита от какой-либо деятельности в случае полного отсутствия входных данных.
    if (needImgs.isEmpty && oldImgs.isEmpty) {
      Future successful Nil
    } else {
      updateOrigImgFullDo(needImgs, oldImgs = oldImgs)
    }
  }
  private def updateOrigImgFullDo(needImgs: Seq[MImgT], oldImgs: Iterable[MImgT]): Future[Seq[MImgT]] = {
    val needImgsIndexed = needImgs.zipWithIndex

    // Разделяем на картинки, которые уже были, и которые затребованы для отправки в хранилище:
    val newOldImgsMapFut = {
      Future.traverse(needImgsIndexed) { case a @ (img, i) =>
        img.original
          .existsInPermanent
          .map { existsInPermanent =>
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

    // Отреагировать на картинки, которые больше не нужны.
    if (imgIds4del.nonEmpty) {
      // Теперь только логгирование, потому что нужно, чтобы N2 GC собрал их.
      LOGGER.debug("Possibly-unused imgs: " +
        imgIds4del.map(UuidUtil.uuidToBase64).mkString(", ") )
    }

    var imgsKeepFut: Future[Seq[(MImgT, Int)]] = newOldImgsMapFut.map { m =>
      m.getOrElse(true, Nil)
        // Ксакеп Вася попытается подставить id уже сохраненной где-то картинки. А потом честно запросить удаление этой картинки.
        // Нужно исключить возможность подмешивать в списки картинок левые id, используя список oldImgs:
        .filter { v =>
          val filterResult = oldImgIdsSet.contains( v._1.rowKey )
          if (!filterResult)
            warn("Tried to keep image, that does not exists in previous imgs: " + v._1.rowKeyStr)
          else
            trace(s"Keeping old img[${v._2}]: ${v._1.rowKeyStr}")
          filterResult
        }
    }

    // Если включена принудительная ревалидация неизменившихся картинок, запускаем параллельную ревалидацию для keeped-картинок.
    if (REVALIDATE_ALREADY_SAVED_IMGS) {
      imgsKeepFut = for {
        res  <- imgsKeepFut
        futs <- Future.traverse(res) { v =>
          for {
            localOpt <- mImg3.toLocalImg( v._1.original )
          } yield {
            localOpt
              .filter { loc =>
                val filterResult = loc.isExists
                if (!filterResult)
                  warn("REVALIDATE_ALREADY_SAVED: keeped image not exists: " + loc.fileName)
                filterResult
              }
              .map(_ => v)
          }
        }
      } yield {
        futs.flatten
      }
    }

    // TODO при изменении настроек кропа надо удалять все картинки из хранилищ кроме оригинала.

    // Новые загруженные картинки - копируем в постоянное хранилище:
    val imgsSaveFut: Future[Seq[(MImgT, Int)]] = newOldImgsMapFut.flatMap { m =>
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
    resultFut
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

  def img2imgInfo(mimg: MImgT): Future[MImgInfo] = {
    for {
      wh <- mImg3.getImageWH(mimg)
    } yield {
      MImgInfo(mimg.fileName, wh.map(MImgInfoMeta.apply))
    }
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
  def repairCrop(crop: ImgCrop, targetSz: ISize2di, srcSz: ISize2di): ImgCrop = {
    var newCrop = crop
    // Пофиксить offset'ы кропа. Кроп может по сдвигу уезжать за пределы допустимой ширины/длины.
    if (crop.offX + crop.width > srcSz.width)
      newCrop = crop.copy(offX = srcSz.width - crop.width)
    if (crop.offY + crop.height > srcSz.height)
      newCrop = crop.copy(offY = srcSz.height - crop.height)
    newCrop
  }


  def imgMeta2md(sz: ISize2di): Map[String, String] = {
    Map(
      IMETA_WIDTH  -> sz.width.toString,
      IMETA_HEIGHT -> sz.height.toString
    )
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

