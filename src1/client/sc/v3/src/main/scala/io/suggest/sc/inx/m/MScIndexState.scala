package io.suggest.sc.inx.m

import diode.FastEq
import io.suggest.dev.MScreen
import io.suggest.geo.MGeoPoint

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.07.17 10:32
  * Description: Модель состояния "индекса" выдачи, т.е. базовые параметры состояния выдачи в целом.
  */
object MScIndexState {

  /** Поддержка FastEq для классов [[MScIndexState]]. */
  implicit object MScIndexStateFastEq extends FastEq[MScIndexState] {
    override def eqv(a: MScIndexState, b: MScIndexState): Boolean = {
      (a.screen eq b.screen) &&
        (a.rcvrIds eq b.rcvrIds) &&
        (a.geoPoint eq b.geoPoint)
    }
  }

}


/** Класс модели состояния индекса выдачи.
  *
  * @param screen Данные по текущему экрану устройства, под которое отрендерена выдача.
  * @param rcvrIds id текущего отображаемого узла в начале списка.
  *                Затем "предыдущие" узлы, если есть.
  * @param geoPoint Текущая гео-точка выдачи.
  */
case class MScIndexState(
                          screen          : MScreen,
                          rcvrIds         : List[String]        = Nil,
                          geoPoint        : Option[MGeoPoint]   = None
                        ) {

  // val или lazy val, т.к. часто нужен инстанс именно текущего узла.
  // А т.к. это "часто" завязано на посторонние FastEq[?], то следует юзать тут val вместо def.
  lazy val currRcvrId = rcvrIds.headOption

  def withScreen(screen: MScreen ) = copy( screen = screen )
  def withRcvrNodeId( rcvrNodeId: List[String] ) = copy( rcvrIds = rcvrNodeId )
  def withGeoPoint( geoPoint: Option[MGeoPoint] ) = copy( geoPoint = geoPoint )

}