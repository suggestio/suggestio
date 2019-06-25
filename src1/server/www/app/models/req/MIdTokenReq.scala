package models.req

import io.suggest.id.token.MIdToken
import io.suggest.mbill2.m.ott.MOneTimeToken
import play.api.mvc.Request

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.06.19 11:52
  * Description: Контейнер данных реквеста с распарсенным id-токеном.
  */
case class MIdTokenReq[A](
                           idToken                : MIdToken,
                           ottOpt                 : Option[MOneTimeToken],
                           override val user      : ISioUser,
                           override val request   : Request[A],
                         )
  extends MReqWrap[A]
