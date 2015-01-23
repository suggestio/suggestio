package models.adv

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
}


trait IExtWsActorArgs extends IExtActorArgs {

  /** Фьючерс списка целей для обработки. */
  def targetsFut: Future[ActorTargets_t]

}


/** Параметры, передаваемые от общего актора к service-актору. */
case class MExtAdvContext(
  qs        : MExtAdvQs,
  request   : RequestWithAdAndProducer[_],
  targetsFut: Future[ActorTargets_t]
) extends IExtWsActorArgs


/** Враппер над [[IExtActorArgs]]. */
trait IExtAdvArgsWrapperT extends IExtActorArgs {
  def _eaArgsUnderlying: IExtActorArgs

  override def qs = _eaArgsUnderlying.qs
  override def request = _eaArgsUnderlying.request
}


/** APIv2: Один подчинённый актор обслуживает только одну цель и обновляет GUI в рамках оной. */
trait IExtAdvTargetActorArgs extends IExtActorArgs {
  /** Цель, с которой нужно вести работу. */
  def target  : MExtTargetInfoFull

  /** Начальный контекст в рамках сервиса. */
  def mctx0   : MJsCtx
}
