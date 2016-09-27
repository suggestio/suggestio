package io.suggest.sc.sjs.m.msrv.index

import scala.scalajs.js
import io.suggest.sc.ScConstants.Resp._
import io.suggest.sc.sjs.m.mgeo.MGeoPoint
import io.suggest.sc.sjs.m.msrv.{IFocResp, MScResp}
import io.suggest.sc.sjs.util.router.srv.routes
import io.suggest.sjs.common.xhr.Xhr
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.sjs.common.util.SjsLogger

import scala.concurrent.Future
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.09.16 22:24
  * Description: Модель ответа данных по индексу.
  */
object MScRespIndex extends SjsLogger {

  /**
   * Запустить index-запрос согласно переданным аргументам.
   * @param args Аргументы поиска index.
   * @return Фьючерс с MNodeIndex внутри.
   */
  def getIndex(args: IScIndexArgs): Future[MScRespIndex] = {
    val argsJson = args.toJson
    // Собрать и отправить запрос за данными index.
    val route = routes.controllers.Sc.index(argsJson)
    // Запустить асинхронный запрос и распарсить результат.
    val fut = for {
      raw <- Xhr.getJson(route)
    } yield {
      // Тут очень кривой код, потому что пока некогда пилить нечто более продвинутое...
      MScResp(raw)
        .actions
        .head
        .index.get
    }

    fut.onFailure { case ex: Throwable =>
      error( ErrorMsgs.GET_NODE_INDEX_FAILED, ex )
    }

    fut
  }


}

case class MScRespIndex(json: MScRespIndexJson) extends IFocResp {

  def html      = json.html

  def adnIdOpt  = json.adnId.toOption

  def titleOpt  = json.title.toOption

  lazy val geoPoint: Option[MGeoPoint] = {
    json.geoPoint
      .toOption
      .map(MGeoPoint.apply)
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
