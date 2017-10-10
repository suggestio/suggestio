package util.ws

import akka.actor.{ActorRef, Props}
import javax.inject.Inject

import com.google.inject.assistedinject.Assisted
import io.suggest.ctx.MCtxId
import io.suggest.util.logs.MacroLogsImplLazy

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.11.14 18:32
 * Description: Актор, обслуживающий унифицированный общий WebSocket-канал, т.е. ws-channel.
 */
class WsChannelActors @Inject() (
                                  factory: IWsChannelActorFactory
                                ) {

  def props(args: MWsChannelActorArgs) = {
    Props( factory.create(args) )
  }

}


/** Интерфейс Guice-сборщика инстансов [[WsChannelActor]]. */
trait IWsChannelActorFactory {
  def create(args: MWsChannelActorArgs): WsChannelActor
}


/**
 * Реализация актора для редактора в личном кабинете.
 * @param args Контейнер передаваемых параметров запуска актора.
 */
case class WsChannelActor @Inject() (
                                      @Assisted args                  : MWsChannelActorArgs,
                                      override val wsDispatcherActors : WsDispatcherActors
)
  extends WsActorDummy
  with SubscribeToWsDispatcher
  //with ColorDetectedWsNotifyActor // TODO Написать и заинклюдить сюда трейт связи с MainColorDetector'ом.
  with MacroLogsImplLazy
{

  import LOGGER._

  override final def wsId = args.ctxId.toString

  override def postStop(): Unit = {
    super.postStop()
    trace(s"Stopping actor for wsId=$wsId and out=${args.out}")
  }

}


/** Модель-контейнер аргументов для запуска актора.
  *
  * @param out Ref актора, скрывающего фактический веб-сокет.
  * @param ctxId Context id.
  */
case class MWsChannelActorArgs(
                                out       : ActorRef,
                                ctxId     : MCtxId
                              )

