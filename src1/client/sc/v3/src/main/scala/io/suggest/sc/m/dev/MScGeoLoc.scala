package io.suggest.sc.m.dev

import diode.FastEq
import diode.data.Pot
import io.suggest.common.empty.{EmptyProduct, OptionUtil}
import io.suggest.geo.{GeoLocType, MGeoLoc}
import io.suggest.sc.m.inx.MScSwitchCtx
import io.suggest.sjs.dom2.GeoLocWatchId_t
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import io.suggest.ueq.JsUnivEqUtil._
import monocle.macros.GenLens

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
  implicit object MScGeoLocFastEq extends FastEq[MScGeoLoc] {
    override def eqv(a: MScGeoLoc, b: MScGeoLoc): Boolean = {
      (a.watchers   ===* b.watchers) &&
      (a.suppressor ===* b.suppressor) &&
      (a.switch     ===* b.switch) &&
      (a.leafletLoc ===* b.leafletLoc)
    }
  }

  @inline implicit def univEq: UnivEq[MScGeoLoc] = UnivEq.derive

  def watchers    = GenLens[MScGeoLoc](_.watchers)
  def suppressor  = GenLens[MScGeoLoc](_.suppressor)
  def switch      = GenLens[MScGeoLoc](_.switch)
  def leafletLoc  = GenLens[MScGeoLoc](_.leafletLoc)

}


/** Класс модели состояния выдачи.
  *
  * @param watchers Наблюдение за геолокацией осуществяется подпиской на события.
  *                 Здесь инфа об активных подписках и данные для отписки.
  *                 Если пусто, то можно считать эту подсистему выключенной.
  */
case class MScGeoLoc(
                      watchers        : Map[GeoLocType, MGeoLocWatcher]     = Map.empty,
                      suppressor      : Option[Suppressor]                  = None,
                      switch          : MGeoLocSwitchS                      = MGeoLocSwitchS.empty,
                      leafletLoc      : Option[MGlSourceS]                  = None,
                    ) {

  /** Данные о текущем наиболее точном местоположении. */
  lazy val currentLocation: Option[(GeoLocType, MGeoLoc)] = {
    val elements = (for {
      (glType, watcher) <- watchers.iterator
      gl <- watcher.lastPos.toOption
    } yield {
      glType -> gl
    })
      .toSeq

    // Вернуть наиболее точный из элементов.
    OptionUtil.maybe( elements.nonEmpty ) {
      elements.maxBy( _._1.precision )
    }
  }

}


object MGeoLocSwitchS {
  def empty = apply()

  implicit object MGeoLocSwitchSFastEq extends FastEq[MGeoLocSwitchS] {
    override def eqv(a: MGeoLocSwitchS, b: MGeoLocSwitchS): Boolean = {
      (a.onOff        ===* b.onOff) &&
      (a.hardLock      ==* b.hardLock) &&
      (a.prevGeoLoc   ===* b.prevGeoLoc) &&
      (a.scSwitch     ===* b.scSwitch)
    }
  }
  @inline implicit def univEq: UnivEq[MGeoLocSwitchS] = UnivEq.derive

  def onOff = GenLens[MGeoLocSwitchS](_.onOff)
  def hardLock = GenLens[MGeoLocSwitchS](_.hardLock)
  def prevGeoLoc = GenLens[MGeoLocSwitchS](_.prevGeoLoc)
  def scSwitch = GenLens[MGeoLocSwitchS](_.scSwitch)

  implicit final class GlSwitchS( private val ss: MGeoLocSwitchS ) extends AnyVal {
    def withOutScSwitch: MGeoLocSwitchS =
      if (ss.scSwitch.isEmpty) ss else (scSwitch set None)(ss)
  }

}
/** Контейнер данных состояния "рубильника" геолокации.
  *
  * @param onOff Включена геолокация или выключена?
  *              Pot.empty: Бывает, что геолокация недоступна (нет поддержки вообще, голый http и т.д.).
  * @param hardLock Состояние on/off жестко задан пользователем?
  * @param prevGeoLoc Последняя геолокация с предшествующего сеанса геолокации, если есть.
  *                   При врЕменном прерывании геолокации (экран выключили, например) значение сохраняется сюда,
  *                   чтобы после узнать, изменилось ли местоположение с момента предыдущего состояния выдачи.
  * @param scSwitch Доп.состояние switch-контекста, которое сбрасывается после первого pub-сигнала.
  */
case class MGeoLocSwitchS(
                           onOff           : Pot[Boolean]                        = Pot.empty,
                           hardLock        : Boolean                             = false,
                           prevGeoLoc      : Option[MGeoLoc]                     = None,
                           scSwitch        : Option[MScSwitchCtx]                = None,
                         )


/** Модель данных об одном geo-watcher'е.
  *
  * @param watchId id для отписки от сообщений.
  * @param lastPos Данные по геопозиционированию, если доступны.
  *                Тут можно хранить ошибки позиционирования тоже.
  */
case class MGeoLocWatcher(
                           watchId    : Pot[GeoLocWatchId_t],
                           lastPos    : Pot[MGeoLoc]   = Pot.empty
                         )
  extends EmptyProduct

object MGeoLocWatcher {
  def lastPos = GenLens[MGeoLocWatcher](_.lastPos)
  def watchId = GenLens[MGeoLocWatcher](_.watchId)

  @inline implicit def univEq: UnivEq[MGeoLocWatcher] = UnivEq.derive
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
  @inline implicit def univEq: UnivEq[Suppressor] = UnivEq.derive
}
