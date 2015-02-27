package io.suggest.xadv.ext.js.runner.m.ex

import io.suggest.xadv.ext.js.runner.m.MErrorInfoT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.02.15 11:12
 * Description: Юзер отказался логинится на внешнем сервисе.
 */
case class LoginCancelledException() extends Exception with MErrorInfoT {
  override def msg: String = "e.ext.adv.unathorized"
  override def args: Seq[String] = Seq.empty
}
