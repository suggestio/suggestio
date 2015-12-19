package models.req

import models.MNode
import play.api.mvc.Request

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.12.15 9:34
 * Description: Модель реквеста с рекламной карточкой и её продьюсером.
 */
trait IAdProdReq[A] extends IAdReq[A] {

  def producer: MNode

}


/** Дефолтовая реализация модели реквеста [[IAdProdReq]]. */
case class MAdProdReq[A](
  mad       : MNode,
  producer  : MNode,
  request   : Request[A],
  user      : ISioUser
)
  extends SioReqWrap[A]
  with IAdProdReq[A]
