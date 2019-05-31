package util.sms

import javax.inject.Inject
import models.sms.{ISmsSendResult, MSms}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.05.19 18:00
  * Description: Реализация клиента sms-клиента.
  */
class SmsRuClient @Inject() (
                              wsClient                  : WSClient,
                              implicit private val ec   : ExecutionContext,
                            )
  extends ISmsSendClient
{

  override def isReady(): Future[Boolean] = {

    ???
  }

  override def smsSend(sms: MSms): Future[ISmsSendResult] = {
    //wsClient.url(  )
    ???
  }

}
