package models.adv

import akka.actor.{ActorRef, Props}
import io.suggest.ext.svc.MExtService
import models.adv.js.ctx.MJsCtx
import models.mctx.Context
import models.req.IAdProdReq
import play.api.libs.oauth.RequestToken

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
  def request : IAdProdReq[_]

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
  override val qs         : MExtAdvQs,
  override val request    : IAdProdReq[_],
  override val targetsFut : Future[ActorTargets_t]
)(
  implicit val ctx: Context
)
  extends IExtWsActorArgs


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
trait IExtAdvServiceActorArgs extends IExtActorArgs with WsMediatorRef {

  /** Текущий сервис, в рамках которого надо делать дела. */
  def service: MExtService

  /** Таргеты, относящиеся к сервису. */
  def targets: Seq[MExtTargetInfoFull]

  /** Начальный контекст. */
  def mctx0: MJsCtx
}


/** Аргументы для target-актора, который обслуживает только одну jsapi-цель. */
trait IExtAdvTargetActorArgs extends IExtActorArgs with WsMediatorRef with MCtx0 {
  /** Цель, с которой нужно вести работу. */
  def target  : MExtTargetInfoFull
}


/** Аргументы для target-актора, который обслуживает одну oauth1-цель. */
trait IOAuth1AdvTargetActorArgs extends IExtAdvTargetActorArgs {
  /** Токен для доступа. */
  def accessToken: RequestToken
}

/** Реализация модели [[IOAuth1AdvTargetActorArgs]]. */
case class MOAuth1AdvTargetActorArgs(
  override val mctx0              : MJsCtx,
  override val target             : MExtTargetInfoFull,
  override val _eaArgsUnderlying  : IExtActorArgs,
  override val wsMediatorRef      : ActorRef,
  override val accessToken        : RequestToken
)
  extends IOAuth1AdvTargetActorArgs
  with IExtAdvArgsWrapperT


/** Подчинённый актор уведомляет медиатора, что нужно подрядить к работе указанных акторов. */
case class AddActors(
  actors    : Iterable[Props]
)

