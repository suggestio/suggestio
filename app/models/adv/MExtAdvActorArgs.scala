package models.adv

import akka.actor.ActorRef
import models.adv.js.ctx.{MJsCtx, JsCtx_t}
import util.acl.RequestWithAdAndProducer

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.01.15 17:27
 * Description: Контейнеры аргументов вызова service-актора, обслуживающего один сервис в
 * рамках системы акторов ext-adv.
 */

/** Аргументы для главного актора ext.adv. */
trait MExtAdvArgsT {
  /** Распарсенные qs-аргументы запроса. */
  def qs      : MExtAdvQs

  /** Экземпляр распарсенного http-реквеста. Содержит в себе продьюсера и карточку. */
  def request : RequestWithAdAndProducer[_]
}


/** Параметры, передаваемые от общего актора к service-актору. */
case class MExtAdvContext(
  qs      : MExtAdvQs,
  request : RequestWithAdAndProducer[_]
) extends MExtAdvArgsT


/** Враппер над [[MExtAdvArgsT]]. */
trait MExtAdvArgsWrapperT extends MExtAdvArgsT {
  def _eaArgsUnderlying: MExtAdvArgsT

  override def qs = _eaArgsUnderlying.qs
  override def request = _eaArgsUnderlying.request
}


/** Аргументы, передаваемые от общего актора внешнего размещения к актору конкретного сервиса. */
trait MExtServiceAdvArgsT extends MExtAdvArgsT {
  /** Актор-интерфейс к веб-сокету. */
  def out       : ActorRef

  /** Сервис, в рамках которого будет работать текущий service-актор. */
  def service   : MExtService

  /** Начальный список целей. */
  def targets0  : ActorTargets_t

  /** Начальный контекст в рамках сервиса. */
  def mctx0      : MJsCtx
}

