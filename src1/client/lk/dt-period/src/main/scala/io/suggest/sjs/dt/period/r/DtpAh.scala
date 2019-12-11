package io.suggest.sjs.dt.period.r

import com.momentjs.Moment
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.dt.interval.{QuickAdvIsoPeriod, QuickAdvPeriods}
import io.suggest.dt.{IPeriodInfo, MAdvPeriod, MYmd}
import io.suggest.sjs.common.dt.JsDateUtil
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.dt.period.m.{DtpInputFns, SetDateStartEnd, SetQap}
import io.suggest.dt.moment.MomentJsUtil.Implicits._
import io.suggest.msg.ErrorMsgs

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
      val oldVal = v0.info.quickAdvPeriod
      if (qap2 == oldVal) {
        noChange
      } else {
        // Подготовить изменения в info-поле...
        val info2: IPeriodInfo = qap2 match {
          case isoPeriod: QuickAdvIsoPeriod =>
            v0.info.withIsoPeriod(isoPeriod)

          case QuickAdvPeriods.Custom =>
            // Переключение на custom-режим. Сгенерить корректные даты автоматом.
            v0.info.withCustomRange(
              v0.info.rangeYmd( JsDateUtil.JsDateHelper )
            )
        }

        val v2 = v0.withInfo( info2 )
        updated(v2, priceUpdateFx)
      }

    // Замена значения кастомной даты.
    case s: SetDateStartEnd =>
      val v0 = value
      v0.info.customRangeOpt.fold {
        LOG.warn( ErrorMsgs.DATE_RANGE_FIELD_CHANGED_BUT_NO_CURRENT_RANGE_VAL )
        noChange

      } { oldRange =>
        // Нужно выставить новую строку с датой в состояние.
        val now = Moment()

        val start1 = if (s.fn == DtpInputFns.start) s.moment else oldRange.dateStart.to[Moment]
        val end1   = if (s.fn == DtpInputFns.end) s.moment else oldRange.dateEnd.to[Moment]

        // Нужно убедится, что дата начала идёт ПЕРЕД датой окончания.
        val start2 = Moment.max(now, start1)

        // Надо убедится, что дата окончания хотя бы на день впереди даты начала.
        val tomorrow = Moment(now).tomorrow
        val startTomorrow = Moment(start2).tomorrow
        val end2 = Moment.max(end1, tomorrow, startTomorrow)

        val range2 = oldRange.copy(
          dateStart = MYmd.from(start2),
          dateEnd   = MYmd.from(end2)
        )

        val v2 = v0.withInfo(
          v0.info.withCustomRange( range2 )
        )

        updated( v2, priceUpdateFx )
      }
  }

}
