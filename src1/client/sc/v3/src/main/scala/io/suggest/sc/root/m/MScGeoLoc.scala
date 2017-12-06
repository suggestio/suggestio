package io.suggest.sc.root.m

import diode.FastEq
import diode.data.Pot
import io.suggest.common.empty.EmptyProduct
import io.suggest.geo.{GeoLocType, MGeoLoc}
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
object MScGeoLoc {

  def empty = apply()

  /** Поддержка FastEq для инстансов [[MScGeoLoc]]. */
  implicit object MScGeoFastEq extends FastEq[MScGeoLoc] {
    override def eqv(a: MScGeoLoc, b: MScGeoLoc): Boolean = {
      (a.watchers   ===* b.watchers) &&
        (a.suppressor ===* b.suppressor)
    }
  }

  implicit def univEq: UnivEq[MScGeoLoc] = UnivEq.derive

}


/** Класс модели состояния выдачи.
  *
  * @param watchers Наблюдение за геолокацией осуществяется подпиской на события.
  *                 Здесь инфа об активных подписках и данные для отписки.
  *                 Если пусто, то можно считать эту подсистему выключенной.
  */
case class MScGeoLoc(
                   watchers     : Map[GeoLocType, MGeoLocWatcher]   = Map.empty,
                   suppressor   : Option[Suppressor]                = None
                 ) {

  def withWatchers(watchers: Map[GeoLocType, MGeoLocWatcher]) = copy(watchers = watchers)
  def withSuppressor(suppressor: Option[Suppressor]) = copy(suppressor = suppressor)

  /** Активна ли система геолокации сейчас? */
  def isEnabled = watchers.nonEmpty

}


/** Модель данных об одном geo-watcher'е.
  *
  * @param watchId id для отписки от сообщений.
  * @param lastPos Данные по геопозиционированию, если доступны.
  *                Тут можно хранить ошибки позиционирования тоже.
  */
case class MGeoLocWatcher(
                           watchId    : Option[Int],
                           lastPos    : Pot[MGeoLoc]   = Pot.empty
                         )
  extends EmptyProduct
{

  def withLastPos(lastPos: Pot[MGeoLoc]) = copy(lastPos = lastPos)
}
object MGeoLocWatcher {
  implicit def univEq: UnivEq[MGeoLocWatcher] = {
    import io.suggest.ueq.JsUnivEqUtil._
    UnivEq.derive
  }
}


/** Модель данных супрессора лишних сигналов.
  * Доминирование одного источника над остальными описывается здесь.
  *
  * @param timerId id таймера, который отсчитывает TTL супрессора.
  *                Например, если GPS доминирует, но спутники пропали надолго, то сработае таймер.
  * @param minWatch Текущий минимальный уровень уведомлений.
  * @param generation "Поколение" для защиты от ложных срабатываний уже отмененных таймеров.
  */
case class Suppressor(
                       timerId      : Int,
                       generation   : Long,
                       minWatch     : GeoLocType
                     )
object Suppressor {
  implicit def univEq: UnivEq[Suppressor] = UnivEq.derive
}
