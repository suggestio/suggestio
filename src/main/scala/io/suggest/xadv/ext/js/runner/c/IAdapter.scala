package io.suggest.xadv.ext.js.runner.c

import io.suggest.xadv.ext.js.runner.m.MJsCtx

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 14:55
 * Description: Интерфейс абстрактного js-клиента и его статической части.
 */

/** Интерфейс динамического клиента. */
trait IAdapter {

  /** Относится ли указанный домен к текущему клиенту? */
  def isMyDomain(domain: String): Boolean

  /** Запуск инициализации клиента. Добавляется необходимый js на страницу,  */
  def ensureReady(mctx0: MJsCtx): Future[MJsCtx]

}
