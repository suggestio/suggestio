package io.suggest.sjs.common.controller

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.05.15 14:02
 * Description: Интерфейс для инициализации
 */
trait IInit {

  /** Запуск инициализации текущего модуля. */
  def init(): Unit

}

/** Пустая реализация [[IInit]]. */
trait IInitDummy extends IInit {
  override def init(): Unit = {}
}
