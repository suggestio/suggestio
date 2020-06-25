package io.suggest.sc.c.dev

import diode.{ActionHandler, ActionResult, Dispatcher, Effect, ModelRW}
import io.suggest.event.DomEvents
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.sc.m.{OnlineInit, OnlineCheckConn, OnlineCheckConnRes}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sc.m.dev.{MOnLineInfo, MOnLineS}
import io.suggest.sjs.dom2.NetworkInformation
import org.scalajs.dom
import org.scalajs.dom.Event
import io.suggest.spa.DoNothing
import io.suggest.spa.DiodeUtil.Implicits._

import scala.scalajs.js
import japgolly.univeq._

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.06.2020 8:39
  * Description: Контроллер online-состояния. Подписка на события и мониторинг.
  */
class OnLineAh[M](
                   modelRW        : ModelRW[M, MOnLineS],
                   dispatcher     : Dispatcher,
                 )
  extends ActionHandler(modelRW)
  with Log
{

  private lazy val _listenerJsF: js.Function1[Event, Unit] = { _: dom.Event =>
    dispatcher.dispatch( OnlineCheckConn )
  }

  private def _listenEvents: List[String] =
    DomEvents.ONLINE :: DomEvents.OFFLINE :: Nil


  /** Эффект подписки или отписки на online/offline события. */
  private def _subscribeFx( subscribe: Boolean ): Effect = {
    Effect.action {
      val f: (String, _listenerJsF.type) => Unit = {
        if (subscribe) dom.window.addEventListener[Event](_, _)
        else dom.window.removeEventListener[Event](_, _)
      }

      for (eventType <- _listenEvents)
        f( eventType, _listenerJsF )

      DoNothing
    }
  }


  /** Поверхностное тестирование интернет-коннекшена. */
  private def _connectivityReadFx: Effect = {
    // API NetworkInformation может быть доступно или нет.
    Effect.action {
      val isOnline = Try( dom.window.navigator.onLine ) getOrElse true
      OnlineCheckConnRes(
        netInfo = NetworkInformation
          .safeGet()
          .toOption
          .flatten
          .fold(
            // Нет конкретной инфы по коннекшену
            MOnLineInfo(
              hasLink       = isOnline,
              isFastEnought = true,
            )
          ) { netInfo =>
            val effectiveTypeOpt = Try( netInfo.effectiveType.toOption )
              .toOption
              .flatten

            MOnLineInfo(
              hasLink = netInfo.`type`
                .toOption
                .map( _ !=* NetworkInformation.Types.NONE )
                .orElse( effectiveTypeOpt.map(_.nonEmpty) )
                .getOrElse( isOnline ),

              isFastEnought = netInfo.effectiveType
                .toOption
                .map { effTyp =>
                  !(effTyp contains NetworkInformation.EffectiveTypes.`2G`)
                }
                // TODO orElse протестировать downline/rtt, если доступны.
                .getOrElse( true ),
            )
          },
      )
    }
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Проверить текущее online-состояние.
    case OnlineCheckConn =>
      val v0 = value

      if (v0.state.isPending) {
        noChange
      } else {
        effectOnly( _connectivityReadFx )
      }


    // Прочитана инфа по соединению.
    case m: OnlineCheckConnRes =>
      val v0 = value

      if (v0.state contains[MOnLineInfo] m.netInfo) {
        noChange

      } else {
        val v2 = MOnLineS.state
          .modify( _.ready(m.netInfo) )(v0)
        updated(v2)
      }


    // Команда (пере)инициализации состояния online-контроллера.
    case m: OnlineInit =>
      val v0 = value

      if (v0.state.isPending) {
        logger.log( ErrorMsgs.REQUEST_STILL_IN_PROGRESS, msg = (m, v0.state) )
        noChange

      } else if (m.init && v0.state.isEmpty) {
        // Инициализация.
        // Нужно подписаться на события online/offline. Они доступны в браузера и в кордове с плагином network-information.
        val subscribeFx = _subscribeFx( true )
        val readFx = _connectivityReadFx
        val fx = subscribeFx + readFx

        val v2 = MOnLineS.state
          .modify( _.pending() )(v0)
        updatedSilent(v2, fx)

      } else if (!m.init && v0.state.nonEmpty) {
        // Де-инициализация
        val unSubscribeFx = _subscribeFx( false )
        val v2 = MOnLineS.empty
        updatedSilent( v2, unSubscribeFx )

      } else {
        logger.log( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = (m, v0) )
        noChange
      }

  }

}
