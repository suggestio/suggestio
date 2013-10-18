package controllers

import play.api.mvc._
import io.suggest.util.UrlUtil
import util._
import play.api.libs.json._
import play.api.libs.Jsonp
import io.suggest.model.SioSearchContext
import play.api.libs.concurrent.Execution.Implicits._
import scala.Some
import models.MDomain

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.13 16:13
 * Description: Контроллер выполнения поисковых запросов. Получает запрос и возвращает поисковую выдачу.
 */

object Search extends SioController with Logs {

  import LOGGER._

  /**
   * json-рендерер списка результатов.
   * Нужен, т.к. стандартный json-генератор понятия не имеет как работать с типом List[SioSearchResult].
   * Передается автоматически в Json.toJson(searchResults).
   */
  implicit val maplistWrites = new Writes[List[SioSearchResult]] {
    /**
     * Сериализация списка результатов поиска.
     * @param l список
     * @return JsValue
     */
    def writes(l: List[SioSearchResult]): JsValue = {
      val jsonList = l.map { ssr =>
        val m1 = ssr.data.mapValues(JsString)
        JsObject(m1.toList)
      }
      JsArray(jsonList)
    }
  }


  /**
   * Запрос на поиск по сайту приходит сюда.
   * @param domainRaw домен поиска.
   * @param queryStr Строка запроса
   * @return
   */
  def siteSearch(domainRaw:String, queryStr:String, debug:Boolean, langs:String) = Action.async {
    val dkey = UrlUtil.normalizeHostname(domainRaw)
    lazy val logPrefix = s"siteSearch($dkey): "
    trace(logPrefix + s"q='$queryStr' debug=$debug langs=$langs")
    MDomain.getForDkey(dkey) flatMap {
      case Some(mdomain) =>
        trace(logPrefix + "domain found: " + mdomain)
        // Домен есть в системе.
        // TODO нужно восстанавливать SearchContext из кукисов реквеста или генерить новый
        val searchContext = new SioSearchContext()
        // TODO Настройки поиска, заданные юзером, надо извлекать из модели DomainData
        val searchOptions = new SioSearchOptions(
          domain = mdomain,
          withExplain = debug
        )
        // Отправляем время выполнения поиска в логи, если включен trace'инг.
        val searchStartedAt: Long = if (isTraceEnabled) System.currentTimeMillis() else -1L
        SiowebEsUtil.searchDomain(
          queryStr = queryStr,
          options  = searchOptions,
          searchContext = searchContext
        ) map { searchResults =>
          trace {
            val tookMs = System.currentTimeMillis() - searchStartedAt
            logPrefix + s"search:'$queryStr' with ${searchResults.size} results. [$tookMs ms]"
          }
          // Отрендерить результаты в json-е
          val jsonResp : Map[String, JsValue] = Map(
            "status"        -> JsString("ok"),
            "timestamp"     -> JsNumber(System.currentTimeMillis()),
            "search_result" -> Json.toJson(searchResults)
          )
          val jsonp = Jsonp("sio._s_add_result", Json.toJson(jsonResp))
          // TODO Сохранить обновлённый searchContext и серилизовать в кукис ответа
          Ok(jsonp)

        } recover { case ex =>
          error(logPrefix + "Search failed", ex)
          ex match {
            case _:NoSuchDomainException      => NotFound(ex.getMessage)
            case _:EmptySearchQueryException  => ExpectationFailed(ex.getMessage)
            case _                            => InternalServerError(ex.getMessage)
          }
        }

      case None =>
        warn(logPrefix + "dkey not found. 404")
        // TODO нужно наверное запускать проверку на предмет наличия на этом сайте скрипта?
        NotFound
    }
  }

}
