package io.suggest.xadv.ext.js.runner.m

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 13:51
 * Description: Формат ответа, отправляемого в websocket.
 */
case class MReply(
  mctx      : MJsCtx,
  replyTo   : Option[String]
)
