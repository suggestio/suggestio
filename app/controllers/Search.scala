package controllers

import play.api.mvc._
import io.suggest.util.UrlUtil
import _root_.util.{SioSearchResult, SioSearchOptions, SiowebEsUtil, SiobixFs}
import io.suggest.model.SioSearchContext
import play.api.libs.json._

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
          withExplain = debug
        )
        val searchResults = SiowebEsUtil.searchDomain(
          domainSettings = domainSettings,
          queryStr = queryStr,
          options  = searchOptions,
          searchContext = searchContext
        )
        // Отрендерить результаты в json-е
        val jsonResp = Json.toJson(searchResults)
        // TODO Сохранить обновлённый searchContext в кукис ответа
        Ok(jsonResp)

      // Нет такого домена в базе. TODO Нужно замутить какую-нибудь проверочку.
      case None =>
        NotFound
    }
  }

}
