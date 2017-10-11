package io.suggest.lk.adv.m

import io.suggest.adv.rcvr.RcvrKey
import io.suggest.bill.MGetPriceResp
import io.suggest.spa.DAction

import scala.util.Try

/** diode-экшены для react-common частей. */

/** Сигнал-экшен для diode-системы об изменении состояния галочки su-бесплатного размещения. */
case class SetAdv4Free(checked: Boolean) extends DAction


/** Команда к пересчёту ценника размещения. */
case object ResetPrice extends DAction

/** Выставить новое значение стоимости размещения.
  * @param ts Timestamp запуска запроса.
  * @param tryResp Ответ сервера, готовый к употреблению. Или ошибка.
  */
case class HandleGetPriceResp(tryResp: Try[MGetPriceResp], ts: Long) extends DAction

/** Сигнал к действиям по окончательному сабмиту всей формы на сервер... */
case object DoFormSubmit extends DAction
/** Сигнал результата "сабмита" формы. */
case class HandleFormSubmitResp(tryResp: Try[String]) extends DAction


/** Сигнал открытия инфы по узлу. */
case class OpenNodeInfoClick(rcvrKey: RcvrKey) extends DAction
