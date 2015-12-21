package models.req

import play.api.mvc.{AnyContent, Request, RequestHeader}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.12.15 22:15
 * Description: Система реквест-контейнеров sio второго поколения.
 * Пришла на смену костыльному поколению реквестов на базе AbstractRequestWithPwOpt.
 */

/** Интерфейс заголовков реквеста sio. */
trait IReqHdr
  extends RequestHeader
  with ExtReqHdr
{

  /** Модель общей инфы о текущем юзере из ActionBuilder'а. Есть в каждом sio-реквесте.
    * В теории здесь может быть любая common-инфа, не только пользовательская, но общая для всех реквестов. */
  def user: ISioUser

}


/** Завёрнутый заголовок реквест с дополнения от sio. */
trait IReqHdrWrap
  extends RequestHeaderWrap
  with IReqHdr

abstract class MReqHdrWrap
  extends IReqHdrWrap


/** Трейт реквеста sio. */
trait IReq[A]
  extends Request[A]
  with IReqHdr


/** Трейт завёрнутого реквеста. */
trait IReqWrap[A]
  extends RequestWrap[A]
  with IReq[A]


/** Абстрактный класс, реализующий части ISioReq.
  * Именно он пришел на смену AbstractRequestWithPwOpt[A] и некоторым смежным классам.
  * Готовый класс упрощает жизнь компилятору, финальные реквесты должны наследовать именно этот класс. */
abstract class MReqWrap[A]
  extends MReqHdrWrap
  with IReqWrap[A]


/** Реквест, но без тела. Обращение к телу такого реквеста будет вызывать исключение.
  * Подмешивается для websocket-экшенов, когда нужен именно реквест. */
trait NoBodyRequest extends IReq[AnyContent] with IReqHdrWrap {

  /** @return [[java.lang.UnsupportedOperationException]] всегда */
  override def body: AnyContent = {
    throw new UnsupportedOperationException("This is request headers wrapper. Body never awailable here.")
  }
}


/** Заголовки реквеста в представлении sio. */
case class MReqHdr(
  override val request: RequestHeader,
  override val user   : ISioUser
)
  extends MReqHdrWrap


/** Реквест к sio. */
case class MReq[A](
  override val request  : Request[A],
  override val user     : ISioUser
)
  extends MReqWrap[A]


/** Особый случай реквеста, когда нет тела реквеста, но очень нужно API от реквеста.
  * Используется, когда на входе только RequestHeader, а очень нужен Request[],
  * и body() точно никто дёргать не будет. */
case class MReqNoBody(
  override val request: RequestHeader,
  override val user   : ISioUser
)
  extends MReqHdrWrap
  with NoBodyRequest
