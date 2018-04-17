package io.suggest.adn.edit.c

import diode._
import io.suggest.adn.edit.api.ILkAdnEditApi
import io.suggest.adn.edit.m.{MAdnEditInternals, MLkAdnEditRoot, SaveResp}
import io.suggest.lk.m.Save
import io.suggest.msg.WarnMsgs
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.04.18 21:35
  * Description: Контроллер внутренних фунцийи редактора ADN-узла.
  */
class InternalsAh[M](
                      api    : ILkAdnEditApi,
                      mrootRO: ModelRO[MLkAdnEditRoot],
                      modelRW: ModelRW[M, MAdnEditInternals]
                    )
  extends ActionHandler( modelRW )
  with Log
{

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    case m @ Save =>
      val v0 = value
      if (v0.saving.isPending) {
        LOG.log( WarnMsgs.REQUEST_STILL_IN_PROGRESS, msg = m )
        noChange
      } else {
        val fx = Effect {
          api
            .save( mrootRO.value.toForm )
            .transform { tryRes =>
              Success( SaveResp(tryRes) )
            }
        }

        val v2 = v0.withSaving(
          v0.saving.pending()
        )

        updated(v2, fx)
      }


    // Результат запроса сохранения.
    case m: SaveResp =>
      val v0 = value
      m.tryResp.fold(
        { ex =>
          val v2 = v0.withSaving(
            v0.saving.fail(ex)
          )
          updated(v2)
        },
        {form2 =>
          println("TODO " + m)
          noChange
        }
      )

  }

}
