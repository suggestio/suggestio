package models.req

import io.suggest.n2.node.MNode
import play.api.mvc.Request

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.11.16 16:20
  * Description: Модель реквеста про карточку, её продьюсера и какого-то целевого узла.
  * Например, проверка прав на размещение карточки на узле.
  */
trait IAdProdRcvrReq[A]
  extends INodeReq[A]
  with IAdProdReq[A]

case class MAdProdRcvrReq[A](
  override val mad       : MNode,
  override val producer  : MNode,
  override val mnode     : MNode,
  override val request   : Request[A],
  override val user      : ISioUser
)
  extends MReqWrap[A]
  with IAdProdRcvrReq[A]
