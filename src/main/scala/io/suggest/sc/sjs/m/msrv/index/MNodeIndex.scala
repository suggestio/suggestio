package io.suggest.sc.sjs.m.msrv.index

import io.suggest.sc.sjs.m.msrv.MSrvUtil
import io.suggest.sc.sjs.util.router.srv.routes

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js.{Dictionary, WrappedDictionary, Any}
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
   * Запустить index-запрос согласно переданным аргументам.
   * @param args Аргументы поиска index.
   * @return Фьючерс с MNodeIndex внутри.
   */
  def getIndex(args: IScIndexArgs)(implicit ec: ExecutionContext): Future[MNodeIndex] = {
    val argsJson = args.toJson
    // Собрать и отправить запрос за данными index.  TODO Унифицировать экшены запросов.
    val router = routes.controllers.MarketShowcase
    val route = args.adnIdOpt match {
      case Some(adnId) =>
        router.nodeIndex(adnId, argsJson)
      case None =>
        router.geoIndex(argsJson)
    }
    // Запустить асинхронный запрос и распарсить результат.
    for {
      raw <- MSrvUtil.reqJson(route)
    } yield {
      val d = raw.asInstanceOf[Dictionary[Any]]
      new MNodeIndex(d)
    }
  }

}


/** Враппер над сырым JSON для повышения удобства доступа к сырому JSON-ответу сервера. */
sealed class MNodeIndex(json: WrappedDictionary[Any]) {

  /** index-верстка выдачи. */
  def html  = json(HTML_FN).asInstanceOf[String]

  /** Прочитать значение флага геовыдачи. */
  def isGeo = json(IS_GEO_FN).asInstanceOf[Boolean]

  /** id узла, если известен. */
  lazy val adnIdOpt = json.get(ADN_ID_FN).map(_.toString)

  /** Нужен только для toString() */
  private def action = json(ACTION_FN).toString

  override def toString: String = {
    action + "(isGeo=" + isGeo + ",nodeId=" + adnIdOpt + ",html=" + html.length + "b)"
  }
}
