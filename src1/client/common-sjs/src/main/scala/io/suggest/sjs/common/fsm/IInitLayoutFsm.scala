package io.suggest.sjs.common.fsm

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.12.15 22:33
 * Description: Бывает, что нужно инициализировать vm'ки элементов с привязкой к FSM.
 * Тут трейты для стандартизации этого процесса.
 */
object IInitLayoutFsm {

  /**
   * Быстрая сборка анонимной фунции для запуска инициализации.
   * @param fsm Конечный автомат, к которому идёт привязка.
   * @return Экземпляр функции.
   */
  def f(fsm: SjsFsm) = {
    {x: IInitLayoutFsm =>
      x.initLayout(fsm)
    }
  }

}


trait IInitLayoutFsm {
  def initLayout(fsm: SjsFsm): Unit
}


/** Пустая реализация [[IInitLayoutFsm]] */
trait IInitLayoutFsmDummy extends IInitLayoutFsm {
  override def initLayout(fsm: SjsFsm): Unit = {}
}



