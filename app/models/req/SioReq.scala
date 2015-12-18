package models.req

import play.api.mvc.{Request, RequestHeader}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.12.15 22:15
 * Description: Система реквест-контейнеров sio второго поколения.
 * Пришла на смену костыльному поколению реквестов на базе AbstractRequestWithPwOpt.
 */

/** Интерфейс заголовков реквеста sio. */
trait ISioReqHdr
  extends RequestHeader
  with ExtReqHdr
{

  /** Модель общей инфы о текущем юзере из ActionBuilder'а. Есть в каждом sio-реквесте.
    * В теории здесь может быть любая common-инфа, не только пользовательская, но общая для всех реквестов. */
  def user: ISioUser

}


/** Завёрнутый заголовок реквест с дополнения от sio. */
trait ISioReqHdrWrap
  extends RequestHeaderWrap
  with ISioReqHdr


/** Трейт реквеста sio. */
trait ISioReq[A]
  extends Request[A]
  with ISioReqHdr


/** Трейт завёрнутого реквеста. */
trait ISioReqWrap[A]
  extends RequestWrap[A]
  with ISioReq[A]


/** Абстрактный класс, реализующий части ISioReq.
  * Именно он пришел на смену AbstractRequestWithPwOpt[A] и некоторым смежным классам.
  * Готовый класс упрощает жизнь компилятору, финальные реквесты должны наследовать именно этот класс. */
abstract class SioReqWrap[A]
  extends ISioReqWrap[A]


/** Реквест, но без тела. Обращение к телу такого реквеста будет вызывать исключение.
  * Подмешивается к ISioReq[A] для websocket-экшенов. */
trait NoBodyRequest extends ISioReq[Nothing] with ISioReqHdrWrap {

  /** @return [[java.lang.UnsupportedOperationException]] всегда */
  override def body: Nothing = {
    throw new UnsupportedOperationException("This is request headers wrapper. Body never awailable here.")
  }
}


/** Реквест к sio. */
case class SioReq[A](
  override val request  : Request[A],
  override val user     : ISioUser
)
  extends ISioReqWrap[A]
