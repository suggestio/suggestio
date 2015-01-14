package models.adv

import akka.actor.ActorRef
import models.adv.js.ctx.JsCtx_t
import util.acl.RequestWithAdAndProducer

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.01.15 17:27
 * Description: Контейнер аргументов вызова service-актора, обслуживающего один сервис в
 * рамках системы акторов ext-adv.
 * @param out Актор-интерфейс к веб-сокету.
 * @param service Сервис, в рамках которого будет работать текущий service-актор.
 * @param targets0 Начальный список целей.
 * @param ctx0 Начальный контекст в рамках сервиса.
 * @param eactx Данные из контекста актора верхнего уровня.
 */
case class MExtAdvActorArgs(
  out       : ActorRef,
  service   : MExtService,
  targets0  : ActorTargets_t,
  ctx0      : JsCtx_t,
  eactx     : MExtAdvContext
)


/**
 * Параметры, передаваемые от общего актора к service-актору.
 * @param qs
 * @param request
 */
case class MExtAdvContext(
  qs      : MExtAdvQs,
  request : RequestWithAdAndProducer[_]
)