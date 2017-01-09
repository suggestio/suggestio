package models.req

import models.MNode
import play.api.mvc.Request

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.12.15 9:34
 * Description: Модель реквеста с рекламной карточкой и её продьюсером.
 */

/** Интерфейс для реквестов, имеющих поле продьюсера. */
trait IProdReq[A] extends IReq[A] {
  def producer: MNode
}

/** Интерфейс реквестов, содержащих карточку и её продьюсера. */
trait IAdProdReq[A]
  extends IAdReq[A]
  with IProdReq[A]


/** Дефолтовая реализация модели реквеста [[IAdProdReq]]. */
case class MAdProdReq[A](
  override val mad       : MNode,
  override val producer  : MNode,
  override val request   : Request[A],
  override val user      : ISioUser
)
  extends MReqWrap[A]
  with IAdProdReq[A]
