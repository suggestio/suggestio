package io.suggest.sc.model.search

import diode.FastEq
import io.suggest.sc.view.search.SearchCss
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

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
      (a.css    ===* b.css) &&
      (a.found  ===* b.found) &&
      (a.data   ===* b.data)
    }
  }

  @inline implicit def univEq: UnivEq[MGeoTabS] = UnivEq.derive

  val mapInit = GenLens[MGeoTabS](_.mapInit)
  def css     = GenLens[MGeoTabS](_.css)
  def found   = GenLens[MGeoTabS](_.found)
  val data    = GenLens[MGeoTabS](_.data)


  implicit class GeoTabExt( private val m: MGeoTabS ) extends AnyVal {

    /** Дедубликация кода сброса значения this.mapInit.loader. */
    def resetMapLoader: MGeoTabS = {
      mapInit
        .andThen( MMapInitState.loader )
        .replace(None)(m)
    }

    /** В mapInit.rcvrs лежит закэшированная карта или нет? */
    def isRcvrsEqCached: Boolean = {
      m.mapInit.rcvrs.exists { rcvrs0 =>
        m.data.rcvrsCache
          .exists(_ ===* rcvrs0)
      }
    }

    /** Вернуть ресиверы, но только если не кэшированные. */
    def rcvrsNotCached = {
      m.mapInit.rcvrs
        .filter { _ =>
          // Если в значении лежат данные из кэша ресиверов карты, то отсеять содержимое безусловно.
          !isRcvrsEqCached
        }
    }

  }

}

/** Класс-контейнер данных гео-таба.
  *
  * @param mapInit Состояние компонента SearchMapR.
  * @param data    Контейнер данных вкладки, используемых только в контроллерах. Не участвуют в рендере.
  * @param found   Данные для списка найденных узлов, или инстанс empty, когда поиск не активен.
  */
case class MGeoTabS(
                     mapInit    : MMapInitState,
                   // TODO Поле было на уровне MScSearch, но унесено сюда, чтобы GeoTabAh не перепиливать. После унификации tags tab, можно будет подумать ещё раз на этим.
                     css        : SearchCss,
                     found      : MNodesFoundS       = MNodesFoundS.empty,
                     data       : MGeoTabData        = MGeoTabData.empty,
                   )
