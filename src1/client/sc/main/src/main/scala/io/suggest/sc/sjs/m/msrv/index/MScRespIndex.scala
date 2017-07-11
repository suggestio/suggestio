package io.suggest.sc.sjs.m.msrv.index

import io.suggest.geo.MGeoPoint

import scala.scalajs.js
import io.suggest.sc.ScConstants.Resp._
import io.suggest.sc.router.routes
import io.suggest.sc.sjs.m.mgeo.MGeoPointExt
import io.suggest.sc.sjs.m.msrv.{IFocResp, MScResp, MSrv}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.sjs.common.primo.IApplyUndef1

import scala.concurrent.Future
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.09.16 22:24
  * Description: Модель ответа данных по индексу.
  */
object MScRespIndex extends Log with IApplyUndef1 {

  override type ApplyArg_t = MScRespIndexJson
  override type T = MScRespIndex

  /**
   * Запустить index-запрос согласно переданным аргументам.
   *
   * @param args Аргументы поиска index.
   * @return Фьючерс с MNodeIndex внутри.
   */
  def getIndex(args: IScIndexArgs): Future[MScRespIndex] = {
    // Собрать и отправить запрос за данными index.
    val route = routes.controllers.Sc.index( args.toJson )

    // Запустить асинхронный запрос и распарсить результат.
    val fut = MSrv.doRequest(route)
      .map(_scResp2index)

    fut.onFailure { case ex: Throwable =>
      LOG.error( ErrorMsgs.GET_NODE_INDEX_FAILED, ex )
    }

    fut
  }

  /** Кривое небезопасное приведение MScResp к [[MScRespIndex]]. */
  // Тут очень кривой код, потому что пока некогда пилить нечто более продвинутое...
  def _scResp2index(mResp: MScResp): MScRespIndex = {
    mResp.actions
      .head
      .index.get
  }

}


case class MScRespIndex(json: MScRespIndexJson) extends IFocResp {

  def html      = json.html

  def adnIdOpt  = json.adnId.toOption

  def titleOpt  = json.title.toOption

  lazy val geoPoint: Option[MGeoPoint] = {
    json.geoPoint
      .toOption
      .map(MGeoPointExt.apply)
  }

  /** toString() генерит слишком много ненужных букв по дефолту. Скрываем html-поле при рендере словаря. */
  override def toString: String = {
    getClass.getSimpleName + "(" + html.length + "c," + adnIdOpt + "," + titleOpt + "," + geoPoint + ")"
  }

}


/** JSON-интерфейс сырого инстанса. */
@js.native
sealed trait MScRespIndexJson extends js.Object {

  @JSName( HTML_FN )
  var html: String = js.native

  @JSName( ADN_ID_FN )
  var adnId: UndefOr[String] = js.native

  @JSName( TITLE_FN )
  var title: UndefOr[String] = js.native

  @JSName( GEO_POINT_FN )
  var geoPoint: UndefOr[js.Array[Double]] = js.native

}
