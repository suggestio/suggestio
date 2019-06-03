package util.sms

import models.sms.{ISmsSendResult, MSmsSend}

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.05.19 18:01
  * Description: Интерфейс клиента сервиса отправки смс.
  */
trait ISmsSendClient {

  /** Готов ли сервис к отправке смс?
    * По идее, это синхронный метод, но тут асинхрон на всякий случай. */
  def isReady(): Future[Boolean]

  /** Отправить одно или несколько sms на указанные номера.
    *
    * @return Фьючерс с результатом отправки.
    */
  def smsSend(sms: MSmsSend): Future[Seq[ISmsSendResult]]

}
