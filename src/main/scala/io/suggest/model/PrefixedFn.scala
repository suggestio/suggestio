package io.suggest.model

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.09.15 11:45
 * Description: Трейт для поддержки сборки полных имён полей на основе префикса.
 * Названия родительского и дочернего полей конкатенируются точкой.
 */
trait PrefixedFn {

  /** Название родительского поля. */
  protected def _PARENT_FN: String

  /** Разделитель имён родительского и дочернего полей. */
  protected def _FN_DELIMITER = "."

  /** Сборка полного имени дочернего поля на основе основного имени.
    * @param fn Имя дочернего поля относительно родительского поля.
    * @return Полное (абсолютное) имя поля.
    */
  protected def _fullFn(fn: String): String = {
    _PARENT_FN + _FN_DELIMITER + fn
  }

}
