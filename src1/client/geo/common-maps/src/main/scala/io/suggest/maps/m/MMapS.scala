package io.suggest.maps.m

import diode.FastEq
import io.suggest.geo.MGeoPoint
import io.suggest.maps.MMapProps
import io.suggest.spa.FastEqUtil
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.01.17 18:55
  * Description: Клиентская модель данных состояния простейшей карты.
  * Изначально под такой картой подразумевалась leaflet-карта с кнопкой "где я?",
  * возможностью перемещения и масштабирования этой карты.
  */
object MMapS {

  lazy val CenterZoomFeq = {
    FastEqUtil[MMapS] { (a, b) =>
      (a.center ===* b.center) &&
      (a.zoom ==* b.zoom)
    }
  }

  implicit object MMapSFastEq4Map extends FastEq[MMapS] {
    override def eqv(a: MMapS, b: MMapS): Boolean = {
      (a.zoom ==* b.zoom) &&
      (a.centerInit ===* b.centerInit) &&
      // Не реагировать на изменение реальной координаты, т.к. сюда идёт запись только из callback'ов карты.
      //(a.centerReal ===* b.centerReal) &&
      (a.locationFound ===* b.locationFound)
    }
  }


  @inline implicit def univEq: UnivEq[MMapS] = UnivEq.derive

  def apply(mapProps: MMapProps): MMapS = {
    MMapS(
      zoom        = mapProps.zoom,
      centerInit  = mapProps.center
    )
  }


  def zoom          = GenLens[MMapS](_.zoom)
  def centerInit    = GenLens[MMapS](_.centerInit)
  def centerReal    = GenLens[MMapS](_.centerReal)
  def locationFound = GenLens[MMapS](_.locationFound)


  implicit final class MMapSOpsExt( private val mmapS: MMapS ) extends AnyVal {

    def center: MGeoPoint =
      mmapS.centerReal getOrElse mmapS.centerInit

    def withCenterInitReal(centerInit: MGeoPoint, centerReal: Option[MGeoPoint] = None) =
      mmapS.copy(centerInit = centerInit, centerReal = centerReal)


    /** Кросс-платформенные данные карты, для экспорта на сервер. */
    def toMapProps: MMapProps = {
      MMapProps(
        center = mmapS.center,
        zoom   = mmapS.zoom,
      )
    }


    def isCenterRealNearInit: Boolean = {
      mmapS.centerReal.fold(true) { centerRealMgp =>
        mmapS.centerInit ~= centerRealMgp
      }
    }

  }

}


/**
  * Класс клиентской модели данных по карте.
  * @param locationFound Состояние геолокации и реакции на неё:
  *                      true уже карта была отцентрована по обнаруженной геолокации.
  *                      false началась геолокация, нужно отцентровать карту по опредённым координатам.
  *                      None Нет ни геолокации, ничего.
  * @param centerReal Фактический центр карты после перемещения.
  *                   Запись напрямую в MMapProps приводит к плохому поведению leaflet,
  *                   поэтому фактический центр сохраняется отдельно от центра инициализации.
  */
final case class MMapS(
                        zoom          : Int,
                        centerInit    : MGeoPoint,
                        centerReal    : Option[MGeoPoint]   = None,
                        locationFound : Option[Boolean]     = None,
                      )
