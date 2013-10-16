package controllers

import play.api.mvc._
import io.suggest.util.UrlUtil
import _root_.util._
import play.api.libs.json._
import play.api.libs.Jsonp
import models.MDomain
import scala.concurrent.Future
import io.suggest.model.SioSearchContext
import play.api.libs.concurrent.Execution.Implicits._
import scala.Some

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.13 16:13
 * Description: Контроллер выполнения поисковых запросов. Получает запрос и возвращает поисковую выдачу.
 */

object Search extends SioController {

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
  def liveSearch(domainRaw:String, queryStr:String, debug:Boolean, langs:String) = Action.async {
    val dkey = UrlUtil.normalizeHostname(domainRaw)
    MDomain.getForDkey(dkey) flatMap {
      case Some(mdomain) =>
        // Домен есть в системе.
        // TODO нужно восстанавливать SearchContext из кукисов реквеста или генерить новый
        val searchContext = new SioSearchContext()
        // TODO Настройки поиска, заданные юзером, надо извлекать из модели DomainData
        val searchOptions = new SioSearchOptions(
          domain = mdomain,
          withExplain = debug
        )
        SiowebEsUtil.searchDomain(
          queryStr = queryStr,
          options  = searchOptions,
          searchContext = searchContext
        ) map { searchResults =>
        // Отрендерить результаты в json-е
          val jsonResp : Map[String, JsValue] = Map(
            "status"        -> JsString("ok"),
            "timestamp"     -> JsNumber(System.currentTimeMillis()),
            "search_result" -> Json.toJson(searchResults)
          )
          val jsonp = Jsonp("sio._s_add_result", Json.toJson(jsonResp))
          // TODO Сохранить обновлённый searchContext и серилизовать в кукис ответа
          Ok(jsonp)
        } recover {
          case ex:NoSuchDomainException      => NotFound(ex.getMessage)
          case ex:EmptySearchQueryException  => ExpectationFailed(ex.getMessage)
          case ex                            => InternalServerError(ex.getMessage)
        }

      case None => NotFound
    }
  }

}
