package io.suggest.mbill2.m.ott

import java.util.UUID

import io.suggest.mbill2.m.gid.IdSlick
import io.suggest.slick.profile.IProfile

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.05.19 17:55
  * Description: Поддержка поля token_id
  */
trait TokenIdSlick extends IProfile with IdSlick {

  import profile.api._

  def TOKEN_ID_FN = "token_id"

  override type Id_t = UUID

  trait TokenIdColumn extends IdColumn { that: Table[_] =>
    def id = column[UUID]( TOKEN_ID_FN )
  }

}
