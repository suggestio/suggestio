package io.suggest.sc.c.inx

import diode.{ActionHandler, ActionResult, Effect, ModelRO, ModelRW}
import io.suggest.sc.m.{SaveConf, SetDebug}
import io.suggest.sc.sc3.{MSc3Conf, MSc3Init}
import io.suggest.sc.u.Sc3ConfUtil
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.DoNothing
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.07.2020 17:29
  * Description: Контроллер конфига.
  */
class ScConfAh[M](
                   modelRW: ModelRW[M, MSc3Conf],
                   scInitRO: ModelRO[MSc3Init],
                 )
  extends ActionHandler( modelRW )
{ ah =>


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    case m: SetDebug =>
      val v0 = value
      if (v0.debug ==* m.isDebug) {
        noChange
      } else {
        val v2 = (MSc3Conf.debug set m.isDebug)(v0)
        // Сохранить конфиг, т.к. debug-флаг сохраняется в постоянно-хранимый конфиг:
        val fx = SaveConf().toEffectPure
        updated( v2, fx )
      }


    case m: SaveConf =>
      val v0 = value

      var v2 = v0

      // JSON-карта ресиверов:
      for (confUpdate <- m.update) {
        for {
          rcvrsMapUrlArgs2 <- confUpdate.rcvrsMap
          if rcvrsMapUrlArgs2 !=* v0.rcvrsMapUrl
        } {
          v2 = (MSc3Conf.rcvrsMapUrl set rcvrsMapUrlArgs2)(v2)
          // TODO Организовать эффект или таймер для обновления карты. Таймер нужен, чтобы карта не обновлялась слишком часто.
        }
      }

      // Если конфиг изменился, и у нас тут постоянная установка, то надо сохранить новый конфиг в состояние.
      if (m.update.nonEmpty && (v0 ===* v2)) {
        noChange

      } else {
        // Конфиг изменился. Залить новый конфиг в состояние, запустить обновление и сохранение конфига, если необходимо.
        v2 = Sc3ConfUtil.prepareSave( v2 )

        // Попытаться сохранить конфигурацию в постоянную модель:
        val fx = Effect.action {
          val init2 = MSc3Init.conf.set( v2 )( scInitRO.value )
          Sc3ConfUtil.saveInitIfPossible( init2 )
          DoNothing
        }

        ah.updateMaybeSilentFx(
          silent = m.update.isEmpty,
        )( v2, fx )
      }

  }

}
