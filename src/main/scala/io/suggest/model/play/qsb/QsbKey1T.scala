package io.suggest.model.play.qsb

import io.suggest.common.qs.QsConstants
import play.api.mvc.QueryStringBindable

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.05.15 12:08
 * Description: Утиль для быстрой сборки названия на основе ключа, разделителя и суффикса.
 */
trait QsbKey1T {

  /** Разделитель ключа с верхнего уровня и ключа поля. */
  def KEY_DELIM = QsConstants.KEY_PARTS_DELIM_STR

  /** Собрать полное название qs-поля на основе переданных данных. */
  def key1(key: String, suf: String): String = {
    key + KEY_DELIM + suf
  }

  def key1F(key: String): String => String = {
    // TODO Надо ли это условие? key всегда должен быть, и не пустой.
    if (key.isEmpty)
      identity
    else
      key1(key, _)
  }

  /** Когда несколько полей, часто бывает актуально их конкатенировать в qs-строку с помощью этого метода. */
  def _mergeUnbinded(unbinded: TraversableOnce[String]): String = {
    unbinded
      .toIterator
      .filter(_.nonEmpty)
      .mkString("&")
  }

}


/**
  * Обычно в проекте используется смесь QueryStringBindable with [[QsbKey1T]].
  * Тут -- абстрактный класс, облегчающий жизнь компилятора и снижающий ресурсопотребление
  * скомпиленных реализаций QSB.
  */
abstract class QueryStringBindableImpl[T]
  extends QueryStringBindable[T]
  with QsbKey1T
