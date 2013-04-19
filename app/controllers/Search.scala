package controllers

import play.api.mvc._
import io.suggest.util.UrlUtil
import util.{SioSearchResult, SioSearchOptions, SiowebEsUtil, SiobixFs}
import io.suggest.model.SioSearchContext
import play.api.libs.json._
import play.api.libs.Jsonp

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.13 16:13
 * Description: Контроллер выполнения поисковых запросов.
 */

object Search extends Controller {

  /**
   * json-рендерер списка результатов.
   */
  implicit val maplistWrites = new Writes[List[SioSearchResult]] {
    /**
     * Сериализация списка результатов поиска.
     * @param l список
     * @return JsValue
     */
    def writes(l: List[SioSearchResult]): JsValue = {
      val jsonList = l.map { ssr =>
        val m1 = ssr.data.mapValues(JsString(_))
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
  def liveSearch(domainRaw:String, queryStr:String, debug:Boolean, langs:String) = Action {
    val dkey = UrlUtil.normalizeHostname(domainRaw)
    SiobixFs.getSettingsForDkeyCache(dkey) match {
      // Домен есть в системе.
      case Some(domainSettings) =>
        // TODO нужно восстанавливать SearchContext из кукисов реквеста или генерить новый
        val searchContext = new SioSearchContext()
        // TODO Настройки поиска, заданные юзером, надо извлекать из модели DomainData
        val searchOptions = new SioSearchOptions(
          dkey = dkey,
          withExplain = debug
        )
        val searchResults = SiowebEsUtil.searchDomain(
          domainSettings = domainSettings,
          queryStr = queryStr,
          options  = searchOptions,
          searchContext = searchContext
        )
        // Отрендерить результаты в json-е
        val jsonResp : Map[String, JsValue] = Map(
          "status"        -> JsString("ok"),
          "timestamp"     -> JsNumber(System.currentTimeMillis()),
          "search_result" -> Json.toJson(searchResults)
        )
        val jsonp = Jsonp("sio._s_add_result", Json.toJson(jsonResp))
        // TODO Сохранить обновлённый searchContext и серилизовать в кукис ответа
        Ok(jsonp)

      // Нет такого домена в базе. TODO Нужно замутить какую-нибудь проверочку.
      case None =>
        NotFound
    }
  }

}