package io.suggest.text

import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.MsgCodes
import japgolly.univeq._

import scala.annotation.tailrec
import scala.util.Random

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.09.15 10:17
 * Description: Всякая утиль для строк.
 */
object StringUtil {

  /**
   * Стрип пустот слева.
   * @param s Исходная строка.
   * @return Строка, где whitespace-символы слева удалены.
   */
  def trimLeft(s: String): String = {
    s.replace("^\\s", "")
  }


  /** Лимитирование длины строки слева. Если строка длинее указанного порога,
    * то она будет урезана и в конце появится многоточие. */
  def strLimitLen(str: String, maxLen: Int, ellipsis: String = HtmlConstants.ELLIPSIS): String = {
    if (str.length <= maxLen) {
      str
    } else {
      str.substring(0, maxLen) + ellipsis
    }
  }


  /** Поиск первой осмысленной строки в множестве исходных.
    * Осмысленной строкой называется строка, которая не пуста и не начинается на TODO.
    *
    * Это костыль для dev-нужд, а не нормальных продакшен код.
    */
  def firstStringMakesSence(strings: String*): Option[String] = {
    strings.find { url =>
      url.nonEmpty && !url.startsWith(MsgCodes.`TODO`)
    }
  }


  implicit class StringCollUtil(private val coll: TraversableOnce[String]) extends AnyVal {

    /** Ленивая конкатенация строки из коллекции строк.
      * Использутся для ограниченного mkString над неограниченно-большой коллекцией.
      */
    def mkStringLimitLen(maxLen: Int, ellipsis: String = ""): String = {
      assert(maxLen > 0)

      val iter = coll.toIterator
      val sb = new StringBuilder(maxLen)

      @tailrec
      def __do(i: Int): String = {
        assert(i < 1000)

        val currLen = sb.length
        if (currLen ==* maxLen) {
          sb.toString()

        } else if (currLen > maxLen) {
          sb.setLength( maxLen )
          sb.setCharAt( maxLen, HtmlConstants.ELLIPSIS.charAt(0) )
          sb.toString()

        } else if (!iter.hasNext) {
          // Больше нечего добавлять в аккамулятор.
          sb.toString()

        } else {
          // Можно ещё добавлять текст в аккамулятор.
          val maxSegLen = maxLen - currLen
          val next = iter.next()
          if (next.length >= maxSegLen) {
            val next2 = strLimitLen(next, maxSegLen, ellipsis)
            sb.append( next2 )
            sb.toString()
          } else {
            sb.append( next )
            __do(i + 1)
          }
        }
      }

      __do(0)
    }

  }


  /**
   * Сгенерить случайны id-шник вида "64srGaf345TfQw34fa"
   * @param len Опциональная длина выходной строки.
   * @return Случайная строка.
   */
  def randomId(len: Int = 10): String = {
    _mkRandomId(len)(randomIdChar)
  }

  private def _mkRandomId(len: Int)(mkCharF: Random => Char): String = {
    val rnd = new Random()
    (1 to len)
      .iterator
      .map(_ => mkCharF(rnd))
      .mkString
  }


  /** Сгенерить случайный символ из диапазона 0-9 a-z A-Z
   * @return случайный alphanumeric символ.
   */
  @tailrec
  private def randomIdChar(rnd: Random): Char = {
    rnd.nextPrintableChar() match {
      case c if c>='0' && c<='9' || c>='A' && c<='Z' || c>='a' && c<='z'  => c
      case _ => randomIdChar(rnd)
    }
  }

  /** Генерация одного случайного lower-case символа латиницы.
   * @return Символ от 'a' до 'z'.
   */
  @tailrec
  def randomIdLatLcChar(rnd: Random): Char = {
    rnd.nextPrintableChar() match {
      case c if c>='a' && c<='z'  => c
      case c if c>='A' && c<='Z'  => c.toLower
      case _                      => randomIdLatLcChar(rnd)
    }
  }

  /** Генерация случайно строки из латинских символов от 'a' до 'z'.
   * @param len Длина результирующей строки.
   * @return Случайная строка вида "asdftbhdb" длины len.
   */
  def randomIdLatLc(len: Int = 10): String = {
    _mkRandomId(len)(randomIdLatLcChar)
  }

}
