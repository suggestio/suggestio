package models.req

import models.ai.MAiMad
import play.api.mvc.Request

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.15 18:28
 * Description: Модель реквеста с инстансом MAiMad внутри.
 */
trait IAiMadReq[A] extends ISioReq[A] {
  def aiMad: MAiMad
}


/** Реализация модели реквеста с инстансом MAiMad внутри. */
case class MAiMadReq[A](
  override val aiMad    : MAiMad,
  override val request  : Request[A],
  override val user     : ISioUser
)
  extends SioReqWrap[A]
  with IAiMadReq[A]
