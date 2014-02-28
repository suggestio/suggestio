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

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.02.14 15:14
 * Description: Для работы с загружаемыми картинками используются эти вспомогательные функции.
 */
object ImgFormUtil extends PlayMacroLogsImpl {
  import play.api.data.Forms._
  import LOGGER._

  /** Маппер для поля с id временной картинки. Используется обертка над id чтобы различать tmp и orig картинки. */
  val tmpImgIdM  = nonEmptyText(minLength=5, maxLength=64)
    .transform(ImgIdKey(_), {iik: ImgIdKey => iik.key})
    .verifying("img.id.invalid.", { _.isValid })


  /** Маппинг обязательного параметра кропа на реальность. */
  val imgCropM = nonEmptyText(maxLength = 16)
    .verifying("crop.invalid", ImgCrop.isValidCropStr(_))
    .transform(ImgCrop(_), {ic: ImgCrop => ic.toCropStr})

  val imgCropOptM = optional(imgCropM)


  /** Нередко бывает несколько картинок при сабмите. */
  def mergeListMappings(iiks: List[ImgIdKey], iCrops: List[ImgCrop]): List[ImgInfo] = {
    iiks.zip(iCrops) map {
      case (iik, crop) => ImgInfo(iik, crop)
    }
  }


  /**
   * В контроллер приходит сабмит по картинкам. Для tmp-картинок надо отправить их целиком в orig-хранилище.
   * Для любых картинок надо обновить нарезку, применив crop и отправив в хранилище.
   * @param img Исходная картинка.
   * @return Фьючерс, содержащий imgId в виде строки.
   */
  def handleImageForStoring(img: ImgInfo): Future[TmpImgReadyForCrop] = {
    img.iik match {
      // Сначала прочитать картинку и отправить в MUserImgOrig
      case tik: TmpImgIdKey =>
        MPictureTmp.find(tik.key) match {
          case Some(mptmp) =>
            val id = MPict.randomId
            // Запустить чтение из файла и сохранение в HBase
            val saveOrigFut = future {
              OrigImageUtil.maybeReadFromFile(mptmp.file)
            } flatMap { imgBytes =>
              val idStr = new String(id)
              MUserImgOrig(idStr, imgBytes).save.map { _ =>
                TmpImgReadyForCrop(idStr, mptmp.file)
              }
            }
            // Пора сгенерить thumbnail без учёта кропов всяких.
            val saveThumbFut = future {
              val tmpThumbFile = File.createTempFile("origThumb", ".jpeg")
              val thumbBytes = try {
                ThumbImageUtil.convert(mptmp.file, tmpThumbFile, mode = ConvertModes.THUMB)
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
            Future failed new FileNotFoundException(tik.key)
        }

      // Постоянная картинка. Нужно выкачать оригинал из БД в /tmp и поработать с ним.
      case oik: OrigImgIdKey =>
        MUserImgOrig.getById(oik.key) map {
          case Some(iwts) =>
            // прочитали ок. отправляем в файл, чтобы convert могла произвести нарезку по исходному кропу в разных размерах
            val tmpFile = File.createTempFile("storedOrig", ".jpeg")
            FileUtils.writeByteArrayToFile(tmpFile, iwts.img)
            TmpImgReadyForCrop(oik.key, tmpFile)

          case None =>
            throw new FileNotFoundException(oik.key + " not found in storage")
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
  override val JPEG_QUALITY_PC: Double = current.configuration.getDouble("img.orig.jpeg.quality") getOrElse 92.0

  /** Максимальный размер сторон будущей картинки (новая картинка должна вписываться в
    * прямоугольник с указанныыми сторонами). */
  override val DOWNSIZE_HORIZ_PX: Integer  = Integer valueOf (current.configuration.getInt("img.orig.maxsize.h.px") getOrElse 2048)
  override val DOWNSIZE_VERT_PX:  Integer  = current.configuration.getInt("img.orig.maxsize.v.px").map(Integer.valueOf) getOrElse DOWNSIZE_HORIZ_PX
}


object ThumbImageUtil extends SioImageUtilT with PlayMacroLogsImpl {
  /** Максимальный размер сторон будущей картинки (новая картинка должна вписываться в
    * прямоугольник с указанныыми сторонами). */
  val DOWNSIZE_HORIZ_PX: Integer = Integer valueOf (current.configuration.getInt("img.thumb.maxsize.h.px") getOrElse 256)
  val DOWNSIZE_VERT_PX : Integer = current.configuration.getInt("img.thumb.maxsize.h.px").map(Integer.valueOf) getOrElse DOWNSIZE_HORIZ_PX

  /** Если на выходе получилась слишком жирная превьюшка, то отсеять её. */
  override def MAX_OUT_FILE_SIZE_BYTES: Option[Int] = None

  /** Картинка считается слишком маленькой для обработки, если хотя бы одна сторона не превышает этот порог. */
  override def MIN_SZ_PX: Int = DOWNSIZE_HORIZ_PX

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
}

case class TmpImgIdKey(key: String) extends ImgIdKey {
  override def isExists: Future[Boolean] = {
    Future successful MPictureTmp.isExist(key)
  }

  override def isValid: Boolean = {
    MPictureTmp.isKeyValid(key)
  }
}

case class OrigImgIdKey(key: String) extends ImgIdKey {
  override def isExists: Future[Boolean] = {
    MUserImgOrig.getById(key).map(_.isDefined)
  }

  override def isValid: Boolean = {
    MPict.isStrIdValid(key)
  }
}


/** Класс для объединения кропа и id картинки (чтобы не использовать Tuple2 с числовыми названиями полей) */
case class ImgInfo(iik: ImgIdKey, crop: ImgCrop)


case class TmpImgReadyForCrop(idStr:String, imgFile:File)
