package io.suggest.xadv.ext.js.runner.m.ex

import io.suggest.xadv.ext.js.runner.m.MErrorInfoT

import scala.scalajs.js
import scala.scalajs.js.Any

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.03.15 12:22
 * Description: Какая произвольная проблема, задаваемая кодом отображаемого сообщения и т.д.
 */
case class CustomException(
  msg   : String,
  args  : Seq[String] = Seq.empty,
  cause : Option[Throwable] = None
) extends Exception with MErrorInfoT {

  override def getCause = cause getOrElse this

  override def info = cause.map { ex =>
    js.Dictionary[Any](
      "eclass" -> ex.getClass.getName,
      "emsg"   -> ex.getMessage
    )
  }
}
