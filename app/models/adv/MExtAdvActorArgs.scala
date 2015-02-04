package models.adv

import akka.actor.{Props, ActorRef}
import models.Context
import models.adv.js.ctx.MJsCtx
import util.acl.RequestWithAdAndProducer

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.01.15 17:27
 * Description: Контейнеры аргументов вызова service-актора, обслуживающего один сервис в
 * рамках системы акторов ext-adv.
 */

/** Аргументы для главного актора ext.adv. */
trait IExtActorArgs {
  /** Распарсенные qs-аргументы запроса. */
  def qs      : MExtAdvQs

  /** Экземпляр распарсенного http-реквеста. Содержит в себе продьюсера и карточку. */
  def request : RequestWithAdAndProducer[_]

  /** Для рендера шаблонов, акторам нужен экземпляр Context, который изготовит для них контроллер. */
  implicit def ctx: Context
}


trait IExtWsActorArgs extends IExtActorArgs {

  /** Фьючерс списка целей для обработки. */
  def targetsFut: Future[ActorTargets_t]

}


/** Аргументы запуска актора, разговаривающего с ws. Этот актор будет выступать посредником (медиатором) между
  * ws и акторами, исполняющими конкретные задачи. */
case class MExtWsActorArgs(
  qs        : MExtAdvQs,
  request   : RequestWithAdAndProducer[_],
  targetsFut: Future[ActorTargets_t]
)(implicit val ctx: Context) extends IExtWsActorArgs


/** Враппер над [[IExtActorArgs]]. */
trait IExtAdvArgsWrapperT extends IExtActorArgs {
  def _eaArgsUnderlying: IExtActorArgs

  override def qs = _eaArgsUnderlying.qs
  override def request = _eaArgsUnderlying.request
  override implicit def ctx: Context = _eaArgsUnderlying.ctx
}


/** Интерфейс для поля mctx0 в аргументах запуска service-актора. */
trait MCtx0 {
  /** Начальный контекст. */
  def mctx0   : MJsCtx
}


/** Куда слать инфу, подлежащую к отправке в ws? */
trait WsMediatorRef {
  /** ActorRef актора-посредника между этим актором и ws. */
  def wsMediatorRef: ActorRef
}


/** Аргументы для service-актора. */
trait IExtAdvServiceActorArgs extends IExtActorArgs with WsMediatorRef with MCtx0 {

  /** Текущий сервис, в рамках которого надо делать дела. */
  def service: MExtService

  /** Таргеты, относящиеся к сервису. */
  def targets: Seq[MExtTargetInfoFull]
}


/** APIv2: Один подчинённый актор обслуживает только одну цель и обновляет GUI в рамках оной. */
trait IExtAdvTargetActorArgs extends IExtActorArgs with WsMediatorRef with MCtx0 {
  /** Цель, с которой нужно вести работу. */
  def target  : MExtTargetInfoFull
}



/** Подчинённый актор уведомляет медиатора, что нужно подрядить к работе указанных акторов. */
case class AddActors(
  actors    : Iterable[Props]
)

