package io.suggest.sc.model.search

import diode.FastEq
import diode.data.Pot
import io.suggest.geo.{MGeoLoc, MGeoPoint}
import io.suggest.maps.MMapS
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

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
      (a.state            ===* b.state) &&
        (a.ready           ==* b.ready) &&
        (a.rcvrs          ===* b.rcvrs) &&
        (a.loader         ===* b.loader) &&
        (a.userLoc        ===* b.userLoc)
    }
  }

  @inline implicit def univEq: UnivEq[MMapInitState] = UnivEq.derive

  val state     = GenLens[MMapInitState](_.state)
  def ready     = GenLens[MMapInitState](_.ready)
  def rcvrs     = GenLens[MMapInitState](_.rcvrs)
  def loader    = GenLens[MMapInitState](_.loader)
  def userLoc   = GenLens[MMapInitState](_.userLoc)

}


/** Класс модели-контейнера инициализации карты.
  *
  * @param state Состояние карты, даже если она не инициализирована.
  * @param ready Запущена ли карта?
  * @param rcvrs Кэш с данными всех ресиверов.
  *              Закэшированные данные возвращаются на карту, когда поиск завершён.
  * @param loader Координата для отображения маркера текущей подгрузки выдачи.
  * @param userLoc Геолокация юзера, записанная в состояния.
  */
case class MMapInitState(
                          state           : MMapS,
                          ready           : Boolean               = false,
                          rcvrs           : Pot[MSearchRespInfo[MGeoNodesResp]]    = Pot.empty,
                          loader          : Option[MGeoPoint]     = None,
                          userLoc         : Option[MGeoLoc]       = None,
                        )
