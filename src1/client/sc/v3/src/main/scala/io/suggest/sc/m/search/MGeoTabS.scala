package io.suggest.sc.m.search

import diode.FastEq
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.07.18 21:15
  * Description: Модель, хранящая обобщённые данные гео-таба панели поиска.
  */
object MGeoTabS {

  implicit object MGeoTabSFastEq extends FastEq[MGeoTabS] {
    override def eqv(a: MGeoTabS, b: MGeoTabS): Boolean = {
      (a.mapInit  ===* b.mapInit) &&
        (a.data ===* b.data)
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
                     mapInit  : MMapInitState,
                   // TODO Добавить сюда Option[MNodesFoundS] для рендера списка NodesFound рядом с картой. Через lazy val это реализовать не очень и не получается.
                     data     : MGeoTabData        = MGeoTabData.empty,
                   ) {

  def withMapInit(mapr: MMapInitState) = copy( mapInit = mapr )
  def withData(data: MGeoTabData) = copy( data = data )


  /** Дедубликация кода сброса значения this.mapInit.loader. */
  def resetMapLoader: MGeoTabS = {
    withMapInit(
      mapInit
        .withLoader( None )
    )
  }

  /** В mapInit.rcvrs лежит закэшированная карта или нет? */
  def isRcvrsEqCached: Boolean = {
    mapInit.rcvrs.exists { rcvrs0 =>
      data.rcvrsCache
        .exists(_ ===* rcvrs0)
    }
  }

  /** Вернуть ресиверы, но только если не кэшированные. */
  def rcvrsNotCached = {
    mapInit.rcvrs
      .filter { _ =>
        // Если в значении лежат данные из кэша ресиверов карты, то отсеять содержимое безусловно.
        !isRcvrsEqCached
      }
  }

}
