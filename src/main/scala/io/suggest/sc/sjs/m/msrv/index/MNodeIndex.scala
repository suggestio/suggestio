package io.suggest.sc.sjs.m.msrv.index

import io.suggest.sc.sjs.util.router.srv.routes
import io.suggest.sjs.common.xhr.Xhr

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.{JSON, UndefOr}
import scala.scalajs.js.annotation.JSName
import io.suggest.sc.ScConstants.Resp._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 12:22
 * Description: Модель доступа к backend-серверу suggest.io за данными.
 * Модель скрывает низкоуровневые тонкости обращения за конкретными вещами:
 * - Использование http или ws канала для запроса.
 */
object MNodeIndex {

  /**
   * Запустить получение index-страницы.
   * @param adnIdOpt id узла.
   * @return Фьючерс с результатами исполнения запроса.
   */
  def getIndex(adnIdOpt: Option[String])(implicit ec: ExecutionContext): Future[MNodeIndex] = {
    val reqArgs = MScReqArgsJson()
    // Собрать и отправить запрос за данными index.
    val router = routes.controllers.MarketShowcase
    val route = adnIdOpt match {
      case Some(adnId) =>
        router.nodeIndex(adnId, reqArgs)
      case None =>
        router.geoIndex(reqArgs)
    }
    Xhr.successWithStatus(200) {
      Xhr.send(
        method  = route.method,
        url     = route.url
      )

    } map { xhr =>
      // Как бы десериализация ответа.
      val json = JSON.parse( xhr.responseText )
        .asInstanceOf[MNodeIndexJson]
      new MNodeIndex(json)
    }
  }

}


/** API доступа к JSON-ответу сервера. */
sealed trait MNodeIndexJson extends js.Object {

  /** id экшена идентифицирует и формат ответа. Тут он по идее всегда одинаковый. */
  @JSName(ACTION_FN)
  val action: String = js.native

  /** Поле html содержит верстку для отображения. */
  @JSName(HTML_FN)
  val html: String = js.native

  /** Использовал ли сервер геолокацию для формирования ответа? */
  @JSName(IS_GEO_FN)
  val is_geo: Boolean = js.native

  /** id узла, к которому относится ответ сервера. */
  @JSName(ADN_ID_FN)
  val curr_adn_id: UndefOr[String] = js.native

}


/** Враппер над [[MNodeIndexJson]] для повышения удобства доступа к сырому JSON-ответу сервера. */
sealed class MNodeIndex(json: MNodeIndexJson) {
  def html  = json.html
  def isGeo = json.is_geo
  lazy val adnIdOpt = json.curr_adn_id.toOption

  override def toString: String = {
    json.action + "(isGeo=" + isGeo + ",nodeId=" + adnIdOpt + ",html=" + html.length + "b)"
  }
}
