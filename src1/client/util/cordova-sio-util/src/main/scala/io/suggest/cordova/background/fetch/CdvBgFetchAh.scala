package io.suggest.cordova.background.fetch

import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import diode.{ActionHandler, ActionResult, Dispatcher, Effect, ModelRW}
import io.suggest.daemon.{DaemonSleepTimerFinish, DaemonSleepTimerSet}
import io.suggest.log.Log
import org.scalajs.dom
import cordova._
import cordova.plugins.background.fetch.CbfConfig
import io.suggest.msg.ErrorMsgs
import io.suggest.spa.DoNothing
import io.suggest.spa.DiodeUtil.Implicits._

import scala.concurrent.Future
import scala.scalajs.js.JavaScriptException
import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.05.2020 15:41
  * Description: diode-контроллер для взаимодействия с cordova-plugin-background-fetch API.
  */
class CdvBgFetchAh[M](
                       modelRW      : ModelRW[M, MCBgFetchS],
                       dispatcher   : Dispatcher,
                     )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  private def CBF = dom.window.BackgroundFetch

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал к управлению таймера фоновой задачи.
    case m: DaemonSleepTimerSet =>
      // Если не запущена система, то запустить с помощью configure().
      val v0 = value

      (for {

        // Если состояние надо изменить, то вернуть эффект.
        fx <- m.options.fold [Option[Effect]] {
          for (taskId <- v0.curTaskId) yield {
            // грохнуть текущий таймер
            Effect {
              CBF.stopTaskF( taskId )
                .flatMap( _ => CBF.stopF() )
                .transform { tryRes =>
                  val try2 = CbfSetEnabled( tryRes.map(_ => false) )
                  Success( try2 )
                }
            }
          }

        } { opts =>
          // Запустить таймер.
          val fx = Effect {
            Future {
              CBF.configure(
                callback = { taskId =>
                  dispatcher( CbfTaskCall(taskId) )
                },
                error = { e =>
                  dispatcher( CbfConfigFail( JavaScriptException(e) ) )
                },
                config = new CbfConfig {
                  override val requiresBatteryNotLow = true
                  override val requiredNetworkType   = CBF.NETWORK_TYPE_ANY
                  override val periodic              = true
                  override val minimumFetchInterval  = Math.max( 1, opts.every.toMinutes.toInt )
                }
              )
            }
              .transform { tryRes =>
                val s = CbfSetEnabled( tryRes.map(_ => true) )
                Success(s)
              }
          }
          Some(fx)
        }

      } yield {
        val v2 = (
          MCBgFetchS.isEnabled.modify( _.pending() ) andThen
          MCBgFetchS.opts.set( m.options )
        )(v0)
        updatedSilent( v2, fx )
      })
        .getOrElse( noChange )


    // Изменение состояние isEnabled.
    case m: CbfSetEnabled =>
      val v0 = value

      val v2F = m.isEnabledTry.fold(
        {ex =>
          logger.error( ErrorMsgs.DAEMON_BACKEND_FAILURE, ex, m )
          MCBgFetchS.isEnabled.modify(_.fail(ex))
        },
        {isBool =>
          MCBgFetchS.isEnabled.modify(_.ready(isBool))
        }
      )

      val v2 = v2F(v0)
      updatedSilent(v2)


    // Срабатывание таймера.
    case m: CbfTaskCall =>
      val v0 = value

      (for {
        opts <- v0.opts
      } yield {
        val v2Opt = Option.when(!(v0.curTaskId contains m.taskId)) {
          (MCBgFetchS.curTaskId set Some(m.taskId))(v0)
        }
        val fx = opts.onTime.toEffectPure
        ah.optionalResult( v2Opt, Some(fx), silent = true )
      })
        .getOrElse {
          // Какой-то неожиданный мусор пришёл.
          logger.warn( ErrorMsgs.SET_TIMER_ERROR, msg = (m, v0) )
          val fx = DaemonSleepTimerFinish.toEffectPure
          effectOnly(fx)
        }


    case e: CbfConfigFail =>
      logger.error( ErrorMsgs.DAEMON_BACKEND_FAILURE, msg = e )

      val fx = Effect {
        CBF.stopF()
          .transform( _ => Success(DoNothing) )
      }

      val v2 = MCBgFetchS.empty
      updatedSilent(v2, fx)


    // Команда к завершению одного срабатывания таймера.
    case DaemonSleepTimerFinish =>
      val v0 = value
      (for {
        taskId <- v0.curTaskId
      } yield {
        val fx = Effect {
          for {
            _ <- CBF.finishF( taskId )
          } yield {
            DoNothing
          }
        }
        val v2 = (MCBgFetchS.curTaskId set None)(v0)
        updatedSilent(v2, fx)
      })
        .getOrElse {
          println("nothing to do")
          noChange
        }


  }

}
