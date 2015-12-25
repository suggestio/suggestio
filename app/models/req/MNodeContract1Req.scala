package models.req

import models.MNode
import models.mbill.MContract
import play.api.mvc.Request

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.15 18:57
 * Description: Модель реквеста с контрактом и узлом.
 */
trait INodeContract1Req[A]
  extends IContract1Req[A]
  with INodeReq[A]


/** Реализация модели [[INodeContract1Req]], т.е. реквеста с контрактом и узлом внутри. */
case class MNodeContract1Req[A](
  override val mnode       : MNode,
  override val mcontract   : MContract,
  override val request     : Request[A],
  override val user        : ISioUser
)
  extends MReqWrap[A]
  with INodeContract1Req[A]
