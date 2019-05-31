package io.suggest.mbill2.m.ott

import java.util.UUID

import io.suggest.slick.profile.IProfile

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.05.19 17:55
  * Description: Поддержка поля token_id
  */
trait TokenIdSlick extends IProfile {

  import profile.api._

  def TOKEN_ID_FN = "token_id"

  trait TokenIdColumn { that: Table[_] =>
    def tokenId = column[UUID]( TOKEN_ID_FN )
  }

}
