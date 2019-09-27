package io.suggest.ad.edit.c

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.ad.edit.m.pop.MAePopupsS
import io.suggest.ad.edit.m.{MAdEditFormConf, MAeRoot, TouchDevSet}
import io.suggest.conf.ConfConst
import io.suggest.kv.MKvStorage
import io.suggest.lk.m.{CloseAllPopups, DocBodyClick, ErrorPopupCloseClick}
import io.suggest.sjs.dom.DomQuick
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.DoNothing
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.09.17 11:21
  * Description: Хвостовой ActionHandler, т.е. перехватывает всякие необязательные к обработке экшены.
  */
class TailAh[M](
                 modelRW: ModelRW[M, MAeRoot]
               )
  extends ActionHandler(modelRW)
{ ah =>

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

      if (m.isTouchDev ==* v0.conf.touchDev) {
        println(m, "noChange")
        noChange

      } else {
        println(m)
        // Записать новое значение в LocalStorage
        val localStorFx = Effect.action {
          val mkv = MKvStorage(ConfConst.IS_TOUCH_DEV, m.isTouchDev)
          MKvStorage.save( mkv )
          DoNothing
        }

        // Подавить дальнейшие сообщения о необходимости переключения react-dnd backend'а путём смены backend на лету,
        // TODO которое не работает даже при полном перерендере всей формы. Поэтому будем перезагружать страницу после сохранения...
        val v2 = (lens set m.isTouchDev)(v0)

        // Организовать перезагрузку страницы после сохранения флага в localStorage.
        val rebootFx = Effect.action {
          DomQuick.reloadPage()
          DoNothing
        }

        val fx = localStorFx >> rebootFx
        updated(v2, fx)
      }

  }

}
