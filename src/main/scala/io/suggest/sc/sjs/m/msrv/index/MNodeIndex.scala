package io.suggest.sc.sjs.m.msrv.index

import io.suggest.sc.sjs.util.router.srv.routes
import io.suggest.sjs.common.model.{TimestampedCompanion, Timestamped}
import io.suggest.sjs.common.xhr.Xhr

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js.{Dictionary, WrappedDictionary, Any}
import io.suggest.sc.ScConstants.Resp._

import scala.util.Try

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
      raw <- Xhr.getJson(route)
    } yield {
      val d = raw.asInstanceOf[Dictionary[Any]]
      MNodeIndex(d)
    }
  }

}


/** Враппер над сырым JSON для повышения удобства доступа к сырому JSON-ответу сервера. */
case class MNodeIndex(json: WrappedDictionary[Any]) {

  /** index-верстка выдачи. */
  def html  = json(HTML_FN).asInstanceOf[String]

  /** Прочитать значение флага геовыдачи. */
  def isGeo = json(IS_GEO_FN).asInstanceOf[Boolean]

  /** id узла, если известен. */
  def adnIdOpt: Option[String] = {
    json.get(ADN_ID_FN)
      .asInstanceOf[Option[String]]
  }

  /** Достаточна ли геолокация по мнению сервера? Мнение возвращает geoShowcase(). */
  def geoAccurEnought: Option[Boolean] = {
    json.get(GEO_ACCURACY_ENOUGHT_FN)
      .asInstanceOf[Option[Boolean]]
  }

  def title: Option[String] = {
    json.get(TITLE_FN)
      .asInstanceOf[Option[String]]
  }

  /** toString() генерит слишком много ненужных букв по дефолту. Скрываем html-поле при рендере словаря. */
  override def toString: String = {
    val jsonMin = json.iterator
      .map { case (k, v) =>
        k + " -> " + (if (k == HTML_FN) "..." else v)
      }
      .mkString(", ")
    getClass.getSimpleName + "(" + jsonMin + ")"
  }
}



/** Контейнер для [[MNodeIndex]] для возврата в комплекте с timestamp начала запроса. */
case class MNodeIndexTimestamped(
  override val result: Try[MNodeIndex],
  override val timestamp: Long
)
  extends Timestamped[MNodeIndex]
object MNodeIndexTimestamped extends TimestampedCompanion[MNodeIndex]

