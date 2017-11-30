package io.suggest.sc.inx.c

import diode._
import io.suggest.react.ReactDiodeUtil.PotOpsExt
import io.suggest.sc.grid.m.GridLoadAds
import io.suggest.sc.ScConstants
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.inx.m.{GetIndex, HandleIndexResp, MScIndex, MWelcomeState}
import io.suggest.sc.resp.MScRespActionTypes
import io.suggest.sc.root.m.MScRoot

import scala.util.Success
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.WarnMsgs
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.07.17 18:39
  * Description: Контроллер index'а выдачи.
  */
class IndexAh[M](
                  api       : IIndexApi,
                  modelRW   : ModelRW[M, MScIndex],
                  stateRO   : ModelRO[MScRoot]
                )
  extends ActionHandler( modelRW )
  with Log
{

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    // Команда запроса и получения индекса выдачи с сервера для текущего состояния.
    case m: GetIndex =>
      val v0 = value
      val ts = System.currentTimeMillis()

      val fx = Effect {
        val root = stateRO()
        val args = MScIndexArgs(
          nodeId      = v0.state.currRcvrId,
          locEnv      = root.locEnv,
          screen      = Some( root.dev.screen.screen ),
          withWelcome = m.withWelcome
        )

        api
          .getIndex(args)
          .transform { tryRes =>
            Success(HandleIndexResp(tryRes, Some(ts), reason = None))
          }
      }

      val v2 = v0.withResp(
        v0.resp.pending(ts)
      )
      updated( v2, fx )


    // Поступление ответа сервера, который ожидается
    case m: HandleIndexResp =>
      val v0 = value

      if (!m.reqTimestamp.fold(true)(v0.resp.isPendingWithStartTime)) {
        // Ответ сервера пришёл поздновато: уже другой запрос ожидается.
        LOG.log( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
        noChange

      } else {
        // Запихать ответ в состояние.
        val v1 = m.tryResp.fold(
          { ex =>
            v0.withResp( v0.resp.fail(ex) )
          },
          {scResp =>
            scResp.respActions
              .find(_.acType ==* MScRespActionTypes.Index)
              .flatMap( _.index )
              .fold {
                v0.withResp(
                  v0.resp.fail( new NoSuchElementException("index") )
                )
              } { inx =>
                // TODO Сравнивать полученный index с текущим состоянием. Может быть ничего сохранять не надо?
                v0.copy(
                  resp = v0.resp.ready(inx),
                  state = v0.state
                    .withRcvrNodeId( inx.nodeId.toList ),
                  search = inx
                    .geoPoint
                    .filter { mgp =>
                      !(v0.search.mapInit.state.center ~= mgp)
                    }
                    .fold(v0.search) { mgp =>
                      v0.search.withMapInit(
                        v0.search.mapInit.withState(
                          v0.search.mapInit.state
                            .withCenterInit( mgp )
                            .withCenterReal(None)
                        )
                      )
                    }
                )
              }
          }
        )
        //val v1 = v0.withResp( resp2 )

        // Нужно огранизовать инициализацию плитки карточек. Для этого нужен эффект:
        val gridInitFx = Effect.action {
          GridLoadAds(clean = true, ignorePending = true)
        }

        // Инициализация приветствия. Подготовить состояние welcome.
        val mWcSFutOpt = for {
          resp <- v1.resp.toOption
          if !v1.resp.isFailed      // Всякие FailingStale содержат старый ответ и новую ошибку, их надо отсеять.
          _    <- resp.welcome
        } yield {
          val tstamp = System.currentTimeMillis()

          // Собрать функцию для запуска неотменяемого таймера.
          // Функция возвращает фьючерс, который исполнится через ~секунду.
          val tpFx = Effect {
            WelcomeUtil.timeout( ScConstants.Welcome.HIDE_TIMEOUT_MS, tstamp )
          }

          // Собрать начальное состояние приветствия:
          val mWcS = MWelcomeState(
            isHiding    = false,
            timerTstamp = tstamp
          )

          (tpFx, mWcS)
        }
        val v2 = v1.withWelcome( mWcSFutOpt.map(_._2) )

        // Объединить эффекты плитки и приветствия воедино:
        val allFxs = mWcSFutOpt.fold[Effect](gridInitFx) { gridInitFx + _._1 }

        updated(v2, allFxs)
      }

  }

}
