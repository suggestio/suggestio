package io.suggest.xadv.ext.js.runner.m.ex

import io.suggest.xadv.ext.js.runner.m.MErrorInfoT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.02.15 16:17
 * Description: При проблеме на странице, подсистема может сообщить об этом серверу.
 */
case class DomUpdateException(underlying: Throwable = null) extends IllegalStateException with MErrorInfoT {
  override def msg: String = "e.dom.update"
  override def args: Seq[Any] = {
    if (underlying != null)
      Seq(underlying.getClass.getName, underlying.getMessage)
    else
      Seq.empty
  }

  override def getCause: Throwable = {
    if(underlying == null) super.getCause else underlying
  }
}
