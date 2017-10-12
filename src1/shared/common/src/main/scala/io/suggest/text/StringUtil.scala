package io.suggest.text

import io.suggest.i18n.MsgCodes

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

}
