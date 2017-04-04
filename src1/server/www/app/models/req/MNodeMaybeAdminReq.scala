package models.req

import io.suggest.model.n2.node.MNode
import play.api.mvc.Request

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.17 19:03
  * Description: Модель реквеста ограниченного доступа к узлу, когда юзер может быть или не быть его админом.
  */
trait INodeMaybeAdminReq[A] extends INodeReq[A] {

  /** Является ли юзер админом текущего узла? */
  def isAdmin: Boolean

  /** Рекламная карточка, если есть. */
  def adProdReqOpt: Option[IAdProdReq[A]]

}


/** Дефолтовая реализация модели реквеста [[INodeMaybeAdminReq]]. */
case class MNodeMaybeAdminReq[A](
                                  override val mnode        : MNode,
                                  override val isAdmin      : Boolean,
                                  override val adProdReqOpt : Option[IAdProdReq[A]],
                                  override val request      : Request[A],
                                  override val user         : ISioUser
                                )
  extends MReqWrap[A]
  with INodeMaybeAdminReq[A]
