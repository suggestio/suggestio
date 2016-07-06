package io.suggest.sc.sjs.c.scfsm.foc

import io.suggest.sc.sjs.vm.foc.FRoot
import io.suggest.sc.ScConstants.Focused.SLIDE_ANIMATE_MS
import io.suggest.sc.sjs.c.scfsm.grid.OnGridBase
import io.suggest.sc.sjs.c.scfsm.ust.StateToUrlT
import io.suggest.sc.sjs.m.magent.VpSzChanged
import io.suggest.sc.sjs.vm.res.FocusedRes
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sjs.common.vm.content.ClearT

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.08.15 16:04
  * Description: Аддон с кодом для сборки состояния закрытия focused-выдачи.
  *
  *
  * 2016.may.12: Изначально состояние занималось сокрытием от начала и до конца анимации.
  *
  * Но когда возникла необходимость ресайзить grid пока идёт сокрытие, было решено сделать
  * из состояния просто асинхронную анимацию + немедленный перескок на следующее состояние.
  *
  * Т.е. всё состояние описывается кодом в afterBecome(), а само состояние начинается и заканчивается мгновенно.
  */
trait Closing extends MouseMoving with OnGridBase with StateToUrlT {

  /** Трейт состояния закрытия focused-выдачи. */
  protected trait FocClosingStateT extends FsmEmptyReceiverState with FocMouseMovingStateT with IBackToGridState {

    override def afterBecome(): Unit = {
      super.afterBecome()

      // Запустить подготовку анимации сокрытия focused-выдачи.
      val fRootOpt = FRoot.find()
      for (froot <- fRootOpt) {
        froot.willAnimate()
      }

      val sd0 = _stateData

      // Посчитать заново размер grid-контейнера и сравнить с тем, что сохранен в grid-состоянии.
      val cwCm2 = sd0.grid.getGridContainerSz( sd0.common.screen )
      // Если параметры контейнера изменились...
      if (!sd0.grid.state.contSz.contains(cwCm2)) {
        // то нужно в фоне спровоцировать перерисовку плитки.
        _sendEvent( VpSzChanged )
      }

      // Затем запускать в фоне анимацию сокрытия focused-выдачи...
      for (froot <- fRootOpt) {
        // В фоне запланировать сокрытие focused-выдачи и прочие асинхронные действия.
        Future {
          froot.disappearTransition()
          DomQuick.setTimeout(SLIDE_ANIMATE_MS) { () =>
            froot.reset()
            FocusedRes.find()
              .foreach(ClearT.f)
          }
        }
      }

      // Выход из состояния закрытия не дожидаясь окончания анимации.
      val sd1 = sd0.copy(
        focused = None
      )
      become(_backToGridState, sd1)

      // Обновить текущее состояние в истории браузера.
      State2Url.pushCurrState()
    }

  }

}
