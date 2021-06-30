package models.usr.esia

import io.suggest.xplay.qsb.AbstractQueryStringBindable
import io.suggest.url.bind.QueryStringBindableUtil._
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.03.19 10:27
  * Description: QS-модель для исходящего реквеста
  */
object MEsiaAcTokQs {

  object Fields {
    val CODE_FN             = "code"
    val GRANT_TYPE_FN       = "grant_type"
    val CLIENT_SECRET_FN    = "client_secret"
    val REDIRECT_URI_FN     = "redirect_uri"
    val TOKEN_TYPE_FN       = "token_type"
  }


  implicit def esiaAcTokQsb(implicit
                            signContentB    : QueryStringBindable[MEsiaSignContent],
                            stringB         : QueryStringBindable[String],
                            esiaGrantTypeB  : QueryStringBindable[MEsiaGrantType],
                            esiaTokenTypeB  : QueryStringBindable[MEsiaTokenType],
                           ): QueryStringBindable[MEsiaAcTokQs] = {

    new AbstractQueryStringBindable[MEsiaAcTokQs] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MEsiaAcTokQs]] = {
        val F = Fields
        for {
          signContentE        <- signContentB   .bind( key,                   params )
          clientSecretE       <- stringB        .bind( F.CLIENT_SECRET_FN,    params )
          authCodeE           <- stringB        .bind( F.CODE_FN,             params )
          grantTypeE          <- esiaGrantTypeB .bind( F.GRANT_TYPE_FN,       params )
          redirectUriE        <- stringB        .bind( F.REDIRECT_URI_FN,     params )
          tokenTypeE          <- esiaTokenTypeB .bind( F.TOKEN_TYPE_FN,       params )
        } yield {
          for {
            signContent       <- signContentE
            clientSecret      <- clientSecretE
            authCode          <- authCodeE
            grantType         <- grantTypeE
            redirectUri       <- redirectUriE
            tokenToken        <- tokenTypeE
          } yield {
            MEsiaAcTokQs(
              signContent   = signContent,
              clientSecret  = clientSecret,
              authCode      = authCode,
              grantType     = grantType,
              redirectUri   = redirectUri,
              tokenType     = tokenToken,
            )
          }
        }
      }

      override def unbind(key: String, value: MEsiaAcTokQs): String = {
        val F = Fields
        _mergeUnbinded1(
          signContentB    .unbind( key,                   value.signContent   ),
          stringB         .unbind( F.CLIENT_SECRET_FN,    value.clientSecret  ),
          stringB         .unbind( F.CODE_FN,             value.authCode      ),
          esiaGrantTypeB  .unbind( F.GRANT_TYPE_FN,       value.grantType     ),
          stringB         .unbind( F.REDIRECT_URI_FN,     value.redirectUri   ),
          esiaTokenTypeB  .unbind( F.TOKEN_TYPE_FN,       value.tokenType     ),
        )
      }

    }

  }

}


case class MEsiaAcTokQs(
                         signContent      : MEsiaSignContent,
                         clientSecret     : String,
                         authCode         : String,
                         grantType        : MEsiaGrantType,
                         redirectUri      : String,
                         tokenType        : MEsiaTokenType,
                       )
