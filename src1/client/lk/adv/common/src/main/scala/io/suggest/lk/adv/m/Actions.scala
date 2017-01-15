package io.suggest.lk.adv.m

import io.suggest.bill.MGetPriceResp
import io.suggest.sjs.common.spa.DAction

/** diode-экшены для react-common частей. */

/** Сигнал-экшен для diode-системы об изменении состояния галочки su-бесплатного размещения. */
case class SetAdv4Free(checked: Boolean) extends DAction

/** Выставить новое значение стоимости размещения. */
case class SetPrice(pricing: MGetPriceResp) extends DAction
