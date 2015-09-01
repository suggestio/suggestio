package io.suggest.sc.sjs.m.mgeo

import io.suggest.sc.sjs.m.mfsm.{IFsmMsgCompanion, IFsmMsg}
import io.suggest.sjs.common.geo.{BssAccuracy, IHighAccuracy}
import org.scalajs.dom.{PositionError, Position}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.08.15 16:03
 * Description: Сигналы для FSM для передачи данных геолокации.
 */
trait IGeoSignal extends IFsmMsg

/** Интерфейс для сигналов с полученными данными геолокации. */
trait IGeoLocSignal extends IGeoSignal with IHighAccuracy {
  def data: Position
}

/** Получены данные по неточной геолокации. */
case class BssGeoLocSignal(override val data: Position)
  extends IGeoLocSignal
  with BssAccuracy
object BssGeoLocSignal
  extends IFsmMsgCompanion[Position]



/** Интерфейс для сигналов с ошибками геолокации. */
trait IGeoErrorSignal extends IGeoSignal with IHighAccuracy {
  def error: PositionError
}


/** Ошибка получения данных геолокации. */
case class BssGeoErrorSignal(override val error: PositionError)
  extends IGeoErrorSignal
  with BssAccuracy
object BssGeoErrorSignal
  extends IFsmMsgCompanion[PositionError]



/** Сигнал от таймера геолокации. */
case object GeoTimeout
  extends IGeoSignal
