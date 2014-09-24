package util.img

import java.awt.Color
import java.io.{FileInputStream, InputStreamReader, File}
import java.nio.file.Files
import java.text.ParseException
import models.MUserImg2
import models.BlockConf
import org.apache.commons.io.{FileUtils, FilenameUtils}
import org.im4java.core.{IMOperation, ConvertCmd}

import play.api.Play.{current, configuration}
import util.PlayMacroLogsImpl
import util.blocks.BlocksUtil.{BlockImgMap, IMG_BG_COLOR_FN}
import scala.util.parsing.combinator.JavaTokenParsers
import scala.util.parsing.input.StreamReader
import scala.concurrent.{Future, future}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.08.14 19:25
 * Description: Утиль для определения основных цветов на изображении.
 * База для работы: convert 12636786604889.jpg -gravity Center -crop 50%\! -gamma 2.0 -quantize Lab  +dither -colors 8 -format "%c" histogram:info:
 */
object MainColorDetector extends PlayMacroLogsImpl {

  import LOGGER._

  /** Дефолтовое значение размера промежуточной палитры цветовой гистограммы. */
  val PALETTE_MAX_COLORS_DFLT = configuration.getInt("mcd.palette.colors.max.dflt") getOrElse 8

  /**
   * Отрендерить гистограмму по указанной картинке в указанный файл.
   * @param imgFilepath Путь в ФС до картинки.
   * @param outFilepath Путь к до файла, в который надо сохранить гистограмму.
   * @param maxColors Необязательный размер палитры и гистограммы.
   */
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
    val tstampStart = if (LOGGER.underlying.isDebugEnabled) System.currentTimeMillis() else 0L
    cmd.run(op)
    debug(s"convertToHistogram(img=$imgFilepath, out=$outFilepath, maxColors=$maxColors): It took ${System.currentTimeMillis() - tstampStart} ms.")
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
          val histContent = Files.readAllBytes(resultFile.toPath)
          val msgSb = new StringBuilder(histContent.length + 128,  "Failed to understand histogram file: \n")
          msgSb append pr
          msgSb append '\n'
          msgSb append " Histogram file content was:\n"
          msgSb append new String(histContent)
          val msg = msgSb.toString()
          if (suppressErrors) {
            warn(msg )
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

  /**
   * Определить основной цвет на картинке.
   * @param img Файл картинки.
   * @param suppressErrors Подавлять ошибки?
   * @param maxColors Необязательный размер промежуточной палитры и гистограммы.
   * @return None при ошибке и suppressErrors или если картинка вообще не содержит цветов.
   */
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



  /**
   * Выполнить действия, связанные с определением цвета фона и возвращение промежуточного результата для MAd.
   * @param oldColors Старый набор цветов. Может содержать старое значение фона.
   * @param newBim Новый набор картинок.
   * @param bc Конфиг блока.
   * @return Фьючерс с действием по обновлению карты цветов рекламной карточки.
   */
  def adPrepareUpdateBgColors(newBim: BlockImgMap, bc: BlockConf, oldColors: Map[String, String] = Map.empty): Future[ImgBgColorUpdateAction] = {
    lazy val logPrefix = "adPrepareUpdateBgColors(): "
    bc.getBgImg(newBim).fold [Future[ImgBgColorUpdateAction]] {
      trace(s"${logPrefix}No background image - nothing to do.")
      Future successful Remove    // TODO Возвращать Keep?
    } { bgImg4s =>
      trace(s"${logPrefix}Starting color detecting bc=${bc.id}  oldColors = ${oldColors.filterKeys(_ == IMG_BG_COLOR_FN)}  newBim = $bgImg4s")
      bgImg4s.iik match {
        // Если есть новая tmp-картинка фона, то нужно запустить определение цвета для неё и вернуть новый цвет фона.
        case tiik: TmpImgIdKey =>
          future {
            val heOpt = detectFileMainColor(tiik.mptmp.file, suppressErrors = true)
            val result = he2updateAction(heOpt)
            trace(s"${logPrefix}Detected color info for tmp img: $result")
            result
          }
        // Бывают разные ситуации: осталась старая картинка и есть старый цвет. Нужно этот цвет портануть в новую карту.
        // А бывает, что есть старая картинка, а цвета нет. Нужно выкачать картинку из модели в tmp, определить цвет и дропнуть картинку.
        case oiik: OrigImgIdKey =>
          oldColors.get(IMG_BG_COLOR_FN).fold [Future[ImgBgColorUpdateAction]] {
            // Старая картинка в базе, а цвета нет. Такое бывает сразу после апдейта до новой версии SIO-market.
            MUserImg2.getByStrId(oiik.data.rowKey, oiik.origQualifierOpt) map {
              case Some(oimg) =>
                val fileSuffix = {
                  val e1 = FilenameUtils.getExtension(oiik.filename)
                  if (e1.isEmpty) ".jpg" else "." + e1
                }
                val tempImg = File.createTempFile("adPrepareUpdateBgColors", fileSuffix)
                try {
                  FileUtils.writeByteArrayToFile(tempImg, oimg.imgBytes)
                  val heOpt = detectFileMainColor(tempImg, suppressErrors = true)
                  val result = he2updateAction(heOpt)
                  trace(s"${logPrefix}Detected color info for already saved orig img: $result")
                  result
                } finally {
                  tempImg.delete()
                }
              case None =>
                warn(s"${logPrefix}Failed to fetch ${oiik.filename} from orig model: 404.")
                Keep
            }
          } { oldBgColor =>
            // Сохранить старый цвет для старой картинки.
            trace(s"${logPrefix}Keeping color of original color: $oldBgColor")
            Future successful Keep
          }
      }
    }
  }

  /** Конвертация выхлопа detectMainColor() в инструкцию по обновлению карты цветов. */
  def he2updateAction(heOpt: Option[HistogramEntry], default: ImgBgColorUpdateAction = Keep): ImgBgColorUpdateAction = {
    if (heOpt.isDefined)
      Update(heOpt.get.colorHex)
    else
      default
  }


  sealed trait ImgBgColorUpdateAction {
    def updateColors(colors: Map[String, String]): Map[String, String]
  }
  case object Keep extends ImgBgColorUpdateAction {
    override def updateColors(colors: Map[String, String]): Map[String, String] = {
      colors
    }
  }
  case class Update(newColorHex: String) extends ImgBgColorUpdateAction {
    override def updateColors(colors: Map[String, String]): Map[String, String] = {
      colors + (IMG_BG_COLOR_FN -> newColorHex)
    }
  }
  case object Remove extends ImgBgColorUpdateAction {
    override def updateColors(colors: Map[String, String]): Map[String, String] = {
      // TODO Opt проверять карту colors на наличие цвета фона?
      colors - IMG_BG_COLOR_FN
    }
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

  def BYTE_NUMBER_P = """(2[0-4]\d|25[0-5]|1?\d{1,2})""".r ^^ { _.toInt }

  def COMMA_SP_SEP: Parser[_] = """,\s*""".r
  def RGB_TUPLE_P = {
    val p = "(" ~> (BYTE_NUMBER_P <~ COMMA_SP_SEP) ~ (BYTE_NUMBER_P <~ COMMA_SP_SEP) ~ BYTE_NUMBER_P <~ ")"
    p ^^ {
      case r ~ g ~ b  =>  RGB(red = r, green = g, blue = b)
    }
  }

  def HEX_COLOR_P = "#" ~> "(?i)[0-9A-F]{6}".r

  /** Запись цвета в srgb. Следует помнить, что для RGB(0,0,0) im возвращает строку "black". */
  def SRGB_REC_P = {
    val comma: Parser[_] = ","
    val p = "srgb(" ~> (BYTE_NUMBER_P <~ comma) ~ (BYTE_NUMBER_P <~ comma) ~ BYTE_NUMBER_P <~ ")"
    p ^^ {
      case r ~ g ~ b  =>  RGB(red = r, green = g, blue = b)
    }
  }

  def COLOR_NAME_P: Parser[String] = "(?i)[_a-z ]+".r

  def LINE_PARSER = {
    val p = (FREQ_P <~ ":") ~ RGB_TUPLE_P ~ HEX_COLOR_P <~ (SRGB_REC_P | COLOR_NAME_P)
    p ^^ {
      case freq ~ rgb ~ hexColor =>  HistogramEntry(freq, hexColor, rgb = rgb)
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
  /**
   * Парсер из hex в [[RGB]].
   * @param colorStr hex-строка вида "FFAA33" или "#FFAA33".
   * @return Инстанс RGB.
   *         Exception, если не удалось строку осилить.
   */
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

