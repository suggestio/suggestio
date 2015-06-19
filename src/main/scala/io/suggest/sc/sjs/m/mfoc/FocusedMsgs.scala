package io.suggest.sc.sjs.m.mfoc

import io.suggest.sc.sjs.m.mfsm.IFsmMsg
import io.suggest.sc.sjs.m.msrv.foc.find.IMFocAds

// Команды для управления focused-выдачей.

/** Команда перехода на следующей карточке (вправо). */
case object Next extends IFsmMsg

/** Команда перехода к предыдущей карточке (влево). */
case object Prev extends IFsmMsg

/** Команда закрытия focused-выдачи. */
case object Close extends IFsmMsg

/** Команда перехода на указанную карточку. */
case class GoTo(index: Int) extends IFsmMsg

/** Событие получения ответа от сервера с порцией focused-карточек. */
case class FadsReceived(fads: IMFocAds) extends IFsmMsg
