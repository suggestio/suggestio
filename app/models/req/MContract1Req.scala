package models.req

import models.mbill.MContract
import play.api.mvc.Request

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.15 18:42
 * Description: Модель реквеста с инстансом какого-то контракта.
 */
trait IContract1Req[A] extends IReq[A] {
  def mcontract: MContract
}


case class MContract1Req[A](
  override val mcontract  : MContract,
  override val request    : Request[A],
  override val user       : ISioUser
)
  extends MReqWrap[A]
  with IContract1Req[A]
