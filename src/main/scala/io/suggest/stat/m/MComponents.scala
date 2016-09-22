package io.suggest.stat.m

import io.suggest.common.menum.{EnumMaybeWithName, StrIdValT}
import io.suggest.model.menum.EnumJsonReadsValT

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.09.16 18:49
  * Description: Модель кодов крупных компонентов системы s.io.
  */
object MComponents extends EnumMaybeWithName with EnumJsonReadsValT with StrIdValT {

  /** Класс для всех экземпляров модели. */
  protected class Val(override val strId: String)
    extends super.Val
    with ValT

  override type T = Val

  /** Компонент sc (showcase), т.е. выдача на главной. */
  val Sc: T = new Val("выдача")

}
