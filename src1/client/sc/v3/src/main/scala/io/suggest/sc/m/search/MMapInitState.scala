package io.suggest.sc.m.search

import diode.FastEq
import diode.data.Pot
import io.suggest.geo.MGeoPoint
import io.suggest.maps.m.MMapS
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
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
        (a.loader   ===* b.loader)
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
  */
case class MMapInitState(
                          state           : MMapS,
                          ready           : Boolean               = false,
                          rcvrsGeo        : Pot[MGeoNodesResp]    = Pot.empty,
                          delay           : Option[MMapDelay]     = None,
                          loader          : Option[MGeoPoint]     = None,
                        ) {

  def withState(state: MMapS)                       = copy( state = state )
  def withReady(ready: Boolean)                     = copy( ready = ready )
  def withRcvrsGeo(rcvrsGeo: Pot[MGeoNodesResp])    = copy( rcvrsGeo = rcvrsGeo )
  def withDelay(delay: Option[MMapDelay])           = copy( delay = delay )
  def withLoader(loader: Option[MGeoPoint])         = copy( loader = loader )

}
