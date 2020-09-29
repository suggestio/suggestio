package models.req

import io.suggest.n2.node.MNode
import play.api.mvc.Request

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.10.17 22:08
  * Description: Модель реквеста с данными по продьюсеру и опциональным инстансом карточки.
  */


/** Класс модели реквеса, который содержит данные по продьюсеру с опциональной рекламной карточкой.
  *
  * @param madOpt Опциональная рекламная карточка.
  * @param producer Узел-продьюсер.
  * @param request Исходный HTTP-реквест.
  * @param user Данные по текущему юзеру.
  * @tparam A Request body.
  */
case class MAdOptProdReq[A](
                             madOpt                 : Option[MNode],
                             override val producer  : MNode,
                             override val request   : Request[A],
                             override val user      : ISioUser
                           )
  extends MReqWrap[A]
  with IProdReq[A]


object MAdOptProdReq {

  def apply[A](madOpt: Option[MNode], req0: INodeReq[A]): MAdOptProdReq[A] = {
    MAdOptProdReq(
      madOpt    = madOpt,
      producer  = req0.mnode,
      request   = req0,
      user      = req0.user
    )
  }

  def apply[A](madOpt: Option[MNode], req0: IProdReq[A]): MAdOptProdReq[A] = {
    MAdOptProdReq(
      madOpt    = madOpt,
      producer  = req0.producer,
      request   = req0,
      user      = req0.user
    )
  }

  def apply[A](req0: IAdProdReq[A]): MAdOptProdReq[A] =
    MAdOptProdReq( Some( req0.mad ), req0 )

}
