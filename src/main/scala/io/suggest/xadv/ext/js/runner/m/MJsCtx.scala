package io.suggest.xadv.ext.js.runner.m

import io.suggest.adv.ext.model.ctx.MJsCtxFieldsT

import scala.scalajs.js
import scala.scalajs.js.WrappedDictionary

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 13:48
 * Description: Состояние запроса. Через этого также происходит обмен данными.
 */

object MJsCtx extends MJsCtxFieldsT with FromStringT {

  override type T = MJsCtx

  def fromDyn(dyn: js.Dynamic): MJsCtx = {
    val d = dyn.asInstanceOf[js.Dictionary[js.Dynamic]] : WrappedDictionary[js.Dynamic]
    MJsCtx(
      action = MAskActions.withName( d.get(ACTION_FN).get.toString ),
      mads = d.get(ADS_FN).map {
          _.asInstanceOf[js.Array[js.Dynamic]]
            .toSeq
            .map(MAdCtx.fromDyn)
        }
        .getOrElse(Seq.empty),
      service = MServiceInfo.fromDyn(d(SERVICE_FN)),
      domains = d.get(DOMAIN_FN)
        .map { _.asInstanceOf[js.Array[String]].toSeq }
        .getOrElse(Nil)
      // status и error игнорим, ибо они только исходящие
    )
  }

}


import MJsCtx._


/** Минимальный интерфейс контекста. */
trait MJsCtxT {
  /** Текущее действо. */
  def action  : MAskAction

  /** Текущие карточки для обработки. */
  def mads    : Seq[MAdCtx]

  /** Текущий сервис и инфа по нему. */
  def service : MServiceInfo

  /** Домены, которых касается запрос. */
  def domains : Seq[String]

  /** Статус исполнения результата. */
  def status  : Option[MAnswerStatus]

  /** Инфа по случившийся ошибке. */
  def error   : Option[MErrorInfoT]

  /** Произвольные данные, выставляемые адаптером в рамках текущего запроса. */
  def custom  : Map[String, String]

  def toJson: js.Dynamic = {
    val lit = js.Dynamic.literal()
    lit.updateDynamic(ACTION_FN)(action.strId)
    if (mads.nonEmpty)
      lit.updateDynamic(ADS_FN)(mads.map(_.toJson))
    lit.updateDynamic(SERVICE_FN)(service.toJson)
    if (domains.nonEmpty)
      lit.updateDynamic(DOMAIN_FN)(domains)
    if (status.nonEmpty)
      lit.updateDynamic(STATUS_FN)(status.get.jsStr)
    if (custom.nonEmpty)
      lit.updateDynamic(CUSTOM_FN)(customJson)
    lit
  }

  def customJson: js.Dynamic = {
    val lit = js.Dynamic.literal()
    custom foreach {
      case (k, v) => lit.updateDynamic(k)(v)
    }
    lit
  }

}

/** Дефолтовая реализация контекста. */
case class MJsCtx(
  action  : MAskAction,
  mads    : Seq[MAdCtx],
  service : MServiceInfo,
  domains : Seq[String],
  status  : Option[MAnswerStatus] = None,
  error   : Option[MErrorInfoT] = None,
  custom  : Map[String, String] = Map.empty
) extends MJsCtxT


import io.suggest.adv.ext.model.ctx.MErrorInfo._


trait MErrorInfoT {
  def msg: String
  def args: Seq[Any]

  def toJson: js.Dynamic = {
    val lit = js.Dynamic.literal()
    lit.updateDynamic(MSG_FN)(msg)
    lit.updateDynamic(ARGS_FN)(args)
    lit
  }
}

case class MErrorInfo(
  msg   : String,
  args  : Seq[Any] = Seq.empty
) extends MErrorInfoT

