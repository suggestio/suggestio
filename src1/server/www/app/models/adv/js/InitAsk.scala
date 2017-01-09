package models.adv.js

import models.adv.js.ctx.MJsCtx

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.03.15 13:36
 * Description: Запрос инициализации системы adv-ext sjs-runner с обратной связью.
 */
case class InitAsk(mctx0: MJsCtx) extends IJsonActionCmd with IJsonActionCtxPatcher  {

  /** Обратного адреса нет, т.к. актор-слушатель веб-сокета и является получаетелем. */
  override def replyTo = None

  /** Действие, которое должно быть в контексте. */
  override def action = MJsActions.Init

  /** Режим отправки значения не имеет, поэтому и на всякий тут Async. */
  override def sendMode = CmdSendModes.Async

}
