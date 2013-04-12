package controllers

import play.api.mvc._
import io.suggest.util.UrlUtil
import util.{SioSearchOptions, SiowebEsUtil, SiobixFs}
import io.suggest.model.SioSearchContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.13 16:13
 * Description: Контроллер выполнения поисковых запросов.
 */

object Search extends Controller {

  // TODO Настройки поиска, заданные юзером, надо извлекать из модели
  val searchOptions = new SioSearchOptions()

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
        // TODO нужно восстанавливать SearchContext из реквеста или генерить новый
        val searchContext = new SioSearchContext()
        val hits = SiowebEsUtil.searchDomain(
          domainSettings = domainSettings,
          queryStr = queryStr,
          options  = searchOptions,
          searchContext = searchContext
        )

        // Отрендерить результаты юзеру
        ???

      case None => NotFound
    }
  }

}
