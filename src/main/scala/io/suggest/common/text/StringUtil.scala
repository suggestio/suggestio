package io.suggest.common.text

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.09.15 10:17
 * Description:
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

}
