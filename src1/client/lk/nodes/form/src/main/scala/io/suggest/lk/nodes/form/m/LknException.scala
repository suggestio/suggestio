package io.suggest.lk.nodes.form.m

import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.model.HttpFailedException
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.17 14:26
  * Description: Модели ошибок и исключений.
  */
sealed trait ILknException extends RuntimeException {
  def msgCode: String
  def titleOpt: Option[String]
}

object LknException {

  def apply(ex: Throwable): ILknException = {
    ex match {
      case ae: HttpFailedException =>
        if (ae.resp.exists(_.status ==* HttpConst.Status.CONFLICT)) {
          LknException( "Node.with.such.id.already.exists", ae )
        } else {
          unknown(ae)
        }

      case _ =>
        unknown(ex)
    }
  }

  def unknown(ex: Throwable) = LknException(
    msgCode  = "Error",
    getCause = ex,
    titleOpt = Option(ex).map(_.toString)
  )

}


/** Ошибка, причина которой определена и выставлена в msgCode. */
case class LknException(
                         override val msgCode: String,
                         override val getCause: Throwable,
                         override val titleOpt: Option[String] = None
                       )
  extends ILknException

