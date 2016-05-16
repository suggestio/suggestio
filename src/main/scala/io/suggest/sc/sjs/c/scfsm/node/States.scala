package io.suggest.sc.sjs.c.scfsm.node

import io.suggest.sc.sjs.c.scfsm.grid.Append

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.08.15 16:36
 * Description: Фасад пакета scfms.node. Здесь содержаться полуготовые состояния цепочки инициализации узла.
 */
trait States extends Index with Welcome with Append {


  /** Трейт состояния запуска запроса index и ожидания его исполнения. */
  trait NodeInit_GetIndex_WaitIndex_StateT
    extends GetIndexStateT
    with WaitIndexStateT

}
