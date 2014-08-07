package util.img

import java.awt.Color
import java.io.{FileInputStream, InputStreamReader, File}
import java.text.ParseException
import org.im4java.core.{IMOperation, ConvertCmd}

import play.api.Play.{current, configuration}
import util.PlayMacroLogsImpl
import scala.util.parsing.combinator.JavaTokenParsers
import scala.util.parsing.input.StreamReader

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.08.14 19:25
 * Description: Утиль для определения основных цветов на изображении.
 * База для работы: convert 12636786604889.jpg -gravity Center -crop 50%\! -gamma 2.0 -quantize Lab  +dither -colors 8 -format "%c" histogram:info:
 */
object MainColorDetector extends PlayMacroLogsImpl {

  import LOGGER._

  val PALETTE_MAX_COLORS_DFLT = configuration.getInt("mcd.palette.colors.max.dflt") getOrElse 8

  def convertToHistogram(imgFilepath: String, outFilepath: String, maxColors: Int) {
    // Запускаем команду,
    val op = new IMOperation()
    op.addImage(imgFilepath)
    op.gravity("Center")
    op.crop().addRawArgs("50%\\!") // -crop 50%\!
    op.gamma(2.0)
    op.quantize("Lab")
    op.p_dither()
    op.colors(8)
    op.format("%c")
    op.addRawArgs("histogram:info:" + outFilepath)
    val cmd = new ConvertCmd
    cmd.run(op)
  }
  

  /**
   * Картинка лежит в файле. Нужно определить у неё палитру основных цветов.
   * Для этого квантуем цвета в пространстве CIE Lab, чтобы визуально близкие цвета принадлежали к одному кванту.
   * @param img Исходная картинка.
   */
  def detectFilePalette(img: File, suppressErrors: Boolean, maxColors: Int = PALETTE_MAX_COLORS_DFLT): List[HistogramEntry] = {
    try {
      // Создаём временный файл для сохранения выхлопа convert histogram:info:
      val resultFile = File.createTempFile(getClass.getSimpleName, ".txt")
      try {
        convertToHistogram(img.getAbsolutePath, resultFile.getAbsolutePath, maxColors)
        // Читаем и парсим из файла гистограмму.
        val pr = HistogramParsers.parseFromFile(resultFile)
        pr getOrElse {
          lazy val msg = s"Failed to parse histogram file: ${resultFile.getAbsolutePath} ::\n $pr"
          if (suppressErrors) {
            warn(msg)
            Nil
          } else {
            throw new ParseException(msg, -1)
          }
        }

      } finally {
        resultFile.delete()
      }

    } catch {
      case ex: Exception =>
        if (suppressErrors) {
          warn(s"Failed to extract palette from picture ${img.getAbsolutePath}", ex)
          Nil
        } else {
          throw ex
        }
    }
  }

  def detectFileMainColor(img: File, suppressErrors: Boolean, maxColors: Int = PALETTE_MAX_COLORS_DFLT): Option[HistogramEntry] = {
    val hist = detectFilePalette(img, suppressErrors, maxColors)
    if (hist.isEmpty) {
      debug("Detected colors pallette is empty. img = " + img.getAbsolutePath)
      None
    } else {
      val result = hist.maxBy(_.frequency)
      Some(result)
    }
  }


  /** Дистанция между точками в трехмерном целочисленном пространстве цветов. Считаем по теореме Пифагора.
    * @param p1 Точка 1.
    * @param p2 Точка 2.
    * @param exp Значение показателя степеней. В теореме Пифагора используется степень и корень по показателю 2,
    *            но можно задать любой другой.
    * @return Расстояние между точками в 3-мерном пространстве.
    */
  def colorDistance3D(p1: ColorPoint3D, p2: ColorPoint3D, exp: Double = 2.0): Double = {
    val expDst = Math.pow(p2.x - p1.x, exp)  +  Math.pow(p2.y - p1.y, exp)  +  Math.pow(p2.z - p1.z, exp)
    Math.pow(expDst, 1.0 / exp)
  }

}


/**
 * Утиль для парсинга выхлопов IM histogram:info вида:
 *   21689: ( 42, 45, 12) #2A2D0C srgb(42,45,12)
 *    5487: ( 54, 42, 18) #362A12 srgb(54,42,18)
 *   83956: (100, 96, 33) #646021 srgb(100,96,33)
 *  ...
 */
object HistogramParsers extends JavaTokenParsers {

  // Используем def вместо val, чтобы сэкономить PermGen, т.к. парсер нужен изредка, а не постоянно.

  def FREQ_P = wholeNumber ^^ { _.toLong }

  def BYTE_NUMBER_P = """(2[0-4]\d|25[0-5]|1?\d{2})""".r ^^ { _.toInt }

  def COMMA_SP_SEP: Parser[_] = """,\s*""".r
  def RGB_TUPLE_P = "(" ~> (BYTE_NUMBER_P <~ COMMA_SP_SEP) ~ (BYTE_NUMBER_P <~ COMMA_SP_SEP) ~ BYTE_NUMBER_P <~ ")"

  def HEX_COLOR_P = "#" ~> "(?i)[0-9A-F]{6}".r

  def SRGB_REC_P = {
    val comma: Parser[_] = ","
    val p = "srgb(" ~> (BYTE_NUMBER_P <~ comma) ~ (BYTE_NUMBER_P <~ comma) ~ BYTE_NUMBER_P <~ ")"
    p ^^ { case r ~ g ~ b  =>  RGB(red = r, green = g, blue = b) }
  }

  def LINE_PARSER = {
    val p = (FREQ_P <~ ":" <~ RGB_TUPLE_P) ~ HEX_COLOR_P ~ SRGB_REC_P
    p ^^ {
      case freq ~ hexColor ~ rgb  =>  HistogramEntry(freq, hexColor, rgb = rgb)
    }
  }

  def MULTILINE_PARSER = rep(LINE_PARSER)


  /**
   * Фунцкия, которая парсит файл, содержащий выхлоп histogram:info IM.
   * @param histogramFile Файл, который будет считан поточно.
   * @return Результат работы парсера, который содержит
   */
  def parseFromFile(histogramFile: File): ParseResult[List[HistogramEntry]] = {
    val is = new FileInputStream(histogramFile)
    try {
      val reader = StreamReader( new InputStreamReader(is) )
      parseAll(MULTILINE_PARSER, reader)
    } finally {
      is.close()
    }
  }

}


/** Интерфейс цветовой точки в абстрактном 3-мерном пространстве цветов. */
sealed trait ColorPoint3D {
  def x: Int
  def y: Int
  def z: Int
}

/** Цвет-точка в 3-мерном пространстве цветов RGB. */
case class RGB(red: Int, green: Int, blue: Int) extends ColorPoint3D {
  override def x = red 
  override def y = green 
  override def z = blue 
}

object RGB {
  def hex2rgb(colorStr: String): RGB = {
    val cs1 = if (colorStr startsWith "#")
      colorStr
    else
      "#" + colorStr
    val c = Color.decode(cs1)
    RGB(c.getRed, c.getGreen, c.getBlue)
  }

  def apply(hex: String) = hex2rgb(hex)
}

/**
 * Распарсенный ряд гистограммы. Включает в себя абсолютную частоту и код цвета.
 * @param frequency Кол-во пикселей с указанным цветом.
 * @param colorHex HEX-код цвета в виде строки: "FFFFFF".
 */
case class HistogramEntry(frequency: Long, colorHex: String, rgb: RGB)

