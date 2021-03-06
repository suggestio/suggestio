package io.suggest.cordova.background.mode

import cordova.Cordova
import cordova.plugins.background.mode.CordovaBgModeEvents
import diode._
import io.suggest.daemon.{BgModeDaemonInit, DaemonBgModeSet}
import io.suggest.i18n.MsgCodes
import io.suggest.msg.ErrorMsgs
import io.suggest.primo.Keep
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.log.Log
import io.suggest.sjs.JsApiUtil
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.{DAction, DoNothing}
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

import scala.collection.immutable.HashMap
import scala.scalajs.js
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.04.2020 14:54
  * Description: Контроллер демонизации для управления cordova-plugin-background-mode.
  */
object CordovaBgModeAh extends Log {

  final def CBGM = Cordova.plugins.backgroundMode


  /** Поддержкивается ли демонизация?
    *
    * @return true, если есть поддержка демонизации.
    */
  def canDaemonize(): Boolean =
    JsApiUtil.isDefinedSafe( CBGM )

}


final class CordovaBgModeAh[M](
                                modelRW       : ModelRW[M, MCBgModeDaemonS],
                                dispatcher    : Dispatcher,
                              )
  extends ActionHandler( modelRW )
{ ah =>

  import CordovaBgModeAh.{CBGM, logger}


  /** Патчинг списка листенеров.
    *
    * @param v0 Состояние контроллера.
    * @return Эффект, если требует что-либо изменить.
    */
  private def _ensureListenersFx( v0: MCBgModeDaemonS ): Option[Effect] = {
    val needListenersTypes = (for {
      initOpts <- v0.initOpts
    } yield {
      var evtTypesAcc = Set.empty[String] + CordovaBgModeEvents.ACTIVATE + CordovaBgModeEvents.DEACTIVATE

      if (initOpts.events.enabled.nonEmpty) {
        evtTypesAcc += CordovaBgModeEvents.ENABLE
        evtTypesAcc += CordovaBgModeEvents.DISABLE
      }

      if (initOpts.events.failure.nonEmpty)
        evtTypesAcc += CordovaBgModeEvents.FAILURE

      evtTypesAcc
    })
      // Если конфигурация не задана в состоянии, то удаляем все листенеры.
      .getOrElse( Set.empty )

    val haveListenerTypes = v0.listeners.keySet

    val removeTypes = haveListenerTypes -- needListenersTypes
    val addTypes = needListenersTypes -- haveListenerTypes

    Option.when( addTypes.nonEmpty || removeTypes.nonEmpty ) {
      Effect.action {
        val removedListeners = (for {
          rmEventType <- removeTypes.iterator
          fun <- v0.listeners.get( rmEventType ).iterator
          _ <- {
            val t = Try( CBGM.un(rmEventType, fun) )
            for (ex <- t.failed)
              logger.error( ErrorMsgs.EVENT_LISTENER_SUBSCRIBE_ERROR, ex, (MsgCodes.`Off`, rmEventType) )
            t.toOption
          }
        } yield {
          rmEventType
        })
          .to( List )

        val addListeners = (for {
          eventType <- addTypes.iterator
          fun: js.Function0[Unit] = { () =>
            dispatcher.dispatch( CbgmEvent(eventType) )
          }
          _ <- {
            val t = Try( CBGM.on(eventType, fun) )
            for (ex <- t.failed)
              logger.error( ErrorMsgs.EVENT_LISTENER_SUBSCRIBE_ERROR, ex, (MsgCodes.`On`, eventType) )
            t.toOption
          }
        } yield {
          eventType -> fun
        })
          .to( HashMap )

        CbgmUpdateListeners( addListeners, removedListeners )
      }
    }
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Команда к инициализации плагина демона.
    case m: BgModeDaemonInit =>
      val v0 = value

      var fxAcc = List.empty[Effect]

      // Если None => Some, то setDefault()
      // Если уже запущен демонизатор, то переконфигурировать.
      for (initOpts2 <- m.initOpts) {
        val defaults2 = CbgmUtil.notifyOpts2cbgm( initOpts2.notification )
        fxAcc ::= Effect.action {
          if (CBGM.isActive()) {
            // Нотификация уже отображается. Нужно configure() делать.
            CBGM.configure( defaults2 )
          } else {
            // Демон неактивен, выставить через setDefault().
            CBGM.setDefaults( defaults2 )
          }
          DoNothing
        }
      }

      val v2 = (MCBgModeDaemonS.initOpts replace m.initOpts)(v0)
      for ( listenersFx <- _ensureListenersFx(v2) )
        fxAcc ::= listenersFx

      ah.updatedSilentMaybeEffect( v2, fxAcc.mergeEffects )


    // Команда к демонизации или раздемонизации приложения.
    case m: DaemonBgModeSet =>
      val fx = Effect.action {
        val isEnabledNow = CBGM.isEnabled()
        //println(s"daemon work := " + isEnabledNow + " => " + m.isDaemon)
        if (isEnabledNow !=* m.isDaemon)
          CBGM.setEnabled( m.isDaemon )
        DoNothing
      }

      effectOnly(fx)


    // Обработка события со стороны cordova-плагина.
    case m: CbgmEvent =>
      val v0 = value

      (for {
        initOpts <- v0.initOpts
        e = initOpts.events
        fx: Effect <- m.eventType match {
          case CordovaBgModeEvents.ACTIVATE =>
            var fxsAcc = List.empty[Effect]
            for (actF <- e.activated)
              fxsAcc ::= actF( true ).toEffectPure

            // При активации демона, для доступа к GPS требуется выйти WebView-оптимизаций в ущерб оптимизациям.
            if (initOpts.descr.needGps) {
              fxsAcc ::= Effect.action {
                CBGM.disableWebViewOptimizations()
                DoNothing
              }
            }

            fxsAcc.mergeEffects

          case evtType =>
            val actionOpt: Option[DAction] = evtType match {
              case CordovaBgModeEvents.DEACTIVATE =>
                e.activated.map(_(false))
              case CordovaBgModeEvents.ENABLE =>
                e.enabled.map(_(true))
              case CordovaBgModeEvents.DISABLE =>
                e.enabled.map(_(false))
              case CordovaBgModeEvents.FAILURE =>
                logger.error( ErrorMsgs.DAEMON_BACKEND_FAILURE )
                // TODO А какие подробности проблемы, где они переданы? Надо покопаться в исходниках.
                e.failure
              case other =>
                logger.error( ErrorMsgs.DAEMON_BACKEND_FAILURE, msg = other )
                None
            }
            actionOpt.map( _.toEffectPure )
        }
      } yield {
        effectOnly( fx )
      })
        .getOrElse( noChange )


    // Подписаны/отписаны листенеры до bgMode-плагина.
    case m: CbgmUpdateListeners =>
      val v0 = value

      var listeners2 = v0.listeners

      if (m.remove.nonEmpty)
        listeners2 --= m.remove

      if (m.add.nonEmpty)
        listeners2 = listeners2.merged( m.add )( Keep.right )

      if (listeners2 ===* v0.listeners) {
        noChange
      } else {
        val v2 = (MCBgModeDaemonS.listeners replace listeners2)(v0)
        updatedSilent( v2 )
      }

  }

}
