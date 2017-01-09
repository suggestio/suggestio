package io.suggest.xadv.ext.js.runner.c

import io.suggest.xadv.ext.js.runner.m.MJsCtx

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.03.15 22:27
 * Description: Контекст экшена: интерфейс и дефолтовая реализация.
 */
trait IActionContext {

  /** Доступ к контексту всего приложения. */
  def app: IAppContext

  /** Доступ к данным инструкции. */
  def mctx0: MJsCtx

}


/** Дефолтовая реализация контекста действия. */
case class ActionContextImpl(
  app   : IAppContext,
  mctx0 : MJsCtx
) extends IActionContext

