package io.suggest.xadv.ext.js.runner.m.ex

import io.suggest.xadv.ext.js.runner.m.MErrorInfoT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.02.15 11:46
 * Description: Возникла проблема при инициализации API сервиса.
 */
case class ApiInitException(override val getCause: Throwable) extends RuntimeException with MErrorInfoT {
  override def msg: String = "e.adv.ext.api.init"
  override def args: Seq[String] = Seq(getCause.getClass.getName, getCause.getMessage)
}
