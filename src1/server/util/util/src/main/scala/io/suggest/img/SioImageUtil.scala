package io.suggest.img

import java.io.File
import java.{lang => jl}

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
  *
  * Код здесь считается доисторическим, но он пока ещё используетя.
  * В [www] есть DynImgUtil, который дергает im convert на основе заданного алгоритма,
  * а тут -- некая очень тривиальная ипостась convert'а, которая является мелким частным случаем
  * функций, доступных DynImgUtil.
  * TODO Итого: нужно реализовать это через DynImgUtil, выкинув этот модуль окончательно.
 */

trait SioImageUtilT extends IMacroLogs {

  /** Качество сжатия jpeg. */
  def JPEG_QUALITY_PC: Double

  /** Размывка для сокрытия артифактов. */
  def GAUSSIAN_BLUG: Option[jl.Double] = None

  /** Некое цветовое переплетение. Позволяет делать progressive jpeg.
    * @see [[http://www.imagemagick.org/script/command-line-options.php#interlace]] */
  def INTERLACING: Option[String] = Some("Plane")


  def identify(filePath: String) = new Info(filePath, true)


  /**
   * Конвертировать с помощью ImageMagick. Есть режимы strip или thumbnail.
   * @param fileOld Файл с исходной картинкой.
   * @param fileNew Файл, в который нужно записать обработанную картинку.
   * @param strip Стрипануть всякие метаданные из выхлопа?
   * @param crop Опциональный кроп картинки.
   */
  def convert(fileOld: File, fileNew: File, strip: Boolean, crop: Option[MCrop] = None): Unit = {
    val cmd = new ConvertCmd
    val op = new IMOperation()

    // TODO Нужно брать рандомный кадр из gif вместо нулевого, который может быть пустым.
    op.addImage(fileOld.getAbsolutePath + "[0]")   // (#117) Без указания кадра, будет ошибка и куча неудаленных файлов в /tmp.
    for (i <- INTERLACING)
      op.interlace(i)

    // Кроп, задаваемый юзером: портирован из альтерраши.
    for (c <- crop)
      op.crop(c.width, c.height, c.offX, c.offY)

    if (strip)
      op.strip()

    for (gb <- GAUSSIAN_BLUG)
      op.gaussianBlur(gb)

    op.quality(JPEG_QUALITY_PC)
    op.samplingFactor(2.0, 1.0)
    op.addImage(fileNew.getAbsolutePath)

    LOGGER.trace("convert(): " + cmd.getCommand.iterator().asScala.mkString(" ") + " " + op.toString)

    cmd.run(op)
  }

}
