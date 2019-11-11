package models.usr.esia

import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.03.19 22:31
  * Description: QS-модель для ссылки первого шага - отправки юзера на логин в ЕСИА.
  * https://esia-portal1.test.gosuslugi.ru/aas/oauth2/ac
  * @see [[https://digital.gov.ru/uploaded/presentations/esiametodicheskierekomendatsii254.pdf]] с.171.
  */

object MEsiaStep1Qs {

  object Fields {
    val RESPONSE_TYPE_FN  = "response_type"
    val CLIENT_SECRET_FN  = "client_secret"
    val REDIRECT_URI_FN   = "redirect_uri"
    val ACCESS_TYPE_FN    = "access_type"
  }

  /** Write-only биндер для ссылки на авторизацию ЕСИА. */
  implicit def esiaStep1QsSignedQsb(implicit
                                    esiaSignContentB  : QueryStringBindable[MEsiaSignContent],
                                    stringB           : QueryStringBindable[String],
                                    esiaRespTypeB     : QueryStringBindable[MEsiaRespType],
                                    esiaAccessTypeB   : QueryStringBindable[MEsiaAccessType],
                                   ): QueryStringBindable[MEsiaStep1Qs] = {
    new QueryStringBindableImpl[MEsiaStep1Qs] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MEsiaStep1Qs]] = {
        // Скорее всего, этот код никогда не вызывается.
        val F = Fields
        for {
          signContentE          <- esiaSignContentB.bind( key,                  params )
          clientSecretE         <- stringB.bind         ( F.CLIENT_SECRET_FN,   params )
          esiaRespTypeE         <- esiaRespTypeB.bind   ( F.RESPONSE_TYPE_FN,   params )
          redirectUriE          <- stringB.bind         ( F.REDIRECT_URI_FN,    params )
          esiaAccessTypeE       <- esiaAccessTypeB.bind ( F.ACCESS_TYPE_FN,     params )
        } yield {
          for {
            signContent         <- signContentE
            clientSecret        <- clientSecretE
            esiaRespType        <- esiaRespTypeE
            redirectUri         <- redirectUriE
            esiaAccessType      <- esiaAccessTypeE
          } yield {
            MEsiaStep1Qs(
              signContent       = signContent,
              clientSecret      = clientSecret,
              responseType      = esiaRespType,
              redirectUri       = redirectUri,
              accessType        = esiaAccessType,
            )
          }
        }
      }

      override def unbind(key: String, value: MEsiaStep1Qs): String = {
        val F = Fields
        _mergeUnbinded1(
          esiaSignContentB.unbind   ( key,                  value.signContent   ),
          stringB.unbind            ( F.CLIENT_SECRET_FN,   value.clientSecret  ),
          esiaRespTypeB.unbind      ( F.RESPONSE_TYPE_FN,   value.responseType  ),
          stringB.unbind            ( F.REDIRECT_URI_FN,    value.redirectUri   ),
          esiaAccessTypeB.unbind    ( F.ACCESS_TYPE_FN,     value.accessType    ),
        )
      }

    }
  }

}


/** Контейнер-обёртка над [[MEsiaSignContent]] для выставления подписи.
  *
  * @param signContent Контейнер неподписанных qs-данных. Подпись будет вставлена в client_secret.
  * @param clientSecret Подпись suggest.io на signContent. PCKS#7 detached signature в base64 url safe.
  * @param responseType Ожидаемый тип ответа ЕСИА для системы-клиента.
  * @param redirectUri URL для редиректа юзера назад в suggest.io.
  * @param accessType offline для доступа без юзера.
  *                   online для доступа только в присутствии юзера.
  */
final case class MEsiaStep1Qs(
                               signContent    : MEsiaSignContent,
                               clientSecret   : String,
                               responseType   : MEsiaRespType,
                               redirectUri    : String,
                               accessType     : MEsiaAccessType,
                             )
