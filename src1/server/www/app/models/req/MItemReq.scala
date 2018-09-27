package models.req

import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.MItem
import play.api.mvc.Request

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.02.16 16:22
  * Description: Модель реквеста, неразрыно связанных с каким-то MItem.
  */
trait IItemReq[A] extends IReq[A] {
  def mitem: MItem
}


case class MItemReq[A](
  override val mitem      : MItem,
  override val request    : Request[A],
  override val user       : ISioUser
)
  extends MReqWrap[A]
  with IItemReq[A]


/** Контейнер реквеста для order-ids. */
case class MOrderIdsReq[A](
                            orderIds                : Iterable[Gid_t],
                            contractId              : Gid_t,
                            override val request    : Request[A],
                            override val user       : ISioUser
                          )
  extends MReqWrap[A]
