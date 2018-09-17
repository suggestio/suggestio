package io.suggest.primo

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.08.15 12:10
 * Description: Интерфейс для метода reset() с сайд-эффектами.
 * Изначально использовался для некоторых ViewModel'ей.
 */

object IReset {

  /** Функция для запуска reset'а на произвольном аргументе. */
  def f = { r: IReset => r.reset() }

}


/** Интерфейс элементов, поддерживающих reset(). */
trait IReset {

  /** Сброс какого-то (текущего, внутреннего) состояния чего-либо изменяемого. */
  def reset(): Unit

}


/** Пустая реализация [[IReset]]. */
trait IResetDummy extends IReset {
  override def reset(): Unit = {}
}