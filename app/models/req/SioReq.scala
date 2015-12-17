package models.req

import play.api.mvc.Request

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.12.15 22:15
 * Description: Система реквест-контейнеров sio второго поколения.
 * Пришла на смену костыльному поколению реквестов на базе AbstractRequestWithPwOpt.
 */

/** Интерфейс заголовков реквеста sio. */
trait SioReqHdr
  extends ExtReqHdr
{

  /** Модель инфы о текущем юзере из ActionBuilder'а. Есть в каждом sio-реквесте. */
  def user: ISioUser

}


/** Трейт реквеста sio с телом реквеста. */
trait SioReq[A]
  extends Request[A]
  with SioReqHdr
