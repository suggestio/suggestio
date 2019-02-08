package io.suggest.sc.m.inx

import diode.FastEq
import io.suggest.geo.MGeoPoint
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens
import scalaz.NonEmptyList

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
      (a.switchAsk ===* b.switchAsk) &&
      (a.views ===* b.views)
    }
  }

  @inline implicit def univEq: UnivEq[MScIndexState] = UnivEq.derive

  val generation = GenLens[MScIndexState](_.generation)
  val switchAsk  = GenLens[MScIndexState](_.switchAsk)
  val views      = GenLens[MScIndexState](_.views)

}


/** Класс модели состояния индекса выдачи.
  *
  * @param generation Random seed выдачи.
  * @param rcvrId id текущего отображаемого узла в начале списка.
  * @param inxGeoPoint Гео-точка текущего загруженного индекса.
  *                    Обычно совпадает с центром гео-карты, но не всегда.
  * @param switchAsk Состояния на-экранного вопроса на тему переключения в новый узел.
  */
case class MScIndexState(
                          generation      : Long                      = System.currentTimeMillis(),
                          switchAsk       : Option[MInxSwitchAskS]    = None,
                          views           : NonEmptyList[MIndexView]  = NonEmptyList( MIndexView.empty ),
                        ) {

  def rcvrId: Option[String] = views.head.rcvrId
  def inxGeoPoint: Option[MGeoPoint] = views.head.inxGeoPoint
  lazy val prevNodeOpt = views.tail.headOption

  def withGeneration(generation: Long)            = copy( generation = generation )
  def withSwitchAsk( switchAsk: Option[MInxSwitchAskS] ) = copy( switchAsk = switchAsk )
  def withViews(views: NonEmptyList[MIndexView]) = copy(views = views)

}
