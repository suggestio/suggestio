package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.c.cutil.ScFsmStub
import io.suggest.sc.sjs.m.mgrid.{VScroll, GridBlockClick}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.06.15 16:16
 * Description: Конечный автомат для поддержки выдачи.
 */
trait GridFsm extends ScFsmStub {

  /** Трейт для сборки состояний, в которых доступна сетка карточек и клики по ней. */
  protected trait TiledStateT extends FsmState {

    /** Обработка кликов по карточкам в сетке. */
    protected def handleGridBlockClick(gbc: GridBlockClick): Unit = {
      // TODO Нужно запустить focused-запрос и перключиться на focused-выдачу.
      ???
    }

    /** Реакция на вертикальный скроллинг. */
    protected def handleVScroll(vs: VScroll): Unit = {
      // TODO Нужно запустить подгрузку ещё карточек, если возможно.
      ???
    }

    /** Обработка событий сетки карточек. */
    override def receiverPart: Receive = {
      // Клик по одной из карточек в сетке оных.
      case gbc: GridBlockClick =>
        handleGridBlockClick(gbc)

      // Вертикальный скроллинг в плитке.
      case vs: VScroll =>
        handleVScroll(vs)
    }

  }


}
