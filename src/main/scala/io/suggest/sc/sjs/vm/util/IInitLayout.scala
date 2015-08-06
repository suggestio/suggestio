package io.suggest.sc.sjs.vm.util

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.08.15 16:51
 * Description: Интерфейс метода однократной инициализации DOM одной ViewModel'и.
 */
object IInitLayout {

  /** closure для вызова инициализации над реализациями [[IInitLayout]]. */
  def f: IInitLayout => Unit = {
    { _.initLayout() }
  }

}

trait IInitLayout {

  /** Инициализация текущей и подчиненных ViewModel'ей. */
  def initLayout(): Unit

}

trait IInitLayoutDummy extends IInitLayout {
  override def initLayout(): Unit = {}
}
