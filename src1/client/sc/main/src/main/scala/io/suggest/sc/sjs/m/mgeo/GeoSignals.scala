package io.suggest.sc.sjs.m.mgeo

import io.suggest.geo.{GeoLocType, MGeoLoc, MGeoPoint}
import io.suggest.sjs.common.fsm.{IFsmMsg, IFsmMsgCompanion, SjsFsm}
import org.scalajs.dom.PositionError

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.08.15 16:03
 * Description: Сигналы для FSM для передачи данных геолокации.
 */
trait IGeoSignal extends IFsmMsg {

  /** Тип геолокации, от которой исходит сигнал. */
  def wtype: GeoLocType

  /** Сообщение об успехе? */
  def isSuccess: Boolean

}

/** Интерфейс компаньона геосигналов. */
trait IGeoSignalCompanion[X] {
  def apply(t: X, wtype: GeoLocType): IGeoSignal
}

trait IGeoLocSignal {
  def data: MGeoLoc
}


/** Сигнал о получении геолокации. */
case class GlLocation(override val data: MGeoLoc, override val wtype: GeoLocType)
  extends IGeoSignal
  with IGeoLocSignal
{
  override def isSuccess = true
}
object GlLocation extends IGeoSignalCompanion[MGeoLoc]


/** Сигнал об ошибке геолокации. */
case class GlError(error: PositionError, override val wtype: GeoLocType)
  extends IGeoSignal
{
  override def isSuccess = false
}
object GlError extends IGeoSignalCompanion[PositionError]


/** Сигнал от таймера ожидания геолокации. */
case object GeoTimeout
  extends IFsmMsg



/** Другие акторы уведомляют GeoLocFsm о необходимости уведомлять или перестать уведомлять их.
  *
  * @param receiver FSM, который необходимо уведомлять.
  * @param notifyZero Слать ли нулевое уведомление? Если true, то уже полученная геолокация будет послана.
  * @param data Параметры уведомлений.
  */
case class Subscribe(
  receiver    : SjsFsm,
  notifyZero  : Boolean,
  data        : SubscriberData = SubscriberData()
)
  extends IFsmMsg

/** Отказ от подписки на уведомления. */
case class UnSubscribe(
  receiver    : SjsFsm
)
  extends IFsmMsg


/** Сигнал для GeoLocFsm о резкой необходимости вернуть любой ответ с инфой по геолокации. */
case class GetAnyGl(to: SjsFsm) extends IFsmMsg

/** Сигнал об отсутствии геолокации. Испускается GeoLocFsm всем вопрошающим. */
case object GlUnknown extends IFsmMsg


/** Сигнал таймаута подавления "слабых" типов геолокации. */
case class SuppressTimeout(generation: Long) extends IFsmMsg
object SuppressTimeout extends IFsmMsgCompanion[Long]


/** Сигнал от MbFsm к ScFsm о смене текущей обозреваемой геолокации. */
case class NewGeoLoc(point: MGeoPoint) extends IFsmMsg
