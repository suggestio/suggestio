package io.suggest.sc.sjs.m.msrv.tile

import io.suggest.primo.IApplyUndef1
import io.suggest.sc.sjs.m.mgrid.{MGridParamsJsonRaw, MGridParamsJsonWrapper}
import io.suggest.sc.ScConstants.Resp._
import io.suggest.sc.sjs.m.msrv.IResp

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSName


object MScRespAdsTile extends IApplyUndef1 {
  override type ApplyArg_t = MScRespAdsTileJson
  override type T = MScRespAdsTile
}

/** Инстанс ответа сервера по теме запроса плитки. */
case class MScRespAdsTile(json: MScRespAdsTileJson) extends IResp {

  def mads: Seq[MFoundAdJson] = {
    json.mads
      .iterator
      .flatten
      .toSeq
  }

  def css: Option[String] = {
    json.css.toOption
  }

  def params: Option[MGridParamsJsonWrapper] = {
    json.params
      .toOption
      .map(MGridParamsJsonWrapper.apply)
  }

  override def toString: String = {
    getClass.getSimpleName + "(" + mads.size + "ads,css=" +
      css.fold("0")(_.length + "b") +
      ",params=" + params +
      ")"
  }

}


/** json-интерфейс для сырого JSON-ответа сервера. */
@js.native
trait MScRespAdsTileJson extends js.Object {

  /** Отрендеренный карточки плитки (блоки) в необходимом порядке.
    * undefined возможно, и означает пустой массив. */
  @JSName( MADS_FN )
  var mads: UndefOr[js.Array[MFoundAdJson]] = js.native

  /** Отрендеренные стили карточек. */
  @JSName( CSS_FN )
  var css: UndefOr[String] = js.native

  /** Параметры отображения плитки. */
  @JSName( PARAMS_FN )
  var params: UndefOr[MGridParamsJsonRaw] = js.native

}
