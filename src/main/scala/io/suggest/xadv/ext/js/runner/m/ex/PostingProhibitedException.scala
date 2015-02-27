package io.suggest.xadv.ext.js.runner.m.ex

import io.suggest.xadv.ext.js.runner.m.MErrorInfoT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.02.15 18:33
 * Description: Запрещен постинг на указанную страницу-цель на стороне сервиса.
 */
case class PostingProhibitedException(tgTitle: String) extends RuntimeException with MErrorInfoT {
  override def msg  = "e.ext.adv.permissions.group"
  override def args = Seq(tgTitle)
}
