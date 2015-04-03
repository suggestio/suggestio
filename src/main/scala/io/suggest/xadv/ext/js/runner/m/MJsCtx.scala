package io.suggest.xadv.ext.js.runner.m

import io.suggest.adv.ext.model.ctx.MJsCtxFieldsT

import scala.scalajs.js.{WrappedDictionary, Dictionary, Any, Array}
import scala.scalajs.js.JSConverters._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 13:48
 * Description: Состояние запроса. Через этого также происходит обмен данными.
 */

object MJsCtx extends MJsCtxFieldsT with FromStringT {

  override type T = MJsCtx

  override def fromJson(dyn: Any): MJsCtx = {
    val d = dyn.asInstanceOf[Dictionary[Any]] : WrappedDictionary[Any]
    MJsCtx(
      action = MAskActions.withName( d.get(ACTION_FN).get.toString ),
      mads = d.get(ADS_FN)
        .map {
          _.asInstanceOf[Array[Any]]
            .toSeq
            .map(MAdCtx.fromJson)
        }
        .getOrElse(Seq.empty),
      target = d.get(TARGET_FN)
        .map(MExtTarget.fromJson),
      service = d.get(SERVICE_FN)
        .map(MServiceInfo.fromJson),
      domains = d.get(DOMAIN_FN)
        .map { _.asInstanceOf[Array[String]].toSeq }
        .getOrElse(Nil),
      custom = d.get(CUSTOM_FN),
      svcTargets = d.get(SVC_TARGETS_FN)
        .iterator
        .flatMap { _.asInstanceOf[Array[Any]].iterator }
        .map { MExtTarget.fromJson }
        .toSeq
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
  def service : Option[MServiceInfo]

  /** Домены, которых касается запрос. */
  def domains : Seq[String]

  /** Описание текущей цели, если есть. */
  def target  : Option[IMExtTarget]

  /** Статус исполнения результата. */
  def status  : Option[MAnswerStatus]

  /** Инфа по случившийся ошибке. */
  def error   : Option[MErrorInfoT]

  /** Произвольные данные, выставляемые адаптером в рамках текущего запроса. */
  def custom  : Option[Any]

  def svcTargets: Seq[IMExtTarget]

  override final def toJson: Dictionary[Any] = {
    val d = Dictionary[Any] (
      ACTION_FN   -> action.strId
    )

    val _service = service
    if (_service.nonEmpty)
      d.update(SERVICE_FN, _service.get.toJson)

    val _mads = mads
    if (_mads.nonEmpty) {
      val madsSer = _mads.map(_.toJson)
      d.update(ADS_FN, madsSer.toJSArray)
    }

    val _domains = domains
    if (_domains.nonEmpty)
      d.update(DOMAIN_FN, _domains.toJSArray)

    val _target = target
    if (_target.nonEmpty)
      d.update(TARGET_FN, _target.get.toJson)

    val _status = status
    if (_status.nonEmpty)
      d.update(STATUS_FN, _status.get.jsStr)

    val _error = error
    if (_error.nonEmpty)
      d.update(ERROR_FN, _error.get.toJson)

    val _custom = custom
    if (_custom.nonEmpty)
      d.update(CUSTOM_FN, _custom.get)

    val _svcTargets = svcTargets
    if (_svcTargets.nonEmpty)
      d.update(SVC_TARGETS_FN, _svcTargets.iterator.map(_.toJson).toJSArray)

    d
  }

  override def toString: String = {
    var acc = "MJsCtxT(" + action
    if (mads.nonEmpty)
      acc = acc + ",mads=" + mads.toString()
    if (service.nonEmpty)
      acc = acc + ",svc=" + service.get.toString
    if (domains.nonEmpty)
      acc = acc + ",domains=[" + domains.mkString(",") + "]"
    if (status.nonEmpty)
      acc = acc + ",status=" + status.get
    if (error.nonEmpty)
      acc = acc + ",error=" + error.get
    if (custom.nonEmpty)
      acc = acc + ",custom=" + custom.get
    acc += ")"
    acc
  }
}


/** Враппер над контекстом. */
trait MJsCtxWrapperT extends MJsCtxT {
  def jsCtxUnderlying: MJsCtxT

  override def action   = jsCtxUnderlying.action
  override def custom   = jsCtxUnderlying.custom
  override def target   = jsCtxUnderlying.target
  override def error    = jsCtxUnderlying.error
  override def svcTargets = jsCtxUnderlying.svcTargets
  override def mads     = jsCtxUnderlying.mads
  override def domains  = jsCtxUnderlying.domains
  override def service  = jsCtxUnderlying.service
  override def status   = jsCtxUnderlying.status
}


/** Дефолтовая реализация контекста. */
case class MJsCtx(
  action      : MAskAction,
  mads        : Seq[MAdCtx],
  service     : Option[MServiceInfo],
  domains     : Seq[String],
  target      : Option[IMExtTarget],
  custom      : Option[Any],
  // Необязательные параметры -- только для исходящего контекста, на входе не парсим.
  status      : Option[MAnswerStatus] = None,
  error       : Option[MErrorInfoT] = None,
  svcTargets  : Seq[IMExtTarget] = Nil
) extends MJsCtxT


import io.suggest.adv.ext.model.ctx.MErrorInfo._


trait MErrorInfoT extends IToJsonDict {
  def msg: String
  def args: Seq[String]
  def info: Option[Any]

  def toJson = {
    val d = Dictionary[Any](
      MSG_FN  -> msg
    )
    // args
    val _args = args
    if (_args.nonEmpty)
      d.update(ARGS_FN, _args.toJSArray)
    // info
    val _info = info
    if (_info.nonEmpty)
      d.update(INFO_FN, _info.get)
    // return
    d
  }

  override def toString: String = {
    "MErrorInfo(" + msg + "," + args + "," + info + ")"
  }
}

case class MErrorInfo(
  msg   : String,
  args  : Seq[String] = Seq.empty,
  info  : Option[Any] = None
) extends MErrorInfoT

