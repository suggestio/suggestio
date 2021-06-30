package models.usr.esia

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumJvmUtil
import io.suggest.xplay.qsb.CrossQsBindable
import japgolly.univeq.UnivEq
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.03.19 10:47
  * Description: Варианты qs-поля grant_type для запросов access_token ЕСИА.
  */

object MEsiaGrantTypes extends StringEnum[MEsiaGrantType] {

  /** Авторизационный  код обменивается на маркер доступа. */
  case object AuthorizationCode extends MEsiaGrantType( "authorization_code" )


  override def values = findValues

}


sealed abstract class MEsiaGrantType(override val value: String) extends StringEnumEntry

object MEsiaGrantType {

  @inline implicit def univEq: UnivEq[MEsiaGrantType] = UnivEq.derive

  implicit def esiaGrantTypeQsb: CrossQsBindable[MEsiaGrantType] =
    EnumeratumJvmUtil.valueEnumQsb( MEsiaGrantTypes )

}
