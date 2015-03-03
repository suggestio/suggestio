package io.suggest.xadv.ext.js.runner.m.ex

import io.suggest.xadv.ext.js.runner.m.MErrorInfoT

import scala.scalajs.js.{Dictionary, Any}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.02.15 16:17
 * Description: При проблеме на странице, подсистема может сообщить об этом серверу.
 */
case class DomUpdateException(underlying: Throwable = null) extends IllegalStateException with MErrorInfoT {
  override def msg: String = "e.adv.ext.dom.update"
  override def args = Seq.empty

  override def getCause: Throwable = {
    if(underlying == null) super.getCause else underlying
  }

  override def info = {
    Option(underlying) map { cause =>
      Dictionary[Any](
        "eclass" -> cause.getClass.getName,
        "emsg"   -> cause.getMessage
      )
    }
  }
}
