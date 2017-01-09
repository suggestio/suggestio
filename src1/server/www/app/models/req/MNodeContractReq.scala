package models.req

import io.suggest.mbill2.m.contract.MContract
import models.MNode
import play.api.mvc.Request

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.15 18:57
 * Description: Модель реквеста с контрактом и узлом.
 */
trait INodeContractReq[A]
  extends IContractReq[A]
  with INodeReq[A]


/** Реализация модели [[INodeContractReq]], т.е. реквеста с контрактом и узлом внутри. */
case class MNodeContractReq[A](
  override val mnode       : MNode,
  override val mcontract   : MContract,
  override val request     : Request[A],
  override val user        : ISioUser
)
  extends MReqWrap[A]
  with INodeContractReq[A]
