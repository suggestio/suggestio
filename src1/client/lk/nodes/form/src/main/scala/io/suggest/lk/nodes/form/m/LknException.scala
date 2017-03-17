package io.suggest.lk.nodes.form.m

import io.suggest.sjs.common.xhr.Xhr
import org.scalajs.dom.ext.AjaxException

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.17 14:26
  * Description: Модели ошибок и исключений.
  */
sealed trait ILknException extends RuntimeException {
  def msgCode: String
  def titleOpt: Option[String] = None
}

object ILknException {

  def apply(ex: Throwable): ILknException = {
    ex match {
      case ae: AjaxException if ae.xhr.status == Xhr.Status.CONFLICT =>
        NodeAlreadyExistException(ae)

      case _ =>
        LknException(ex)
    }
  }

}


/** Какая-то произвольная ошибка. */
case class LknException(override val getCause: Throwable) extends ILknException {
  override def msgCode = "Error"
  override def titleOpt = Option( getCause ).map(_.toString)
}

/** Ошибка, связанная с конфликтом id узла. */
case class NodeAlreadyExistException(override val getCause: Throwable) extends ILknException {
  override def msgCode = "Node.with.such.id.already.exists"
}

