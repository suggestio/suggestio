package io.suggest.ad.edit.c

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.ad.edit.m.pop.MAePopupsS
import io.suggest.ad.edit.m.{MAdEditFormConf, MAeRoot, TouchDevSet}
import io.suggest.lk.m.{CloseAllPopups, DocBodyClick, ErrorPopupCloseClick}
import io.suggest.sjs.dom.DomQuick
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.09.17 11:21
  * Description: Хвостовой ActionHandler, т.е. перехватывает всякие необязательные к обработке экшены.
  */
class TailAh[M](modelRW: ModelRW[M, MAeRoot]) extends ActionHandler(modelRW) {

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Перехват ненужного события клика в документе.
    case DocBodyClick =>
      noChange

    // Клик по кнопке закрытия попапа ошибки.
    case ErrorPopupCloseClick =>
      val v0 = value
      val v2 = MAeRoot.popups
        .composeLens(MAePopupsS.error)
        .set(None)(v0)
      updated( v2 )

    // Закрытие всех попапов.
    case CloseAllPopups =>
      val v0 = value
      val v2 = MAeRoot.popups.set( MAePopupsS.empty )(v0)
      updated( v2 )


    // Обнаружилось touch-устройство, использующее редактор.
    case m: TouchDevSet =>
      val v0 = value

      val lens = MAeRoot.conf
        .composeLens( MAdEditFormConf.touchDev )

      val currIsTouchDevOpt = lens.get(v0)
      if (currIsTouchDevOpt.isEmpty) {
        // Второй шаг изменения состояния рендера. Выставить запрашиваемое значение.
        val v2 = (lens set m.isTouchDev)(v0)
        updated(v2)

      } else if (m.isTouchDev ==* v0.conf.touchDev) {
        noChange

      } else {
        // Тут рендер в 2 шага: сначала None для сброса всего рендера и пауза + полный пере-рендер с новым backend'ом
        println(m)
        val v2 = (lens set None)(v0)
        val fx = Effect {
          DomQuick
            .timeoutPromiseT(200)(m)
            .fut
        }
        updated(v2, fx)
      }

  }

}
