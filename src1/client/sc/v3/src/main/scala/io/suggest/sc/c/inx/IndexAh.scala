package io.suggest.sc.c.inx

import diode._
import io.suggest.react.ReactDiodeUtil
import io.suggest.react.ReactDiodeUtil.PotOpsExt
import io.suggest.sc.ScConstants
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.m.grid.GridLoadAds
import io.suggest.sc.m.inx.{GetIndex, HandleIndexResp, MScIndex, MWelcomeState}
import io.suggest.sc.m.search.GetMoreTags
import io.suggest.sc.m.{MScRoot, ResetUrlRoute}
import io.suggest.sc.resp.MScRespActionTypes
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.{ErrorMsgs, WarnMsgs}
import japgolly.univeq._

import scala.util.Success

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
      // Оптимизация: Используем updatedSilent, т.к. сейчас визуально нигде не отражается получение индекса.
      updatedSilent( v2, fx )


    // Поступление ответа сервера, который ожидается
    case m: HandleIndexResp =>
      val v0 = value

      if (!m.reqTimestamp.fold(true)(v0.resp.isPendingWithStartTime)) {
        // Ответ сервера пришёл поздновато: уже другой запрос ожидается.
        LOG.log( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
        noChange

      } else {
        // Запихать ответ в состояние.
        m.tryResp
          .toEither
          .right.flatMap { scResp =>
            scResp.respActions
              .find(_.acType ==* MScRespActionTypes.Index)
              .flatMap( _.index )
              .toRight( new NoSuchElementException("index") )
          }
          .fold(
            // Ошибка получения индекса с сервера.
            {ex =>
              LOG.error( ErrorMsgs.GET_NODE_INDEX_FAILED, ex, msg = m )
              val v2 = v0.withResp(
                v0.resp.fail(ex)
              )
              updated( v2 )
            },
            // Индекс получен.
            {inx =>
              // TODO Сравнивать полученный index с текущим состоянием. Может быть ничего сохранять не надо?
              val v1 = v0.copy(
                resp = v0.resp.ready(inx),
                state = v0.state
                  .withRcvrNodeId( inx.nodeId.toList ),
                search = {
                  val s0 = v0.search
                  // Выставить полученную с сервера геоточку как текущую.
                  val s1 = inx.geoPoint
                    .filter { mgp =>
                      !(v0.search.mapInit.state.center ~= mgp)
                    }
                    .fold(s0) { mgp =>
                      s0.withMapInit(
                        s0.mapInit.withState(
                          s0.mapInit.state.copy(
                            centerInit = mgp,
                            centerReal = None
                            // TODO выставлять ли zoom?
                          )
                        )
                      )
                    }
                  // Возможный сброс состояния тегов
                  s1.maybeResetTags
                }
              )

              // Сайд-эффекты закидываются в этот аккамулятор:
              var fxsAcc = List.empty[Effect]

              // Если вкладка с тегами видна, то запустить получение тегов в фоне.
              if (v1.search.isTagsVisible) {
                fxsAcc ::= Effect.action {
                  GetMoreTags(clear = true)
                }
              }

              // Возможно, нужно организовать обновление URL в связи с обновлением состояния узла.
              fxsAcc ::= Effect.action( ResetUrlRoute )

              // Нужно огранизовать инициализацию плитки карточек. Для этого нужен эффект:
              fxsAcc ::= Effect.action {
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
              for (mwc <- mWcSFutOpt)
                fxsAcc ::= mwc._1

              val allFxs = ReactDiodeUtil.mergeEffectsSet( fxsAcc ).get

              updated(v2, allFxs)
            }
          )
      }

  }

}
