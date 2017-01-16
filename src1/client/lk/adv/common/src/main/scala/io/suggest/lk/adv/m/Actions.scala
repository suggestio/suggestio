package io.suggest.lk.adv.m

import io.suggest.bill.MGetPriceResp
import io.suggest.sjs.common.spa.DAction

/** diode-экшены для react-common частей. */

/** Сигнал-экшен для diode-системы об изменении состояния галочки su-бесплатного размещения. */
case class SetAdv4Free(checked: Boolean) extends DAction


/** Команда к пересчёту ценника размещения. */
case object ResetPrice extends DAction

/** Выставить новое значение стоимости размещения.
  * @param ts Timestamp запуска запроса.
  * @param resp Ответ сервера, готовый к употреблению.
  */
case class SetPrice(resp: MGetPriceResp, ts: Long) extends DAction
