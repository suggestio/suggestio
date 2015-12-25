package models.req

import models.mbill.{MContract, MTariffFee}
import play.api.mvc.Request

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.15 19:08
 * Description: Модель реквеста с инстансами MTariffFee и MContract внутри.
 */
trait ITariffFeeContractReq[A]
  extends IContract1Req[A]
{
  def mTariffFee: MTariffFee
}


case class MTariffFeeContractReq[A](
  override val mTariffFee   : MTariffFee,
  override val mcontract    : MContract,
  override val request      : Request[A],
  override val user         : ISioUser
)
  extends MReqWrap[A]
  with ITariffFeeContractReq[A]
