package io.suggest.sc.m.search

import diode.FastEq
import diode.data.Pot
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.sjs.leaflet.map.LMap
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.MapsUnivEq._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.07.18 21:44
  * Description: Модель, куда сваливаются доп.данные панели гео-поиска (карты).
  * Эти данные НЕ участвуют в рендере панели, они свалены отдельно от моделей всяких компонентов.
  *
  * Неявно-пустая модель.
  */
object MGeoTabData {

  def empty = apply()

  implicit object MGeoTabDataFastEq extends FastEq[MGeoTabData] {
    override def eqv(a: MGeoTabData, b: MGeoTabData): Boolean = {
      (a.rcvrsCache ===* b.rcvrsCache) &&
      (a.delay ===* b.delay) &&
      (a.lmap ===* b.lmap) &&
      (a.selTagIds ===* b.selTagIds)
    }
  }

  @inline implicit def univEq: UnivEq[MGeoTabData] = UnivEq.derive

  def rcvrsCache  = GenLens[MGeoTabData](_.rcvrsCache)
  val delay       = GenLens[MGeoTabData](_.delay)
  def lmap        = GenLens[MGeoTabData](_.lmap)
  def selTagIds   = GenLens[MGeoTabData](_.selTagIds)

}


/** Контейнер модели состояния поиска узлов на карте.
  *
  * @param rcvrsCache Кэш полной карты ресиверов.
  * @param delay Опциональное состояние отложенной реакции на события карты.
  * @param lmap leaflet instance для воздействия напрямую на карта в обход в react-leaflet.
  *             Возможно, станет ненужным при использовании react context api (react-leaflet v2+).
  * @param selTagIds id выбранных тегов.
  *                  Для рендера используются после объединения с id тегов.
  */
case class MGeoTabData(
                        rcvrsCache      : Pot[MSearchRespInfo[MGeoNodesResp]]    = Pot.empty,
                        delay           : Option[MMapDelay]     = None,
                        lmap            : Option[LMap]          = None,
                        selTagIds       : Set[String]           = Set.empty,
                      )
