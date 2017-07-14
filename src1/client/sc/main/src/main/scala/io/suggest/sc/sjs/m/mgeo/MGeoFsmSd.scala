package io.suggest.sc.sjs.m.mgeo

import io.suggest.common.empty.EmptyProduct
import io.suggest.geo.MGeoLoc
import io.suggest.sjs.common.fsm.SjsFsm

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.04.16 16:06
  * Description: Контейнер данных состояния.
  *
  * @param watchers id текущего гео-вотчера на стороне браузера.
  * @param subscribers Карта подписчиков.
  */
case class MGeoFsmSd(
  watchers    : Map[GlWatchType, MglWatcher]  = Map.empty,
  subscribers : Map[SjsFsm, SubscriberData]   = Map.empty,
  suppressor  : Option[Suppressor]            = None
)


/** Описания одного активного гео-ватчера.
  *
  * @param watchId id watcher'а на стороне браузера, если есть.
  * @param lastPos Данные последней геолокации.
  */
case class MglWatcher(
  watchId    : Option[Int]      = None,
  lastPos    : Option[MGeoLoc]  = None
)
  extends EmptyProduct


/** Доп.данные подписки.
  *
  * @param minWatch Минимальный интересующий источник, по сути тут характеризуется интересующая точность.
  * @param withErrors Слать ли ошибки геолокации?
  */
case class SubscriberData(
  minWatch     : GlWatchType      = GlWatchTypes.min,
  withErrors   : Boolean          = false
) {

  def +(other: SubscriberData): SubscriberData = {
    copy(
      minWatch    = Seq(minWatch, other.minWatch).min,
      withErrors  = withErrors || other.withErrors
    )
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
  timerId   : Int,
  generation: Long,
  minWatch  : GlWatchType
)
