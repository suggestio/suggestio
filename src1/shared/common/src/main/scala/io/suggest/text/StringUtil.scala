package io.suggest.text

import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.MsgCodes
import japgolly.univeq._

import scala.annotation.tailrec

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
  def strLimitLen(str: String, maxLen: Int, ellipsis: String): String = {
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


}
