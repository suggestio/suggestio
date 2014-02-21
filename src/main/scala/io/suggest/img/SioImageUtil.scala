package io.suggest.img

import org.im4java.core.{Info, ConvertCmd, IMOperation}
import java.io.{FileInputStream, File}
import java.nio.file.Files
import com.typesafe.scalalogging.slf4j.Logger
import io.suggest.img.ConvertModes.ConvertMode

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.03.13 17:39
 * Description: Функции работы с картинками: фетч, обработка и т.д.
 * Есть ряд настроек для манипуляции с картинками, поэтому тут трейт вместо object. На стороне конкретных проектов
 * уже можно делать конкретную реализацию (обычно через object).
 */

trait SioImageUtilT {

  protected def LOGGER: Logger

  /** Максимальный размер сторон будущей картинки (новая картинка должна вписываться в
    * прямоугольник с указанныыми сторонами). */
  def DOWNSIZE_VERT_PX: Integer
  def DOWNSIZE_HORIZ_PX: Integer

  /** Качество сжатия jpeg. */
  def JPEG_QUALITY_PC: Double

  /** Если исходный jpeg после стрипа больше этого размера, то сделать resize.
    * Иначе попытаться стрипануть icc-профиль по jpegtran, чтобы снизить размер без пересжатия. */
  def MAX_SOURCE_JPEG_NORSZ_BYTES: Option[Long]

  /** Картинка считается слишком маленькой для обработки, если хотя бы одна сторона не превышает этот порог. */
  def MIN_SZ_PX: Int

  /** Если любая из сторон картинки превышает этот лимит, то convert с уменьшением размера без вариантов. */
  def SRC_SZ_SMALL_HORIZ_PX: Int = DOWNSIZE_HORIZ_PX.intValue()
  def SRC_SZ_SMALL_VERT_PX: Int  = DOWNSIZE_VERT_PX.intValue()

  /** Если на выходе получилась слишком жирная превьюшка, то отсеять её. */
  def MAX_OUT_FILE_SIZE_BYTES: Option[Int]


  /**
   * Является ли файл картинкой?
   * @param file файл
   * @return true | false
   */
  def isImage(file: File) : Boolean = {
    try {
      val result = Files.probeContentType(file.toPath)
      LOGGER.trace(s"isImage(${file.getAbsolutePath}): Content-Type: $result")
      result.startsWith("image/")
    } catch {
      case e:Throwable =>
        LOGGER.error("Cannot probe mime for file " + file.getAbsolutePath, e)
        false
    }
  }


  /** Блокирующе запилить картинку. */
  def checkPrepareThumb(fetchedFile: File, toFile:File) {
    // Отресайзить картинку во временный файл
    if (isImage(fetchedFile)) {
      // Тут блокировка на время конвертации.
      prepareThumb(fetchedFile, toFile)
    } else {
      throw new UnsupportedOperationException(fetchedFile.getAbsolutePath + " is not a picture. Aborting")
    }
  }


  /** Прочитать готовую картинку в память, если её размер нормальный. */
  def maybeReadFromFile(resultFile: File): Array[Byte] = {
    // Картинка подготовлена. Нужно её прочитать и сгенерить результат.
    val buffSz = resultFile.length().toInt
    // Если результат слишком большой, то не читать.
    if (MAX_OUT_FILE_SIZE_BYTES.isDefined && buffSz > MAX_OUT_FILE_SIZE_BYTES.get) {
      throw new IllegalArgumentException(s"Preview file '$resultFile' too large: $buffSz ;; max size = ${MAX_OUT_FILE_SIZE_BYTES.get}")
    }
    val is = new FileInputStream(resultFile)
    try {
      val buf = new Array[Byte](buffSz.toInt)
      is.read(buf)
      buf
    } finally {
      is.close()
    }
  }


  /**
   * Отресайзить картинку в превьюшку. Переписано с sio_html_an_img_selector:prepare_image/1
   * @param fileOld Файл с картинкой.
   * @return
   */
  def prepareThumb(fileOld:File, fileNew:File) {
    lazy val logPrefix = s"prepareImage($fileOld): "
    LOGGER.trace(logPrefix + "to file " + fileNew.getAbsolutePath)
    // Получаем инфу о текущей картинке
    val imageInfo = new Info(fileOld.getAbsolutePath, true)
    // Подготавливаем новый файлец
    // Ресайзим в зависимости от исходного размера и формата.
    // Выбрать методику переколбашивания картинки в зависимости от размеров сторон, формата и размера файла.
    if (imageInfo.getImageHeight > SRC_SZ_SMALL_VERT_PX || imageInfo.getImageWidth > SRC_SZ_SMALL_HORIZ_PX) {
      // Картинка большая - только ресайзить
      LOGGER.trace(logPrefix + "is a big picture. Convert/resize.")
      convert(fileOld, fileNew, ConvertModes.THUMB)

    } else if (imageInfo.getImageHeight <= MIN_SZ_PX || imageInfo.getImageWidth <= MIN_SZ_PX) {
      // Слишком маленькая картинка, чтобы её куда-то ресайзить.
      throw new PictureTooSmallException(fileOld.getAbsolutePath)

    } else if (MAX_SOURCE_JPEG_NORSZ_BYTES.isDefined && imageInfo.getImageFormat.equalsIgnoreCase("JPEG")) {
      // Изображение умещается в желаемые размеры и это JPEG. Стрипануть и проверить размер.
      LOGGER.trace(logPrefix + "Not big image and JPEG. Let's try jpegtran.")
      val jtResult = jpegtran(fileOld, fileNew)
      if (!jtResult || fileNew.length() > MAX_SOURCE_JPEG_NORSZ_BYTES.get) {
        LOGGER.trace(logPrefix + s"jpegtran produced too fat file (newSz=${fileNew.length}) or success=$jtResult. Convert/strip.")
        convert(fileOld, fileNew, ConvertModes.STRIP)
      }

    } else {
      // Всякие PNG и т.д. -- конверить в JPEG без ресайза.
      LOGGER.trace(logPrefix + s"Small picture, but is a ${imageInfo.getImageFormat}, not JPEG. Convert/strip.")
      convert(fileOld, fileNew, ConvertModes.STRIP)
    }
  }


  /**
   * Стрипануть jpeg-файл на тему метаданных и профиля.
   * @param fileOld Файл с исходной картинкой.
   * @param fileNew Файл, в который нужно записать обработанную картинку.
   */
  def jpegtran(fileOld:File, fileNew:File) : Boolean = {
    val cmd = Array("jpegtran", "-copy", "none", "-outfile", fileNew.getAbsolutePath, fileOld.getAbsolutePath)
    val p = Runtime.getRuntime.exec(cmd)
    val result = p.waitFor()
    LOGGER.trace(cmd.mkString(" ") + " ==> " + result)
    result == 0
  }


  /**
   * Конвертировать с помощью ImageMagick. Есть режимы strip или thumbnail.
   * @param fileOld Файл с исходной картинкой.
   * @param fileNew Файл, в который нужно записать обработанную картинку.
   * @param mode Режим работы конвертера. По умолчанию - RESIZE.
   * @param crop Опциональный кроп картинки.
   */
  def convert(fileOld:File, fileNew:File, mode:ConvertMode = ConvertModes.RESIZE, crop: Option[PicCrop] = None) {
    val cmd = new ConvertCmd
    val op = new IMOperation()
    // TODO Нужно брать рандомный кадр из gif вместо нулевого, который может быть пустым.
    op.addImage(fileOld.getAbsolutePath + "[0]")   // (#117) Без указания кадра, будет ошибка и куча неудаленных файлов в /tmp.
    // Кроп, задаваемый юзером: портирован из альтерраши.
    if (crop.isDefined) {
      val c = crop.get
      op.crop(c.w, c.h, c.offX, c.offY)
    }
    mode match {
      case ConvertModes.STRIP  => op.strip()
      case ConvertModes.THUMB  => op.thumbnail(DOWNSIZE_HORIZ_PX, DOWNSIZE_VERT_PX)
      case ConvertModes.RESIZE => op.resize(DOWNSIZE_HORIZ_PX, DOWNSIZE_VERT_PX)
    }
    op.quality(JPEG_QUALITY_PC)
    op.samplingFactor(2.0, 1.0)
    op.addImage(fileNew.getAbsolutePath)
    cmd.run(op)
  }

}

case class PictureTooSmallException(filePath: String) extends Exception("Picture too small: " + filePath)


object ConvertModes extends Enumeration {
  type ConvertMode = Value
  val STRIP, THUMB, RESIZE = Value
}


object PicCrop {

  val CROP_MATCHER = "^(\\d+)x(\\d+)([+-]\\d+)([+-]\\d+)$".r

  def apply(cropStr: String) : PicCrop = {
    val CROP_MATCHER(w, h, offX, offY) = cropStr
    new PicCrop(w = w.toInt,  h = h.toInt,  offX = offX.toInt,  offY = offY.toInt)
  }

  private def optSign(v: Int) : String = {
    if (v < 0)
      v.toString
    else
      "+" + v
  }
}

import PicCrop._

case class PicCrop(w:Int, h:Int, offX:Int, offY:Int) {

  /**
   * Сконвертить в строку cropStr.
   * @return строку, пригодную для возврата в шаблоны/формы
   */
  def toCropStr: String = {
    w.toString + "x" + h + optSign(offX) + optSign(offY)
  }

}

