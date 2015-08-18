package io.suggest.sc.sjs.m.mfoc

import io.suggest.sc.sjs.m.mfsm.IFsmMsg
import io.suggest.sc.sjs.m.msrv.foc.find.IMFocAds

// Команды для управления focused-выдачей.

/** Команда перехода на следующей карточке (вправо). */
case object Next extends IFsmMsg

/** Команда перехода к предыдущей карточке (влево). */
case object Prev extends IFsmMsg

/** Сигнал клика по кнопке скрытия focused-выдачи. */
case object CloseBtnClick extends IFsmMsg

/** Внутренний сигнал о завершении сокрытия focused-выдачи. */
case object FocRootDisappeared extends IFsmMsg

/** Сигнал по логотипу продьюсера focused-выдачи. */
case object ProducerLogoClick extends IFsmMsg

/** Внутренний сигнал о завершении появления на экране focused-выдачи. */
case object FocRootAppeared extends IFsmMsg
