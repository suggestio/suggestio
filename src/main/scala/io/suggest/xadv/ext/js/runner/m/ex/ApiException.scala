package io.suggest.xadv.ext.js.runner.m.ex

import io.suggest.xadv.ext.js.runner.m.MErrorInfoT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.02.15 17:34
 * Description: Ошибка взаимодействия с API сервиса.
 */
case class ApiException(from: String, cause: Throwable = null) extends Exception with MErrorInfoT {
  override val getCause: Throwable = {
    if (cause == null) super.getCause else cause
  }
  override def msg: String = "e.adv.ext.api"
  override def args: Seq[String] = Seq(from, getCause.getClass.getName, getCause.getMessage)
}
