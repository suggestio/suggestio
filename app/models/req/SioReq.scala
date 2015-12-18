package models.req

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.12.15 22:15
 * Description: Система реквест-контейнеров sio второго поколения.
 * Пришла на смену костыльному поколению реквестов на базе AbstractRequestWithPwOpt.
 */

/** Интерфейс заголовков реквеста sio. */
trait ISioReqHdr
  extends RequestHeaderWrap
  with ExtReqHdr
{

  /** Модель общей инфы о текущем юзере из ActionBuilder'а. Есть в каждом sio-реквесте.
    * В теории здесь может быть любая common-инфа, не только пользовательская, но общая для всех реквестов. */
  def user: ISioUser

}


/** Трейт реквеста sio с телом реквеста. */
trait ISioReq[A]
  extends RequestWrap[A]
  with ISioReqHdr


/** Абстрактный класс, реализующий части ISioReq.
  * Именно он пришел на смену AbstractRequestWithPwOpt[A] и некоторым смежным классам.
  * Готовый класс упрощает жизнь компилятору, финальные реквесты должны наследовать этот класс. */
abstract class SioReq[A]
  extends ISioReq[A]

