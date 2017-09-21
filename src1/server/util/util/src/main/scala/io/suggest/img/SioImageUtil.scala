package io.suggest.img

import java.io.File

import io.suggest.img.ConvertModes.ConvertMode
import io.suggest.img.crop.MCrop
import io.suggest.util.logs.IMacroLogs
import org.im4java.core.{ConvertCmd, IMOperation, Info}

import scala.collection.JavaConverters._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.03.13 17:39
 * Description: Функции работы с картинками: фетч, обработка и т.д.
 * Есть ряд настроек для манипуляции с картинками, поэтому тут трейт вместо object. На стороне конкретных проектов
 * уже можно делать конкретную реализацию (обычно через object).
 */

trait SioImageUtilT extends IMacroLogs {

  /** Максимальный размер сторон будущей картинки (новая картинка должна вписываться в
    * прямоугольник с указанныыми сторонами). */
  def DOWNSIZE_VERT_PX: Integer
  def DOWNSIZE_HORIZ_PX: Integer

  /** Качество сжатия jpeg. */
  def JPEG_QUALITY_PC: Double

  /** Размывка для сокрытия артифактов. */
  def GAUSSIAN_BLUG: Option[java.lang.Double] = None

  /** Некое цветовое переплетение. Позволяет делать progressive jpeg.
    * @see [[http://www.imagemagick.org/script/command-line-options.php#interlace]] */
  def INTERLACING: Option[String] = Some("Plane")


  def identify(file: File) = new Info(file.getAbsolutePath, true)


  /**
   * Конвертировать с помощью ImageMagick. Есть режимы strip или thumbnail.
   * @param fileOld Файл с исходной картинкой.
   * @param fileNew Файл, в который нужно записать обработанную картинку.
   * @param mode Режим работы конвертера. По умолчанию - RESIZE.
   * @param crop Опциональный кроп картинки.
   */
  def convert(fileOld:File, fileNew:File, mode:ConvertMode = ConvertModes.RESIZE, crop: Option[MCrop] = None) {
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
    LOGGER.trace("convert(): " + cmd.getCommand.iterator().asScala.mkString(" ") + " " + op.toString)
    cmd.run(op)
  }

}


object ConvertModes extends Enumeration {
  type ConvertMode = Value
  val STRIP, THUMB, RESIZE = Value
}

