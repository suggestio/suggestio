package io.suggest.sc.sjs.m.msrv.foc

import io.suggest.primo.IApplyUndef1
import io.suggest.sc.ScConstants.Resp._
import io.suggest.sc.sjs.m.mfoc.MFocAd
import io.suggest.sc.sjs.m.msrv.IFocResp

import scala.scalajs.js
import scala.scalajs.js._
import scala.scalajs.js.annotation.JSName



object MScRespAdsFoc extends IApplyUndef1 {
  override type ApplyArg_t = MScRespAdsFocJson
  override type T = MScRespAdsFoc
}

/** Реализация модели ответов на запросы к focused-api. */
case class MScRespAdsFoc(json: MScRespAdsFocJson) extends IFocResp {

  /** Массив карточек. */
  // TODO А почему тут List, а не Seq?
  lazy val fads: List[MFocAd] = {
    json.fads
      .toOption
      .iterator
      .flatten
      .map( MFocAd.apply )
      .toList
  }

  /** inline-стили для focused-карточек. */
  def styles: Option[String] = {
    json.styles
      .toOption
  }

  /** Общее кол-во карточек во всей запрошенной выборке. */
  def totalCount: Int = {
    json.totalCount
  }

  override def toString: String = {
    val _fads = fads
    getClass.getSimpleName + "(" +
      _fads.iterator.map(_.toString).mkString(",") +
      ",s=" + styles.fold(0)(_.length) + "," +
      _fads.length + "/" + totalCount +
      ")"
  }

}


/** JSON-интерфейс к сырому ответу сервера с focused-карточками. */
@js.native
sealed trait MScRespAdsFocJson extends js.Object {

  @JSName( FOCUSED_ADS_FN )
  var fads: js.UndefOr[ js.Array[js.Dictionary[js.Any]] ] = js.native

  @JSName( TOTAL_COUNT_FN )
  var totalCount: Int = js.native

  @JSName( STYLES_FN )
  var styles: UndefOr[String] = js.native

}
