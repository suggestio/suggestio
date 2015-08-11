package util.img

import java.io.{FileInputStream, InputStreamReader, File}
import java.nio.file.Files
import java.text.ParseException
import models.im.{MLocalImg, ImOp, MImg, Im4jAsyncSuccessProcessListener}
import org.im4java.core.{IMOperation, ConvertCmd}

import play.api.Play.{current, configuration}
import util.PlayMacroLogsImpl
import models.blk.AdColorFns.IMG_BG_COLOR_FN.{toString => IMG_BG_COLOR_FN}
import models.im._
import scala.util.parsing.combinator.JavaTokenParsers
import scala.util.parsing.input.StreamReader
import scala.concurrent.Future
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
  def convertToHistogram(imgFilepath: String, outFilepath: String, maxColors: Int, preImOps: Seq[ImOp] = Nil): Future[_] = {
    // Запускаем команду,
    val op = new IMOperation()
    op.addImage(imgFilepath)
    preImOps.foreach { imOp =>
      imOp.addOperation(op)
    }
    op.gravity("Center")
    op.crop().addRawArgs("50%\\!") // -crop 50%\!
    op.gamma(2.0)
    op.quantize("Lab")
    op.p_dither()
    op.colors(8)
    op.format("%c")
    op.addRawArgs("histogram:info:" + outFilepath)
    val cmd = new ConvertCmd
    cmd.setAsyncMode(true)
    val listener = new Im4jAsyncSuccessProcessListener
    cmd.addProcessEventListener(listener)
    val tstampStart = if (LOGGER.underlying.isDebugEnabled) System.currentTimeMillis() else 0L
    cmd.run(op)
    val resFut = listener.future
    if (LOGGER.underlying.isDebugEnabled) {
      resFut onComplete { case res =>
        debug(s"convertToHistogram(img=$imgFilepath, out=$outFilepath, maxColors=$maxColors): It took ${System.currentTimeMillis() - tstampStart} ms. Result is $res")
      }
    }
    resFut
  }
  

  /**
   * Картинка лежит в файле. Нужно определить у неё палитру основных цветов.
   * Для этого квантуем цвета в пространстве CIE Lab, чтобы визуально близкие цвета принадлежали к одному кванту.
   * @param img Исходная картинка.
   */
  def detectFilePaletteUnsorted(img: File, suppressErrors: Boolean, maxColors: Int = PALETTE_MAX_COLORS_DFLT,
                        preImOps: Seq[ImOp] = Nil): Future[List[HistogramEntry]] = {
    // Создаём временный файл для сохранения выхлопа convert histogram:info:
    val resultFile = File.createTempFile(getClass.getSimpleName, ".txt")
    val resultFut = convertToHistogram(img.getAbsolutePath, resultFile.getAbsolutePath, maxColors, preImOps) map { _ =>
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
    }
    resultFut.onComplete { case _ =>
      resultFile.delete()
    }
    if (suppressErrors) {
      resultFut recoverWith {
        case ex: Exception =>
          warn(s"Failed to extract palette from picture ${img.getAbsolutePath}", ex)
          Future successful Nil
      }
    }
    resultFut
  }

  /**
   * Определить основной цвет на картинке.
   * @param img Файл картинки.
   * @param suppressErrors Подавлять ошибки?
   * @param maxColors Необязательный размер промежуточной палитры и гистограммы.
   * @return None при ошибке и suppressErrors или если картинка вообще не содержит цветов.
   */
  def detectFileMainColor(img: File, suppressErrors: Boolean, maxColors: Int = PALETTE_MAX_COLORS_DFLT,
                          preImOps: Seq[ImOp] = Nil): Future[Option[HistogramEntry]] = {
    detectFilePaletteUnsorted(img, suppressErrors, maxColors, preImOps) map { hist =>
      if (hist.isEmpty) {
        debug("Detected colors pallette is empty. img = " + img.getAbsolutePath)
        None
      } else {
        val result = hist.maxBy(_.frequency)
        Some(result)
      }
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


  /** Результат вызова prepareImg(). */
  case class PrepareImgResult(localOpt: Option[MLocalImg], imOps: Seq[ImOp])

  /**
   * Подготовить указанную картинку к извлечению гистограммы.
   * @param bgImg4s Данные по картинке.
   * @return Фьючерс с данными картинки.
   */
  def prepareImg(bgImg4s: MImg): Future[PrepareImgResult] = {
    lazy val logPrefix = s"prepareImg(${bgImg4s.fileName}): "
    // toLocalImg не существовует обычно вообще (ибо голый orig [+ crop]). Поэтому сразу ищем оригинал, но не теряя надежды.
    val localOrigImgFut = bgImg4s
      .original
      .toLocalImg
    // Всё-таки ищем отропанный результат.
    var localImg2Fut = bgImg4s.toLocalImg
      .filter(_.exists(_.isExists))
      .map { v => PrepareImgResult(v, Seq.empty[ImOp]) }
    // Если исходная картинка - чистый оригинал, то отрабатывать отсутствие произодной картинки не требуется.
    if (bgImg4s.hasImgOps) {
      // Если исходная картинка - обрезок, то можно изъять операции из исходной картинки и повторить их на оригинале вместе с генерацией гистограммы.
      localImg2Fut = localImg2Fut.recoverWith {
        case ex: NoSuchElementException =>
          val resFut = localOrigImgFut
            .map { v => PrepareImgResult(v, bgImg4s.dynImgOps) }
          trace(s"${logPrefix}Derived img not exists. Re-applying ${bgImg4s.dynImgOps.size} IM ops to original: ${bgImg4s.dynImgOps}")
          resFut
      }
    }
    // Подавляем возможные исключения.
    localImg2Fut recover {
      case ex: Throwable =>
        warn(s"${logPrefix}Failed to find img requested.", ex)
        PrepareImgResult(None, Nil)
    }
  }

  /**
   * Поиск "главного" цвета для указанной (через указатель) картинки.
   * @param bgImg4s Исходная картинка.
   * @return Фьючерс с результатом работы.
   */
  def detectColorFor(bgImg4s: MImg): Future[ImgBgColorUpdateAction] = {
    lazy val logPrefix = s"detectColorFor(${bgImg4s.fileName}): "
    prepareImg(bgImg4s) flatMap {
      case PrepareImgResult(Some(localImg), preImOps) =>
        detectFileMainColor(localImg.file, suppressErrors = true, preImOps = preImOps) map { heOpt =>
          val result = he2updateAction(heOpt)
          trace(s"${logPrefix}Detected color info for already saved orig img: $result")
          result
        }

      // Почему-то нет картинки.
      case _ =>
        warn(s"${logPrefix}Img not found anywhere: ${bgImg4s.fileName}")
        Future successful Keep
    }
  }

  /**
   * Получить палитру цветов для указанной картинки.
   * @param bgImg4s Данные по исходной картинке.
   * @param maxColors Макс.размер результирующей палитры.
   * @return Фьючерс с гистограммой, где самый частый в начале, и далее по убыванию.
   */
  def detectPaletteFor(bgImg4s: MImg, maxColors: Int = PALETTE_MAX_COLORS_DFLT): Future[Histogram] = {
    prepareImg(bgImg4s) flatMap {
      case PrepareImgResult(Some(localImg), preImOps) =>
        detectFilePaletteUnsorted(localImg.file, suppressErrors = true, preImOps = preImOps, maxColors = maxColors)
          .map { hist => Histogram( hist.sortBy(v => -v.frequency) ) }
      case other =>
        Future failed new IllegalArgumentException("Failed to extract palette. prepareImg() result is " + other)
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
  
  protected def RGB_P: Parser[RGB] = {
    val np = BYTE_NUMBER_P
    val comma = COMMA_SP_SEP
    val npc = np <~ comma
    val p = npc ~ npc ~ np <~ opt(comma ~> np)
    p ^^ {
      case r ~ g ~ b  =>
        RGB(red = r, green = g, blue = b)
    }
  }

  // 2014.oct.30: При парсинге PNG может вылетать RGBA-кортеж, который содержит прозрачность (обычно, нулевую) Мы её дропаем.
  def RGB_TUPLE_P = "(" ~> RGB_P <~ ")"

  def HEX_COLOR_P: Parser[String] = {
    "#" ~> "(?i)[0-9A-F]{6}".r <~ opt("[0-9A-F]{2}".r)
  }

  /** Запись цвета в srgb. Следует помнить, что для RGB(0,0,0) im возвращает строку "black". */
  def SRGB_REC_P = "s?rgba?\\(".r ~> RGB_P <~ ")"

  /** "gray", "gray(255)", "white", etc. */
  def COLOR_NAME_P: Parser[String] = "(?i)[_a-z ]+[(0-9)]*".r

  def LINE_PARSER = {
    val p = (FREQ_P <~ ":") ~ RGB_TUPLE_P ~ HEX_COLOR_P <~ (SRGB_REC_P | COLOR_NAME_P)
    p ^^ {
      case freq ~ rgb ~ hexColor =>
        HistogramEntry(freq, hexColor, rgb = rgb)
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

