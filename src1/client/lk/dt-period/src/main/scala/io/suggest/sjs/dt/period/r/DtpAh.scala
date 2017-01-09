package io.suggest.sjs.dt.period.r

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.common.empty.OptionUtil
import io.suggest.dt.MAdvPeriod
import io.suggest.sjs.common.dt.{JsDateUtil, MYmdJs}
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.WarnMsgs
import io.suggest.sjs.dt.period.m.{DtpInputFns, SetDateStartEnd, SetQap}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.12.16 13:10
  * Description: Date Period Action handler (diode)
  */
class DtpAh[M](
                modelRW       : ModelRW[M, MAdvPeriod],
                priceUpdateFx : Effect
              )
  extends ActionHandler(modelRW)
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Выставление нового значения quick adv period.
    case SetQap(qap2) =>
      val v0 = value
      val oldVal = v0.quickAdvPeriod
      if (qap2 == oldVal) {
        noChange
      } else {
        val customDates2 = OptionUtil.maybe(qap2.isCustom) {
          // Переключение на custom-режим. Сгенерить корректные даты автоматом.
          MAdvPeriod.toRange(v0)( JsDateUtil.JsDateHelper )
        }

        val v2 = v0.copy(
          quickAdvPeriod  = qap2,
          customRange     = customDates2
        )

        updated(v2, priceUpdateFx)
      }

    // Замена значения кастомной даты.
    case s: SetDateStartEnd =>
      val v0 = value
      v0.customRange.fold {
        LOG.warn( WarnMsgs.DATE_RANGE_FIELD_CHANGED_BUT_NO_CURRENT_RANGE_VAL )
        noChange

      } { oldRange =>
        // Нужно распарсить новую строку с датой и выставить её в нужное поле.
        val ymd2 = MYmdJs.parse(s.ymdStr).get

        val range2 = oldRange.copy(
          dateStart = if (s.fn == DtpInputFns.start) ymd2 else oldRange.dateStart,
          dateEnd   = if (s.fn == DtpInputFns.end) ymd2 else oldRange.dateEnd
        )

        updated( v0.withCustomRange(Some(range2)), priceUpdateFx )
      }
  }

}
