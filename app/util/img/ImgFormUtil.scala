package util.img

import util.PlayMacroLogsImpl
import models.MPictureTmp
import io.suggest.img.{ConvertModes, ImgCrop, SioImageUtilT}
import play.api.Play.current
import io.suggest.model.{MImgThumb, MUserImgOrig, MPict}
import scala.concurrent.{Future, future}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import java.io.{File, FileNotFoundException}
import org.apache.commons.io.FileUtils
import scala.util.{Failure, Success}
import java.lang

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.02.14 15:14
 * Description: Для работы с загружаемыми картинками используются эти вспомогательные функции.
 */

object ImgFormUtil extends PlayMacroLogsImpl {
  import play.api.data.Forms._
  import LOGGER._

  /** Маппер для поля с id картинки. Используется обертка над id чтобы прозрачно различать tmp и orig картинки. */
  val imgIdM  = nonEmptyText(minLength = 8, maxLength = 42)
    .transform(ImgIdKey.apply, {iik: ImgIdKey => iik.key})
    .verifying("img.id.invalid.", { _.isValid })

  /** Маппинг обязательного параметра кропа на реальность. */
  val imgCropM = nonEmptyText(minLength = 4, maxLength = 16)
    .verifying("crop.invalid", ImgCrop.isValidCropStr(_))
    .transform(ImgCrop.apply, {ic: ImgCrop => ic.toCropStr})

  val imgCropOptM = optional(imgCropM)


  /** Нередко бывает несколько картинок при сабмите. */
  def mergeListMappings(iiks: List[ImgIdKey], iCrops: List[ImgCrop]): List[ImgInfo[ImgIdKey]] = {
    iiks.zip(iCrops) map {
      case (iik, crop) => ImgInfo(iik, Some(crop))
    }
  }

  def updateOrigImgIds(needImgs: Seq[ImgInfo[ImgIdKey]], oldImgIds: Seq[String]): Future[Seq[String]] = {
    updateOrigImg(needImgs, oldImgIds.map(OrigImgIdKey.apply))
  }
  
  /**
   * Замена иллюстрации к некоей сущности.
   * @param needImgs Необходимые в результате набор картинок. Тут могут быть как уже сохранённыя картинка,
   *                 так и новая из tmp. Если Nil, то старые картинки будут удалены.
   * @param oldImgs Уже сохранённые ранее картинки, если есть.
   * @return Список id новых и уже сохранённых картинок TODO в исходном порядке.
   */
  def updateOrigImg(needImgs: Seq[ImgInfo[ImgIdKey]], oldImgs: Seq[OrigImgIdKey]): Future[Seq[String]] = {
    val oldImgsSet = oldImgs.toSet
    val newTmpImgs = needImgs.iterator
      .filter { _.iik.isInstanceOf[TmpImgIdKey] }
      .map { _.asInstanceOf[ImgInfo[TmpImgIdKey]] }
      .toList
    val needOrigImgs = needImgs.iterator
      .filter { _.iik.isInstanceOf[OrigImgIdKey] }
      .map { _.asInstanceOf[ImgInfo[OrigImgIdKey]] }
      .filter { oii => oldImgsSet contains oii.iik }  // Отбросить orig-картинки, которых не было среди старых оригиналов.
      .toList
    val delOldImgs = oldImgsSet -- needOrigImgs.map(_.iik)
    // Запускаем в фоне удаление старых картинок. TODO Возможно, надо этот фьючерс подвязывать к фьючерсу сохранения?
    Future.traverse(delOldImgs) { oldOiik =>
      val fut = MPict.deleteFully(oldOiik.key)
      fut onComplete {
        case Success(_)  => trace("Old img deleted: " + oldOiik)
        case Failure(ex) => error("Failed to delete old img " + oldOiik, ex)
      }
      fut
    }
    Future.traverse(newTmpImgs) { tii =>
      val fut = handleTmpImageForStoring(tii)
      fut onComplete { case tryResult =>
        tii.iik.getFile.foreach(_.file.delete())
      }
      fut onFailure {
        case ex =>  error(s"Failed to store picture " + tii, ex)
      }
      fut recover {
        case ex: Exception => null
      }
    } map { savedTmpImgsOrNull =>
      val newSavedIds = savedTmpImgsOrNull
        .filter { _ != null }
        .map { _.idStr }
      val preservedIds = needOrigImgs.map { _.iik.key }
      // TODO Нужно восстанавливать исходный порядок! Сейчас пока плевать на это, но надо это как-то исправлять.
      newSavedIds ++ preservedIds
    }
  }

  /**
   * В контроллер приходит сабмит по картинкам. Для tmp-картинок надо отправить их целиком в orig-хранилище.
   * Для любых картинок надо обновить нарезку, применив crop и отправив в хранилище.
   * @param tii Исходная tmp-картинка.
   * @return Фьючерс, содержащий imgId в виде строки.
   */
  def handleTmpImageForStoring(tii: ImgInfo[TmpImgIdKey]): Future[SavedTmpImg] = {
    tii.iik.getFile match {
      case Some(mptmp) =>
        val id = MPict.randomId
        // Запустить чтение из уже отрезайзенного tmp-файла и сохранение как-бы-исходного материала в HBase
        val saveOrigFut = future {
          OrigImageUtil.maybeReadFromFile(mptmp.file)
        } flatMap { imgBytes =>
          val idStr = MPict.idBin2Str(id)
          MUserImgOrig(idStr, imgBytes).save.map { _ =>
            SavedTmpImg(idStr, mptmp.file)
          }
        }
        // Пора сгенерить thumbnail без учёта кропов всяких.
        val saveThumbFut = future {
          val tmpThumbFile = File.createTempFile("origThumb", ".jpeg")
          val thumbBytes = try {
            ThumbImageUtil.convert(mptmp.file, tmpThumbFile, mode = ConvertModes.THUMB, crop = tii.cropOpt)
            // Прочитать файл в памяти и сохранить в MImgThumb
            FileUtils.readFileToByteArray(tmpThumbFile)
          } finally {
            tmpThumbFile.delete()
          }
          val thumbDatum = new MImgThumb(id=id, thumb=thumbBytes, imageUrl=null)
          thumbDatum.save
        }
        // Связываем оба фьючерса
        saveThumbFut flatMap { _ => saveOrigFut }

      case None =>
        Future failed new FileNotFoundException(tii.iik.key)
    }
  }


  /**
   * Асинхронное удаление отработанных времененных файлов.
   * @param imgs Поюзанные картинки.
   * @return Фьючерс для синхронизации.
   */
  def rmTmpImgs(imgs: Seq[SavedTmpImg]): Future[_] = {
    future {
      imgs.foreach { img =>
        try {
          img.tmpImgFile.delete()
        } catch {
          case ex: FileNotFoundException =>
            trace("File already deleted: " + img.tmpImgFile.getAbsolutePath)
          case ex: Throwable => error("Failed to delete file " + img.tmpImgFile.getAbsolutePath, ex)
        }
      }
    }
  }

}


/** Резайзилка картинок, используемая для генерация "оригиналов", т.е. картинок, которые затем будут кадрироваться. */
object OrigImageUtil extends SioImageUtilT with PlayMacroLogsImpl {
  /** Если на выходе получилась слишком жирная превьюшка, то отсеять её. */
  override def MAX_OUT_FILE_SIZE_BYTES: Option[Int] = None

  /** Картинка считается слишком маленькой для обработки, если хотя бы одна сторона не превышает этот порог. */
  override val MIN_SZ_PX: Int = current.configuration.getInt("img.orig.sz.min.px") getOrElse 256

  /** Если исходный jpeg после стрипа больше этого размера, то сделать resize.
    * Иначе попытаться стрипануть icc-профиль по jpegtran, чтобы снизить размер без пересжатия. */
  override def MAX_SOURCE_JPEG_NORSZ_BYTES: Option[Long] = None

  /** Качество сжатия jpeg. */
  override val JPEG_QUALITY_PC: Double = current.configuration.getDouble("img.orig.jpeg.quality") getOrElse 90.0

  /** Максимальный размер сторон будущей картинки (новая картинка должна вписываться в
    * прямоугольник с указанныыми сторонами). */
  override val DOWNSIZE_HORIZ_PX: Integer  = Integer valueOf (current.configuration.getInt("img.orig.maxsize.h.px") getOrElse 2048)
  override val DOWNSIZE_VERT_PX:  Integer  = current.configuration.getInt("img.orig.maxsize.v.px").map(Integer.valueOf) getOrElse DOWNSIZE_HORIZ_PX

  override val GAUSSIAN_BLUG: Option[lang.Double] = None
}


object ThumbImageUtil extends SioImageUtilT with PlayMacroLogsImpl {
  /** Максимальный размер сторон будущей картинки (новая картинка должна вписываться в
    * прямоугольник с указанныыми сторонами). */
  val DOWNSIZE_HORIZ_PX: Integer = Integer valueOf (current.configuration.getInt("img.thumb.maxsize.h.px") getOrElse 256)
  val DOWNSIZE_VERT_PX : Integer = current.configuration.getInt("img.thumb.maxsize.h.px").map(Integer.valueOf) getOrElse DOWNSIZE_HORIZ_PX

  /** Если на выходе получилась слишком жирная превьюшка, то отсеять её. */
  override def MAX_OUT_FILE_SIZE_BYTES: Option[Int] = None

  /** Картинка считается слишком маленькой для обработки, если хотя бы одна сторона не превышает этот порог. */
  override def MIN_SZ_PX: Int = DOWNSIZE_HORIZ_PX.intValue()

  /** Если исходный jpeg после стрипа больше этого размера, то сделать resize.
    * Иначе попытаться стрипануть icc-профиль по jpegtran, чтобы снизить размер без пересжатия. */
  override def MAX_SOURCE_JPEG_NORSZ_BYTES: Option[Long] = None

  /** Качество сжатия jpeg. */
  override val JPEG_QUALITY_PC: Double = current.configuration.getDouble("img.thumb.jpeg.quality") getOrElse 85.0
}


object ImgIdKey {
  def apply(key: String): ImgIdKey = {
    if (key startsWith MPictureTmp.KEY_PREFIX) {
      TmpImgIdKey(key)
    } else {
      OrigImgIdKey(key)
    }
  }
}

sealed trait ImgIdKey {
  def key: String
  def isExists: Future[Boolean]
  def isValid: Boolean
  def isTmp: Boolean
  override def hashCode = key.hashCode
}

case class TmpImgIdKey(key: String) extends ImgIdKey {

  def isTmp: Boolean = true

  def isExists: Future[Boolean] = {
    Future successful MPictureTmp.isExist(key)
  }

  def isValid: Boolean = {
    MPictureTmp.isKeyValid(key)
  }
  
  def getFile = MPictureTmp.find(key)
}

case class OrigImgIdKey(key: String) extends ImgIdKey {

  def isTmp: Boolean = false

  def isExists: Future[Boolean] = {
    MUserImgOrig.getById(key).map(_.isDefined)
  }

  def isValid: Boolean = {
    MPict.isStrIdValid(key)
  }
}


/** Класс для объединения кропа и id картинки (чтобы не использовать Tuple2 с числовыми названиями полей) */
case class ImgInfo[+T <: ImgIdKey](iik: T, cropOpt: Option[ImgCrop])


case class SavedTmpImg(idStr:String, tmpImgFile:File)
