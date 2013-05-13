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


  /**
   * Сгенерить случайный символ из диапазона 0-9 a-z A-Z
   * @return случайный alphanumeric символ.
   */
  @tailrec
  def randomIdChar : Char = rnd.nextPrintableChar() match {
    case c if c>='0' && c<='9' || c>='A' && c<='Z' || c>='a' && c<='z'  => c
    case _ => randomIdChar
  }

}
