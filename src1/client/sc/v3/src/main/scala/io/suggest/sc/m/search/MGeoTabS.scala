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
        (a.found  ===* b.found) &&
        (a.data   ===* b.data)
    }
  }

  implicit def univEq: UnivEq[MGeoTabS] = UnivEq.derive

}

/** Класс-контейнер данных гео-таба.
  *
  * @param mapInit Состояние компонента [[io.suggest.sc.v.search.SearchMapR]].
  * @param data Контейнер данных вкладки, используемых только в контроллерах. Не участвуют в рендере.
  * @param found Данные для списка найденных узлов, или инстанс empty, когда поиск не активен.
  */
case class MGeoTabS(
                     mapInit    : MMapInitState,
                     found      : MNodesFoundS       = MNodesFoundS.empty,
                     data       : MGeoTabData        = MGeoTabData.empty,
                   ) {

  def withMapInit(mapr: MMapInitState) = copy( mapInit = mapr )
  def withData(data: MGeoTabData) = copy( data = data )
  def withFound(found: MNodesFoundS) = copy(found = found)


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
