package models.req

import models.usr.{EmailActivation, EmailPwIdent}
import play.api.mvc.Request

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.12.15 12:06
 * Description: Реквест с данными для восстановления пароля.
 */
trait IEmailPwIdentReq[A] extends ISioReq[A] {
  def epw: EmailPwIdent
}


trait IRecoverPwReq[A]
  extends MEmailActivationReq[A]
  with IEmailPwIdentReq[A]


case class MRecoverPwReq[A](
  override val epw     : EmailPwIdent,
  override val eact    : EmailActivation,
  override val request : Request[A],
  override val user    : ISioUser
)
  extends SioReqWrap[A]
  with IRecoverPwReq[A]
