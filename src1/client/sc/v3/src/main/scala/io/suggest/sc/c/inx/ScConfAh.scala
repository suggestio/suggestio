package io.suggest.sc.c.inx

import diode.{ActionHandler, ActionResult, Effect, ModelRO, ModelRW}
import io.suggest.maps.m.RcvrMarkersInit
import io.suggest.sc.m.{SaveConf, SetDebug}
import io.suggest.sc.sc3.{MSc3Conf, MSc3Init}
import io.suggest.sc.u.Sc3ConfUtil
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.DoNothing
import io.suggest.spa.delay.DelayAction
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

    case m: SaveConf =>
      val v0 = value

      var modsAcc = List.empty[MSc3Conf => MSc3Conf]
      var fxAcc = List.empty[Effect]

      for (confUpdate <- m.update) {
        // Отработать rcvrMapUrl:
        for {
          rcvrsMapUrlArgs2 <- confUpdate.rcvrsMap
          if rcvrsMapUrlArgs2 !=* v0.rcvrsMapUrl
        } {
          // Закинуть новый URL ресиверов в состояние:
          modsAcc ::= (MSc3Conf.rcvrsMapUrl set rcvrsMapUrlArgs2)

          // Организовать эффект или таймер для обновления карты. Таймер нужен, чтобы карта не обновлялась слишком часто:
          fxAcc ::= Effect.action {
            DelayAction(
              key     = RcvrMarkersInit.getClass.getSimpleName,
              delayMs = 3000,
              fx      = Effect.action( RcvrMarkersInit() ),
            )
          }
        }

        // Сюда можно добавить дополнительные обработчики данных из confUpdate.
      }

      // Если конфиг изменился, и у нас тут постоянная установка, то надо сохранить новый конфиг в состояние.
      val v2Opt = for {
        modF <- modsAcc.reduceOption(_ andThen _)
      } yield {
        Sc3ConfUtil.prepareSave( modF(v0) )
      }

      // Попытаться сохранить конфигурацию в постоянную модель:
      for (v2 <- v2Opt) {
        fxAcc ::= Effect.action {
          val init2 = MSc3Init.conf.set( v2 )( scInitRO.value )
          Sc3ConfUtil.saveInitIfPossible( init2 )
          DoNothing
        }
      }

      ah.optionalResult( v2Opt, fxAcc.mergeEffects, silent = true )


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

  }

}
