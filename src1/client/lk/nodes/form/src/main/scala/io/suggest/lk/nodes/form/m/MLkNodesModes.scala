package io.suggest.lk.nodes.form.m

import enumeratum.values.{StringEnum, StringEnumEntry}
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.09.2020 14:42
  * Description: Модель режимов работы формы lk-nodes.
  * Изначально, для описания состояния использовался флаг isAdv, имеющий два boolean-значения.
  */
object MLkNodesModes extends StringEnum[MLkNodesMode] {

  case object NodesManage extends MLkNodesMode( "m" )

  case object AdvInNodes extends MLkNodesMode( "a" )

  override def values = findValues

}


sealed abstract class MLkNodesMode( override val value: String ) extends StringEnumEntry

object MLkNodesMode {
  @inline implicit def univEq: UnivEq[MLkNodesMode] = UnivEq.derive
}