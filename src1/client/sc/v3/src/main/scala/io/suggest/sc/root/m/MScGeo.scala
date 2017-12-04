package io.suggest.sc.root.m

import diode.FastEq
import io.suggest.geo.MGeoPoint
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.12.17 16:32
  * Description: Состояние компонента системы глобального позиционирования (глонасс, gps и т.д.).
  *
  * Модель является неявно-пустой, т.к. по сути всё в мире уже оборудовано поддержкой гео-позиционирования.
  */
object MScGeo {

  def empty = apply()

  /** Поддержка FastEq для инстансов [[MScGeo]]. */
  implicit object MScGeoFastEq extends FastEq[MScGeo] {
    override def eqv(a: MScGeo, b: MScGeo): Boolean = {
      (a.enabled ==* b.enabled) &&
      (a.loc ===* b.loc) &&
        (a.accuracyM ===* b.accuracyM)
    }
  }

  implicit def univEq: UnivEq[MScGeo] = UnivEq.derive

}


/** Класс модели состояния выдачи.
  *
  * @param enabled Активна ли система геолокации сейчас?
  * @param loc Точка локации.
  * @param accuracyM Точность гео-позиционировния, если известна.
  */
case class MScGeo(
                   enabled    : Boolean             = false,
                   // TODO Map[GeoLocType, <INFO>]
                   loc        : Option[MGeoPoint]   = None,
                   accuracyM  : Option[Double]      = None
                 )
{

  def withEnabled(enabled: Boolean) = copy(enabled = enabled)
  def withLocAccuracy(loc: Option[MGeoPoint], accuracyM: Option[Double] = None) = copy(loc = loc, accuracyM = accuracyM)

}
