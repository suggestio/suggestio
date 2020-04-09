package io.suggest.lk.c

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.conf.ConfConst
import io.suggest.kv.MKvStorage
import io.suggest.lk.m.TouchDevSet
import io.suggest.sjs.dom2.DomQuick
import io.suggest.spa.DoNothing
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.09.2019 13:40
  * Description: Контроллер для связки с [[io.suggest.lk.r.TouchSwitchR]].
  */
class IsTouchDevSwitchAh[M](
                             modelRW: ModelRW[M, Boolean],
                           )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Обнаружилось touch-устройство, использующее редактор.
    case m: TouchDevSet =>
      val v0 = value

      if (m.isTouchDev ==* v0) {
        noChange

      } else {
        // Записать новое значение в LocalStorage
        val localStorFx = Effect.action {
          val mkv = MKvStorage(ConfConst.IS_TOUCH_DEV, m.isTouchDev)
          MKvStorage.save( mkv )
          DoNothing
        }

        // Подавить дальнейшие сообщения о необходимости переключения react-dnd backend'а путём смены backend на лету,
        // TODO которое не работает даже при полном перерендере всей формы. Поэтому будем перезагружать страницу после сохранения...
        val v2 = m.isTouchDev

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
