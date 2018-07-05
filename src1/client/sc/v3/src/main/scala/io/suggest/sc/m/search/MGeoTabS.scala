package io.suggest.sc.m.search

import diode.FastEq
import io.suggest.ueq.UnivEqUtil._
import io.suggest.sjs.leaflet.map.LMap
import japgolly.univeq.UnivEq
import io.suggest.ueq.MapsUnivEq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.07.18 21:15
  * Description: Модель, хранящая обобщённые данные гео-таба панели поиска,
  * которые НЕ нужны в компоненте SearchMapR.
  */
object MGeoTabS {

  implicit object MGeoTabSFastEq extends FastEq[MGeoTabS] {
    override def eqv(a: MGeoTabS, b: MGeoTabS): Boolean = {
      (a.mapInit  ===* b.mapInit) &&
        (a.nodesSearch ===* b.nodesSearch) &&
        (a.delay  ===* b.delay) &&
        (a.lmap   ===* b.lmap)
    }
  }

  implicit def univEq: UnivEq[MGeoTabS] = UnivEq.derive

}

/** Класс-контейнер данных гео-таба.
  *
  * @param mapInit Состояние компонента [[io.suggest.sc.v.search.SearchMapR]].
  * @param delay Опциональное состояние отложенной реакции на события карты.
  * @param lmap leaflet instance для воздействия напрямую на карта в обход в react-leaflet.
  *             Возможно, станет ненужным при использовании react context api (react-leaflet v2+).
  */
case class MGeoTabS(
                     mapInit         : MMapInitState,
                     nodesSearch     : MGeoTabSearchS        = MGeoTabSearchS.empty,
                     delay           : Option[MMapDelay]     = None,
                     lmap            : Option[LMap]          = None,
                   ) {

  def withMapInit(mapr: MMapInitState)              = copy( mapInit = mapr )
  def withNodesSearch(nodesSearch: MGeoTabSearchS)  = copy( nodesSearch = nodesSearch )
  def withDelay(delay: Option[MMapDelay])           = copy( delay = delay )
  def withLmap(lmap: Option[LMap])                  = copy( lmap = lmap )


  /** Дедубликация кода сброса значения this.mapInit.loader. */
  def resetMapLoader: MGeoTabS = {
    withMapInit(
      mapInit
        .withLoader( None )
    )
  }

}
