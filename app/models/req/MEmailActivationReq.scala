package models.req

import models.usr.EmailActivation
import play.api.mvc.Request

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.12.15 12:57
 * Description: Модель реквеста с экземпляром EmailActivation внутри.
 */
trait IEmailActivationReq[A] extends ISioReq[A] {
  def eact      : EmailActivation
}


case class MEmailActivationReq[A](
  override val eact    : EmailActivation,
  override val request : Request[A],
  override val user    : ISioUser
)
  extends SioReqWrap[A]
  with IEmailActivationReq[A]
