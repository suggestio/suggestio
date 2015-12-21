package models.req

import models.mbill.{MContract, MTariffStat}
import play.api.mvc.Request

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.15 22:20
 * Description: Реквест с экземпляром MTariffStat внутри.
 */
trait ITariffStatContractReq[A] extends IContractReq[A] {
  def mTariffStat: MTariffStat
}


case class MTariffStatContractReq[A](
  override val mTariffStat  : MTariffStat,
  override val mcontract    : MContract,
  override val request      : Request[A],
  override val user         : ISioUser
)
  extends MReqWrap[A]
  with ITariffStatContractReq[A]
