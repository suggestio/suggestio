package models.usr.esia

import java.time.OffsetDateTime
import java.util.UUID

import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.03.19 10:30
  * Description: Модель подписываемых данных для ЕСИА.
  */

object MEsiaSignContent {

  object Fields {
    val CLIENT_ID_FN      = "client_id"
    val TIMESTAMP_FN      = "timestamp"
    val STATE_FN          = "state"
    val SCOPE_FN          = "scope"
  }


  implicit def esiaQsSignContentQsb(implicit
                                    stringB         : QueryStringBindable[String],
                                    uuidB           : QueryStringBindable[UUID],
                                   ): QueryStringBindable[MEsiaSignContent] = {
    val timestampB = MEsiaQs.esiaTimestampQsb
    new QueryStringBindableImpl[MEsiaSignContent] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MEsiaSignContent]] = {
        val F = Fields
        for {
          clientIdE             <- stringB.bind         ( F.CLIENT_ID_FN,       params )
          timestampE            <- timestampB.bind      ( F.TIMESTAMP_FN,       params )
          stateE                <- uuidB.bind           ( F.STATE_FN,           params )
          scopeE                <- stringB.bind         ( F.SCOPE_FN,           params )
        } yield {
          for {
            clientId            <- clientIdE
            timestamp           <- timestampE
            state               <- stateE
            scope               <- scopeE
          } yield {
            MEsiaSignContent(
              clientId          = clientId,
              timestamp         = timestamp,
              state             = state,
              scope             = scope,
            )
          }
        }
      }

      override def unbind(key: String, value: MEsiaSignContent): String = {
        val F = Fields
        _mergeUnbinded1(
          stringB.unbind        ( F.CLIENT_ID_FN,       value.clientId     ),
          timestampB.unbind     ( F.TIMESTAMP_FN,       value.timestamp    ),
          uuidB.unbind          ( F.STATE_FN,           value.state        ),
          stringB.unbind        ( F.SCOPE_FN,           value.scope        ),
        )
      }

    }
  }

  @inline implicit def univEq: UnivEq[MEsiaSignContent] = UnivEq.derive

}


/** Контейнер данных запроса логина.
  *
  * @param clientId id системы по мнению ЕСИА.
  * @param timestamp текущее время.
  * @param state nonce или какой-то идентификатор состояния на стороне системы-клиента.
  * @param scope "openid" или что-нибудь ещё.
  */
final case class MEsiaSignContent(
                                   clientId      : String,
                                   timestamp     : OffsetDateTime,
                                   state         : UUID,
                                   scope         : String,
                                 )

