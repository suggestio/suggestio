package io.suggest.img

import io.suggest.ym.model.common.MImgSizeT
import org.im4java.core.{Info, ConvertCmd, IMOperation}
import java.io.{FileInputStream, File}
import java.nio.file.Files
import com.typesafe.scalalogging.slf4j.Logger
import io.suggest.img.ConvertModes.ConvertMode
import scala.util.parsing.combinator.JavaTokenParsers
import scala.collection.JavaConversions._

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

  /** Размывка для сокрытия артифактов. */
  def GAUSSIAN_BLUG: Option[java.lang.Double] = None

  /** Некое цветовое переплетение. Позволяет делать progressive jpeg.
    * @see [[http://www.imagemagick.org/script/command-line-options.php#interlace]] */
  def INTERLACING: Option[String] = Some("Plane")

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


  def identify(file: File) = new Info(file.getAbsolutePath, true)


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
  def convert(fileOld:File, fileNew:File, mode:ConvertMode = ConvertModes.RESIZE, crop: Option[ImgCrop] = None) {
    val cmd = new ConvertCmd
    val op = new IMOperation()
    // TODO Нужно брать рандомный кадр из gif вместо нулевого, который может быть пустым.
    op.addImage(fileOld.getAbsolutePath + "[0]")   // (#117) Без указания кадра, будет ошибка и куча неудаленных файлов в /tmp.
    if (INTERLACING.isDefined) {
      op.interlace(INTERLACING.get)
    }
    // Кроп, задаваемый юзером: портирован из альтерраши.
    if (crop.isDefined) {
      val c = crop.get
      op.crop(c.width, c.height, c.offX, c.offY)
    }
    mode match {
      case ConvertModes.STRIP  => op.strip()
      case ConvertModes.THUMB  => op.thumbnail(DOWNSIZE_HORIZ_PX, DOWNSIZE_VERT_PX, '>')
      case ConvertModes.RESIZE => op.strip().resize(DOWNSIZE_HORIZ_PX, DOWNSIZE_VERT_PX, '>')
    }
    if (GAUSSIAN_BLUG.isDefined) {
      op.gaussianBlur(GAUSSIAN_BLUG.get)
    }
    op.quality(JPEG_QUALITY_PC)
    op.samplingFactor(2.0, 1.0)
    op.addImage(fileNew.getAbsolutePath)
    LOGGER.trace("convert(): " + cmd.getCommand.mkString(" ") + " " + op.toString)
    cmd.run(op)
  }

}

case class PictureTooSmallException(filePath: String) extends Exception("Picture too small: " + filePath)


object ConvertModes extends Enumeration {
  type ConvertMode = Value
  val STRIP, THUMB, RESIZE = Value
}

/** Комбо-парсеры для img-части. */
object ImgUtilParsers extends JavaTokenParsers


/** Статическая часть ImgCrop, описывающего кадрирование картинки. */
object ImgCrop {
  import ImgUtilParsers._

  val CROP_STR_PARSER: Parser[ImgCrop] = {
    val whP: Parser[Int] = "\\d+".r ^^ { Integer.parseInt }
    val offIntP: Parser[Int] = "[+-_]\\d+".r ^^ { parseOffInt }
    (whP ~ ("[xX]".r ~> whP) ~ offIntP ~ offIntP) ^^ {
      case w ~ h ~ offX ~ offY =>
        ImgCrop(width=w, height=h, offX=offX, offY=offY)
    }
  }

  /** Метод для парсинга offset-чисел, которые всегда знаковые. */
  private def parseOffInt(offStr: String): Int = {
    // При URL_SAFE-кодировании используется _ вместо +. Этот символ нужно отбросить.
    val offStr1 = if (offStr.charAt(0) == '_') {
      offStr.substring(1)
    } else {
      offStr
    }
    Integer.parseInt(offStr1)
  }

  private def parseCropStr(cropStr: String) = parse(CROP_STR_PARSER, cropStr)

  def apply(cropStr: String): ImgCrop = parseCropStr(cropStr).get

  def maybeApply(cropStr: String): Option[ImgCrop] = {
    val pr = parseCropStr(cropStr)
    if (pr.successful)
      Some(pr.get)
    else
      None
  }

  private def optSign(v: Int, posSign: Char, acc: StringBuilder) {
    if (v < 0) {
      acc.append(v)
    } else {
      acc.append(posSign).append(v)
    }
  }
}

case class ImgCrop(width: Int, height: Int, offX: Int, offY: Int) extends MImgSizeT {
  import ImgCrop.optSign

  /**
   * Сконвертить в строку cropStr.
   * @return строку, пригодную для возврата в шаблоны/формы
   */
  def toCropStr: String = toStr('+')

  /**
   * Сериализовать данные в строку вида "WxH_offX-offY".
   *@return Строка, пригодная для использования в URL или ещё где-либо и обратимая назад в экземпляр ImgCrop.
   */
  def toUrlSafeStr: String = toStr('_')

  /** Хелпер для сериализации экземпляра класса. */
  def toStr(posSign: Char, szArgSuf: Option[Char] = None): String = {
    val sb = new StringBuilder(32)
    sb.append(width)
    szArgSuf foreach sb.append
    sb.append('x').append(height)
    szArgSuf foreach sb.append
    optSign(offX, posSign, sb)
    optSign(offY, posSign, sb)
    sb.toString()
  }

  /**
   * IM допускает задание размеров по отношению к текущим размерам изображения.
   * @return Строка вида 50%x60%+40+0
   */
  def toRelSzCropStr: String = toStr('+', Some('%'))

}

