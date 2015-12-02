package io.suggest.common.menum

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 14:13
 * Description: Поддержка apply/unapply действий над scala enumeration.
 */
trait EnumApply extends EnumValue2Val with IVeryLightEnumeration {

  protected[this] trait ValT extends super.ValT {
    def strId: String
  }

  override type T <: Val with ValT

  def withNameT(name: String): T = {
    withName(name)
  }

  def unapply(x: T): Option[String] = {
    Some(x.strId)
  }

}
