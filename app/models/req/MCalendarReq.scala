package models.req

import models.MCalendar
import play.api.mvc.Request

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.15 17:32
 * Description: Модель sio-реквеста с календарём.
 */

trait ICalendarReq[A]
  extends IReq[A]
{
  def mcal: MCalendar
}


/** Реализация реквеста с календарём. */
case class MCalendarReq[A](
  override val mcal     : MCalendar,
  override val request  : Request[A],
  override val user     : ISioUser
)
  extends MReqWrap[A]
  with ICalendarReq[A]
