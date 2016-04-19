package io.suggest.model.play.qsb

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.05.15 12:08
 * Description: Утиль для быстрой сборки названия на основе ключа, разделителя и суффикса.
 */
trait QsbKey1T {

  /** Разделитель ключа с верхнего уровня и ключа поля. */
  def KEY_DELIM = "."

  /** Собрать полное название qs-поля на основе переданных данных. */
  def key1(key: String, suf: String): String = {
    key + KEY_DELIM + suf
  }

  def key1F(key: String) = key1(key, _: String)

}
