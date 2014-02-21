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

  /** Максимальный размер сторон будущей превьюшки. */
  def THUMBNAIL_SIZE_PX: Integer

  /**  Качество сжатия jpeg. */
  def THUMBNAIL_QUALITY_PC: Double

  /** Если исходный jpeg после стрипа больше этого размера, то сделать resize. */
  def MAX_SOURCE_JPEG_BYTES: Long

  /** Картинка считается слишком маленькой для обработки, если хотя бы одна сторона не превышает этот порог. */
  def MIN_SZ_PX: Int

  /** Если любая из сторон картинки превышает этот лимит, то convert с уменьшением размера без вариантов. */
  def SRC_SZ_ALWAYS_DOWNSIZE: Int

  /** Если на выходе получилась слишком жирная превьюшка, то отсеять её. */
  def MAX_OUT_FILE_SIZE_BYTES: Int


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
  def prepareImageFetchedSync(fetchedFile: File, toFile:File) {
    // Отресайзить картинку во временный файл
    if (isImage(fetchedFile)) {
      // Тут блокировка на время конвертации.
      prepareImage(fetchedFile.getAbsolutePath, toFile)
    } else {
      throw new UnsupportedOperationException(fetchedFile.getAbsolutePath + " is not a picture. Aborting")
    }
  }


  /** Прочитать готовую картинку в память, если её размер нормальный. */
  def maybeReadThumbFromFile(resultFile: File): Array[Byte] = {
    // Картинка подготовлена. Нужно её прочитать и сгенерить результат.
    val buffSz = resultFile.length().toInt
    // Если результат слишком большой, то не читать.
    if (buffSz > MAX_OUT_FILE_SIZE_BYTES) {
      throw new IllegalArgumentException(s"Preview file '$resultFile' too large: $buffSz ;; max size = $MAX_OUT_FILE_SIZE_BYTES")
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
   * @param filenameOld Файл с картинкой.
   * @return
   */
  def prepareImage(filenameOld:String, fileNew:File) {
    lazy val logPrefix = s"prepareImage($filenameOld): "
    LOGGER.trace(logPrefix + "to file " + fileNew.getAbsolutePath)
    // Получаем инфу о текущей картинке
    val imageInfo = new Info(filenameOld, true)
    // Подготавливаем новый файлец
    val filenameNew = fileNew.getAbsolutePath
    // Ресайзим в зависимости от исходного размера и формата.
    // Выбрать методику переколбашивания картинки в зависимости от размеров сторон, формата и размера файла.
    if (imageInfo.getImageHeight > SRC_SZ_ALWAYS_DOWNSIZE || imageInfo.getImageWidth > SRC_SZ_ALWAYS_DOWNSIZE) {
      // Картинка большая - только ресайзить
      LOGGER.trace(logPrefix + "is a big picture. Convert/resize.")
      convert(filenameOld, filenameNew, ConvertModes.RESIZE)

    } else if (imageInfo.getImageHeight <= MIN_SZ_PX || imageInfo.getImageWidth <= MIN_SZ_PX) {
      // Слишком маленькая картинка, чтобы её куда-то ресайзить.
      throw new PictureTooSmallException(filenameOld)

    } else if (imageInfo.getImageFormat equalsIgnoreCase "JPEG") {
      // Изображение умещается в желаемые размеры и это JPEG. Стрипануть и проверить размер.
      LOGGER.trace(logPrefix + "Not big image and JPEG. Let's try jpegtran.")
      val jtResult = jpegtran(filenameOld, filenameNew)
      if (!jtResult || fileNew.length() > MAX_SOURCE_JPEG_BYTES) {
        LOGGER.trace(logPrefix + s"jpegtran produced too fat file (newSz=${fileNew.length}) or success=$jtResult. Convert/strip.")
        convert(filenameOld, filenameNew, ConvertModes.STRIP)
      }

    } else {
      // Всякие PNG и т.д. -- конверить в JPEG без ресайза.
      LOGGER.trace(logPrefix + s"Small picture, but is a ${imageInfo.getImageFormat}, not JPEG. Convert/strip.")
      convert(filenameOld, filenameNew, ConvertModes.STRIP)
    }
  }


  /**
   * Стрипануть jpeg-файл на тему метаданных и профиля.
   * @param filenameOld Файл с исходной картинкой.
   * @param filenameNew Файл, в который нужно записать обработанную картинку.
   */
  def jpegtran(filenameOld:String, filenameNew:String) : Boolean = {
    val cmd = Array("jpegtran", "-copy", "none", "-outfile", filenameNew, filenameOld)
    val p = Runtime.getRuntime.exec(cmd)
    val result = p.waitFor()
    LOGGER.trace(cmd.mkString(" ") + " ==> " + result)
    result == 0
  }


  /**
   * Конвертировать с помощью ImageMagick. Есть режимы strip или thumbnail.
   * @param filenameOld Файл с исходной картинкой.
   * @param filenameNew Файл, в который нужно записать обработанную картинку.
   * @param mode Режим работы конвертера.
   */
  def convert(filenameOld:String, filenameNew:String, mode:ConvertMode) {
    val cmd = new ConvertCmd
    val op = new IMOperation()
    // TODO Нужно брать рандомный кадр из gif вместо нулевого, который может быть пустым.
    op.addImage(filenameOld + "[0]")   // (#117) Без указания кадра, будет ошибка и куча неудаленных файлов в /tmp.
    // Почему-то match не работает, поэтому тут if-else
    if (mode == ConvertModes.STRIP) {
      op.strip()
    } else if (mode == ConvertModes.RESIZE) {
      op.thumbnail(THUMBNAIL_SIZE_PX, THUMBNAIL_SIZE_PX)
    } else {
      throw new NotImplementedError("mode = " + mode.toString)
    }

    op.quality(THUMBNAIL_QUALITY_PC)
    op.samplingFactor(2.0, 1.0)
    op.addImage(filenameNew)
    cmd.run(op)
  }

}

case class PictureTooSmallException(filePath: String) extends Exception("Picture too small: " + filePath)


object ConvertModes extends Enumeration {
  type ConvertMode = Value
  val STRIP, RESIZE = Value
}

