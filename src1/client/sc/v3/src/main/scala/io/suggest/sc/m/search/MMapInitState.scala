package io.suggest.sc.m.search

import diode.FastEq
import diode.data.Pot
import io.suggest.geo.{MGeoLoc, MGeoPoint}
import io.suggest.maps.m.MMapS
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.sjs.leaflet.map.LMap
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.MapsUnivEq._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.11.17 22:24
  * Description: Модель промежуточной инициализации поисковой карты выдачи.
  *
  * Из-за проблем с инициализацией Leaflet за экраном, используется инициализация в два шага:
  * - Карта вообще отсутствует.
  * - Карта инициализируется.
  * - Карта готова к работе.
  *
  * Эта модель предусматривает обработку всех шагов.
  */
object MMapInitState {

  implicit object MMapInitStateFastEq extends FastEq[MMapInitState] {
    override def eqv(a: MMapInitState, b: MMapInitState): Boolean = {
      (a.state      ===* b.state) &&
        (a.ready     ==* b.ready) &&
        (a.rcvrsGeo ===* b.rcvrsGeo) &&
        (a.delay    ===* b.delay) &&
        (a.loader   ===* b.loader) &&
        (a.lmap     ===* b.lmap) &&
        (a.userLoc  ===* b.userLoc)
    }
  }


  implicit def univEq: UnivEq[MMapInitState] = UnivEq.derive

}


/** Класс модели-контейнера инициализации карты.
  *
  * @param state Состояние карты, даже если она не инициализирована.
  * @param ready Запущена ли карта?
  * @param rcvrsGeo Гео-данные ресиверов.
  * @param delay Опциональное состояние отложенной реакции на события карты.
  * @param loader Координата для отображения маркера текущей подгрузки выдачи.
  * @param lmap leaflet instance для воздействия напрямую на карта в обход в react-leaflet.
  *                  Возможно, станет ненужным при использовании react context api (react-leaflet v2+).
  * @param userLoc Геолокация юзера, записанная в состояния.
  */
case class MMapInitState(
                          state           : MMapS,
                          ready           : Boolean               = false,
                          rcvrsGeo        : Pot[MGeoNodesResp]    = Pot.empty,
                          delay           : Option[MMapDelay]     = None,
                          loader          : Option[MGeoPoint]     = None,
                          lmap            : Option[LMap]          = None,
                          userLoc         : Option[MGeoLoc]       = None,
                        ) {

  def withState(state: MMapS)                       = copy( state = state )
  def withReady(ready: Boolean)                     = copy( ready = ready )
  def withRcvrsGeo(rcvrsGeo: Pot[MGeoNodesResp])    = copy( rcvrsGeo = rcvrsGeo )
  def withDelay(delay: Option[MMapDelay])           = copy( delay = delay )
  def withLoader(loader: Option[MGeoPoint])         = copy( loader = loader )
  def withLInstance(lmap: Option[LMap])             = copy( lmap = lmap )
  def withUserLoc(userLoc: Option[MGeoLoc])         = copy( userLoc = userLoc )

}
