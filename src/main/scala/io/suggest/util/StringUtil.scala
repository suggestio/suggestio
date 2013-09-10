package io.suggest.util

import annotation.tailrec
import scala.util.Random

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.03.13 16:16
 * Description: Функции для строк.
 */

object StringUtil {

  private val rnd = new Random(System.currentTimeMillis())

  /**
   * Сгенерить случайны id-шник вида "64srGaf345TfQw34fa"
   * @param len Опциональная длина выходной строки.
   * @return Случайная строка.
   */
  def randomId(len:Int = 10) = 1 to len map {_ => randomIdChar} mkString


  /** Сгенерить случайный символ из диапазона 0-9 a-z A-Z
   * @return случайный alphanumeric символ.
   */
  @tailrec
  def randomIdChar : Char = rnd.nextPrintableChar() match {
    case c if c>='0' && c<='9' || c>='A' && c<='Z' || c>='a' && c<='z'  => c
    case _ => randomIdChar
  }

  /** Генерация одного случайного lower-case символа латиницы.
   * @return Символ от 'a' до 'z'.
   */
  @tailrec
  def randomIdLatLcChar: Char = rnd.nextPrintableChar() match {
    case c if c>='a' && c<='z'  => c
    case c if c>='A' && c<='Z'  => c.toLower
    case _                      => randomIdLatLcChar
  }

  /** Генерация случайно строки из латинских символов от 'a' до 'z'.
   * @param len Длина результирующей строки.
   * @return Случайная строка вида "asdftbhdb" длины len.
   */
  def randomIdLatLc(len: Int = 10) = 1 to len map { _ => randomIdLatLcChar } mkString

}
