package io.suggest.sc.inx.c

import diode._
import diode.data.{PendingBase, Pot}
import io.suggest.sc.Sc3Api
import io.suggest.sc.index.{MSc3IndexResp, MScIndexArgs}
import io.suggest.sc.inx.m.{GetIndex, HandleScResp}
import io.suggest.sc.resp.MScRespActionTypes
import io.suggest.sc.root.m.MScRoot

import scala.util.Success
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.07.17 18:39
  * Description: Контроллер index'а выдачи.
  */
class IndexAh[M](
                  api       : IIndexApi,
                  modelRW   : ModelRW[M, Pot[MSc3IndexResp]],
                  stateRO   : ModelRO[MScRoot]
                )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Команда запроса и получения индекса выдачи с сервера для текущего состояния.
    case m: GetIndex =>
      val root = stateRO()

      val args = MScIndexArgs(
        nodeId      = m.rcvrId,
        locEnv      = root.locEnv,
        screen      = Some( root.index.state.screen ),
        withWelcome = true,
        apiVsn      = Sc3Api.API_VSN
      )

      val ts = System.currentTimeMillis()

      val fx = Effect {
        api
          .getIndex(args)
          .transform { tryRes =>
            Success(HandleScResp(ts, tryRes))
          }
      }
      val v2 = value.pending(ts)
      updated( v2, fx )


    // Поступление ответа сервера, который ожидается
    case m: HandleScResp if value.isPending && value.asInstanceOf[PendingBase].startTime == m.timestampMs =>
      val v0 = value

      val v2: Pot[MSc3IndexResp] = m.tryResp.fold(
        v0.fail,
        {scResp =>
          scResp.respActions
            .find(_.acType == MScRespActionTypes.Index)
            .flatMap( _.index )
            .fold { v0.fail( new NoSuchElementException("index") ) } (v0.ready)
        }
      )

      updated( v2 )

  }

}
