package models.adv.bill

import io.suggest.common.menum.EnumMaybeWithName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.11.15 15:40
 * Description: Модель режимов/состояний экземпляров модели [[MAdv2]].
 */

object MAdv2Modes extends EnumMaybeWithName {

  protected[this] abstract class Val(val strId: String)
    extends super.Val(strId)
  {
    def tableSuffix: String
    def tableName: String = "adv2_" + tableSuffix
  }

  override type T = Val

  val Req: T = new Val("r") {
    override def tableSuffix: String = "req"
  }

}
