package io.suggest.sjs.common.model.wsproto

import io.suggest.common.menum.LightEnumeration
import io.suggest.common.ws.proto.{MAnswerStatusesBaseT, AnswerStatusConstants}
import AnswerStatusConstants._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 14:10
 * Description: Реализация sjs-модели со статусами ответов на suggest.io.
 */
object MAnswerStatuses extends MAnswerStatusesBaseT with LightEnumeration {

  protected sealed abstract class Val(val jsStr: String) extends super.ValT

  override type T = Val

  override val Success        : T = new Val(ST_SUCCESS) with VSuccess
  override val Error          : T = new Val(ST_ERROR) with VError
  override val FillContext    : T = new Val(ST_FILL_CONTEXT) with VFillCtx

  override def maybeWithName(n: String): Option[T] = {
    n match {
      case Success.jsStr      => Some(Success)
      case Error.jsStr        => Some(Error)
      case FillContext.jsStr  => Some(FillContext)
      case _                  => None
    }
  }

}
