package models.req

import models.MNode
import play.api.mvc.Request

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.15 19:16
 * Description: Реквест с инстансом рекламной карточки внутри.
 */
trait IAdReq[A] extends IReq[A] {
  def mad: MNode
}


/** Реализация модели реквеста с узлом-карточкой внутри. */
case class MAdReq[A](
  mad     : MNode,
  request : Request[A],
  user    : ISioUser
)
  extends MReqWrap[A]
  with IAdReq[A]
