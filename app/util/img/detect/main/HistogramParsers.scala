package util.img.detect.main

import java.io.{File, FileInputStream, InputStreamReader}

import models.im.{HistogramEntry, RGB}

import scala.util.parsing.combinator.JavaTokenParsers
import scala.util.parsing.input.StreamReader

/**
 * Утиль для парсинга выхлопов IM histogram:info вида:
 *   21689: ( 42, 45, 12) #2A2D0C srgb(42,45,12)
 *    5487: ( 54, 42, 18) #362A12 srgb(54,42,18)
 *   83956: (100, 96, 33) #646021 srgb(100,96,33)
 *  ...
 */
class HistogramParsers extends JavaTokenParsers {

  // Используем def вместо val, чтобы сэкономить PermGen, т.к. парсер нужен изредка, а не постоянно.

  def FREQ_P = wholeNumber ^^ { _.toLong }

  def BYTE_NUMBER_P = """(2[0-4]\d|25[0-5]|1?\d{1,2})""".r ^^ { _.toInt }

  def COMMA_SP_SEP: Parser[_] = """,\s*""".r

  protected def RGB_P: Parser[RGB] = {
    val np = BYTE_NUMBER_P
    val comma = COMMA_SP_SEP
    val npc = np <~ comma
    val alphaP = opt(comma ~> decimalNumber)
    val p = (npc ~ npc ~ np) <~ alphaP
    p ^^ {
      case r ~ g ~ b  =>
        RGB(red = r, green = g, blue = b)
    }
  }

  // 2014.oct.30: При парсинге PNG может вылетать RGBA-кортеж, который содержит прозрачность (обычно, нулевую) Мы её дропаем.
  def RGB_TUPLE_P = "(" ~> RGB_P <~ ")"

  def HEX_COLOR_P: Parser[String] = {
    val colorHexP = "(?i)[0-9A-F]{6}".r
    val alphaP = opt("[0-9A-F]{2}".r)
    "#" ~> colorHexP <~ alphaP
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
