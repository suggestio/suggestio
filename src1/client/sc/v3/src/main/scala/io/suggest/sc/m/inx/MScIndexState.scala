package io.suggest.sc.m.inx

import diode.FastEq
import io.suggest.geo.MGeoPoint
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.07.17 10:32
  * Description: Модель состояния "индекса" выдачи, т.е. базовые параметры состояния выдачи в целом.
  */
object MScIndexState {

  def empty = apply()

  /** Поддержка FastEq для классов [[MScIndexState]]. */
  implicit object MScIndexStateFastEq extends FastEq[MScIndexState] {
    override def eqv(a: MScIndexState, b: MScIndexState): Boolean = {
      (a.generation ==* b.generation) &&
      (a.rcvrIds ===* b.rcvrIds) &&
      (a.inxGeoPoint ===* b.inxGeoPoint) &&
      (a.switchAsk ===* b.switchAsk)
    }
  }

  @inline implicit def univEq: UnivEq[MScIndexState] = UnivEq.derive

}


/** Класс модели состояния индекса выдачи.
  *
  * @param generation Random seed выдачи.
  * @param rcvrIds id текущего отображаемого узла в начале списка.
  *                Затем "предыдущие" узлы, если есть.
  * @param inxGeoPoint Гео-точка текущего загруженного индекса.
  *                    Обычно совпадает с центром гео-карты, но не всегда.
  * @param switchAsk Состояния на-экранного вопроса на тему переключения в новый узел.
  */
case class MScIndexState(
                          generation      : Long                      = System.currentTimeMillis(),
                          rcvrIds         : List[String]              = Nil,
                          inxGeoPoint     : Option[MGeoPoint]         = None,
                          switchAsk       : Option[MInxSwitchAskS]    = None,
                        ) {

  // val или lazy val, т.к. часто нужен инстанс именно текущего узла.
  // А т.к. это "часто" завязано на посторонние FastEq[?], то следует юзать тут val вместо def.
  lazy val currRcvrId = rcvrIds.headOption

  def withGeneration(generation: Long)            = copy( generation = generation )
  def withRcvrNodeId( rcvrNodeId: List[String] )  = copy( rcvrIds = rcvrNodeId )
  def withInxGeoPoint( inxGeoPoint: Option[MGeoPoint] ) = copy( inxGeoPoint = inxGeoPoint )
  def withSwitchAsk( switchAsk: Option[MInxSwitchAskS] ) = copy( switchAsk = switchAsk )

}
