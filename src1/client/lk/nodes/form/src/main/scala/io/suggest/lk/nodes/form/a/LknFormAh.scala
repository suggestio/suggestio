package io.suggest.lk.nodes.form.a

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.lk.nodes.MLknConf
import io.suggest.lk.nodes.form.m.{MLkNodesRoot, SetAd, TreeInit}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.09.2020 21:21
  * Description: Контроллер lkn-формы верхнего уровня. Отрабатывает глобальные операции.
  */
class LknFormAh[M](
                    modelRW: ModelRW[M, MLkNodesRoot]
                  )
  extends ActionHandler(modelRW)
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Смена режима работы формы или смена текущей карточки.
    case m: SetAd =>
      val v0 = value
      if (v0.conf.adIdOpt ==* m.adId) {
        noChange

      } else {
        val v2 = MLkNodesRoot.conf
          .composeLens( MLknConf.adIdOpt )
          .set( m.adId )(v0)

        // Запустить пере-инициализацию дерева:
        val fx = TreeInit().toEffectPure
        updated(v2, fx)
      }


  }

}
