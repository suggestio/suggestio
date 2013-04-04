package controllers

import play.api.mvc._
import io.suggest.util.UrlUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.13 16:13
 * Description: Контроллер выполнения поисковых запросов.
 */

object Search extends Controller {

  /**
   * Запрос на поиск по сайту приходит сюда.
   * @param domainRaw домен поиска.
   * @param queryStr Строка запроса
   * @return
   */
  def liveSearch(domainRaw:String, queryStr:String) = Action {
    val domain = UrlUtil.normalizeHostname(domainRaw)
    // TODO определить индексы и типы, в которых мы будем искать.
    NotFound
  }

}
