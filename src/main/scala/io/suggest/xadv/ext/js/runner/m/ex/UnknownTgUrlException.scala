package io.suggest.xadv.ext.js.runner.m.ex

import io.suggest.xadv.ext.js.runner.m.MErrorInfoT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.03.15 17:07
 * Description: Модель-исключение, возникающее когда нет возможности разобрать ссылку, которую указал юзер.
 */
class UnknownTgUrlException extends IllegalStateException with MErrorInfoT  {
  override def msg  = "e.adv.ext.unknown.tg.url"
  override def info = None
  override def args = Seq.empty
}
