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

  override def fromJson(dyn: js.Any): MJsCtx = {
    val d = dyn.asInstanceOf[js.Dictionary[js.Dynamic]] : WrappedDictionary[js.Dynamic]
    MJsCtx(
      action = MAskActions.withName( d.get(ACTION_FN).get.toString ),
      mads = d.get(ADS_FN)
        .map {
          _.asInstanceOf[js.Array[js.Dynamic]]
            .toSeq
            .map(MAdCtx.fromJson)
        }
        .getOrElse(Seq.empty),
      target = d.get(TARGET_FN)
        .map(MExtTarget.fromJson),
      service = MServiceInfo.fromJson(d(SERVICE_FN)),
      domains = d.get(DOMAIN_FN)
        .map { _.asInstanceOf[js.Array[String]].toSeq }
        .getOrElse(Nil),
      custom = d.get(CUSTOM_FN)
      // status и error игнорим, ибо они только исходящие
    )
  }

}


import MJsCtx._


/** Минимальный интерфейс контекста. */
trait MJsCtxT extends IToJsonDict {
  /** Текущее действо. */
  def action  : MAskAction

  /** Текущие карточки для обработки. */
  def mads    : Seq[MAdCtx]

  /** Текущий сервис и инфа по нему. */
  def service : MServiceInfo

  /** Домены, которых касается запрос. */
  def domains : Seq[String]

  /** Описание текущей цели, если есть. */
  def target  : Option[MExtTarget]

  /** Статус исполнения результата. */
  def status  : Option[MAnswerStatus]

  /** Инфа по случившийся ошибке. */
  def error   : Option[MErrorInfoT]

  /** Произвольные данные, выставляемые адаптером в рамках текущего запроса. */
  def custom  : Option[js.Any]

  override def toJson: js.Dictionary[js.Any] = {
    val d = js.Dictionary[js.Any] (
      ACTION_FN   -> action.strId,
      SERVICE_FN  -> service.toJson
    )
    if (mads.nonEmpty) {
      val madsSer = mads.map(_.toJson)
      d.update(ADS_FN, madsSer)
    }
    if (domains.nonEmpty)
      d.update(DOMAIN_FN, domains)
    if (target.nonEmpty)
      d.update(TARGET_FN, target.get.toJson)
    if (status.nonEmpty)
      d.update(STATUS_FN, status.get.jsStr)
    if (error.nonEmpty)
      d.update(ERROR_FN, error.get.toJson)
    if (custom.nonEmpty)
      d.update(CUSTOM_FN, custom.get)
    d
  }
}

/** Дефолтовая реализация контекста. */
case class MJsCtx(
  action  : MAskAction,
  mads    : Seq[MAdCtx],
  service : MServiceInfo,
  domains : Seq[String],
  target  : Option[MExtTarget],
  custom  : Option[js.Any],
  // Необязательные параметры -- только для исходящего контекста, на входе не парсим.
  status  : Option[MAnswerStatus] = None,
  error   : Option[MErrorInfoT] = None
) extends MJsCtxT


import io.suggest.adv.ext.model.ctx.MErrorInfo._


trait MErrorInfoT extends IToJsonDict {
  def msg: String
  def args: Seq[String]

  def toJson = js.Dictionary[js.Any](
    MSG_FN  -> msg,
    ARGS_FN -> args
  )
}

case class MErrorInfo(
  msg   : String,
  args  : Seq[String] = Seq.empty
) extends MErrorInfoT

