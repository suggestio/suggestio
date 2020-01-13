package models.req

import io.suggest.n2.node.MNode
import play.api.mvc.Request

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.15 22:29
 * Description: Реквест с данными активации узла (реализация инвайта на узел).
 * @param emailOwner Юзер-владелец email, если существует.
 */

case class MNodeInviteReq[A](
                              override val mnode     : MNode,
                              nodeOwnerIds           : Set[String],
                              emailOwner             : Option[MNode],
                              override val request   : Request[A],
                              override val user      : ISioUser
                            )
  extends MReqWrap[A]
  with INodeReq[A]
