package io.suggest.common.text

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

}
