package models.msc.resp

import io.suggest.common.menum.{EnumMaybeWithName, StrIdValT}
import io.suggest.sc.ScConstants

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.09.16 18:56
  * Description: Список sc-resp-экшенов.
  */
object MScRespActionTypes extends EnumMaybeWithName with StrIdValT {

  protected class Val(override val strId: String) extends
    super.Val(strId)
    with ValT

  override type T = Val

  /** Ответ по index'у. */
  val Index = new Val( ScConstants.Resp.INDEX_RESP_ACTION )

}
