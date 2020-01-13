package models.req

import io.suggest.mbill2.m.order.MOrder
import io.suggest.n2.node.MNode
import play.api.mvc.Request

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.02.17 15:27
  * Description: Запрос с ордером внутри.
  */
trait IOrderReq[A] extends IReq[A] {

  /** Инстанс ордера, вокруг которого выполняется запрос. */
  def morder: MOrder

}


/** Трейт запроса с ордером и нодой внутри. */
trait INodeOrderReq[A]
  extends IOrderReq[A]
  with INodeReq[A]


/** Класс реквеста с ордером внутри. */
case class MNodeOrderReq[A](
                             override val morder   : MOrder,
                             override val mnode    : MNode,
                             override val user     : ISioUser,
                             override val request  : Request[A]
                           )
  extends MReqWrap[A]
  with INodeOrderReq[A]


/** Реквест с опциональным mnode, по аналогии с [[MNodeOrderReq]]. */
case class MNodeOptOrderReq[A](
                                override val morder   : MOrder,
                                override val mnodeOpt : Option[MNode],
                                override val user     : ISioUser,
                                override val request  : Request[A]
                              )
  extends MReqWrap[A]
  with IOrderReq[A]
  with INodeOptReq[A]


/** Узел + опциональный ордер. */
case class MNodeOrderOptReq[A](
                                morderOpt               : Option[MOrder],
                                override val mnode      : MNode,
                                override val user       : ISioUser,
                                override val request    : Request[A]
                              )
  extends MReqWrap[A]
  with INodeReq[A]


/** Модель реквеста с опциональным инстансом MOrder. */
case class MOrderOptReq[A](
                            morderOpt             : Option[MOrder],
                            override val user     : ISioUser,
                            override val request  : Request[A]
                          )
  extends MReqWrap[A]

object MOrderOptReq {
  def apply[A](morderOpt: Option[MOrder], req: IReq[A]): MOrderOptReq[A] =
    MOrderOptReq(morderOpt, req.user, req)
}
