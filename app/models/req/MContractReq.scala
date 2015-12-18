package models.req

import models.mbill.MContract
import play.api.mvc.Request

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.15 18:42
 * Description: Модель реквеста с инстансом какого-то контракта.
 */
trait IContractReq[A] extends ISioReq[A] {
  def mcontract: MContract
}


case class MContractReq[A](
  override val mcontract  : MContract,
  override val request    : Request[A],
  override val user       : ISioUser
)
  extends SioReqWrap[A]
  with IContractReq[A]
