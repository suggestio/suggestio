package io.suggest.model

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.09.15 11:45
 * Description: Трейт для поддержки сборки полных имён полей на основе префикса.
 * Названия родительского и дочернего полей конкатенируются точкой.
 */
trait PrefixedFnBase {

  /** Разделитель имён родительского и дочернего полей. */
  protected def _FN_DELIMITER = "."

  /** Сборка полного имени дочернего поля на основе основного имени и имени родительского подя.
    * @param fn Имя дочернего поля относительно родительского поля.
    * @param parent Имя родительского поля.
    * @return Полное (абсолютное) имя поля.
    */
  protected def _fullFn(parent: String, fn: String): String = {
    parent + _FN_DELIMITER + fn
  }

}


/** Поддержка префиксированных имён полей на основе статического родительского поля. */
trait PrefixedFn extends PrefixedFnBase {

  /** Название родительского поля. */
  protected def _PARENT_FN: String

  /** Сборка полного имени дочернего поля на основе основного имени.
    * @param fn Имя дочернего поля относительно родительского поля.
    * @return Полное (абсолютное) имя поля.
    */
  protected def _fullFn(fn: String): String = {
    _fullFn(_PARENT_FN, fn)
  }

}
