package io.suggest.ym.model.common

import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.model.menum.EnumJsonReadsValT

/** Положение участника сети и его возможности описываются флагами прав доступа. */
object AdnRights extends EnumMaybeWithName with EnumJsonReadsValT {

  protected[this] sealed abstract class Val(val name: String)
    extends super.Val(name)
  {
    def longName: String
  }

  override type T = Val

  /** Продьюсер может создавать свою рекламу. */
  val PRODUCER: T = new Val("p") {
    override def longName = "producer"
  }

  /** Ресивер может отображать в выдаче и просматривать в ЛК рекламу других участников, которые транслируют свою
    * рекламу ему через receivers. Ресивер также может приглашать новых участников. */
  val RECEIVER: T = new Val("r") {
    override def longName = "receiver"
  }

}
