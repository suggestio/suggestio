package io.suggest.adv.direct

import io.suggest.common.html.HtmlConstants._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.12.15 18:58
 * Description: Константы формы прямого размещения.
 */
object AdvDirectFormConstants {

  /** Префикс id ряда элементов только данной формы. */
  def PREFIX                  = "adi"

  /** id контейнера динамического списка выборки узлов. */
  def NODES_BAR_ID            = PREFIX + "N"

  /** id контейнера заголовков табов городов. */
  def CITIES_HEADS_CONT_ID    = NODES_BAR_ID + "CH"

  /** Контейнер тел содержимого городов. */
  def CITIES_BODIES_CONT_ID   = NODES_BAR_ID + "CB"

  /** Префикс id таба города. */
  def CITY_TAB_BODY_PREFIX    = CITIES_HEADS_CONT_ID + "b"

  /** id таба одного города. */
  def CITY_TAB_HEAD_ID(cityId: String) = CITIES_HEADS_CONT_ID + "." + cityId

  /** сборка id тела данных по одному городу. */
  def CITY_TAB_BODY_ID(cityId: String) = CITY_TAB_BODY_PREFIX + "." + cityId

  def ATTR_CITY_ID            = ATTR_NODE_ID
  def ATTR_CAT_ID             = ATTR_PREFIX + "ci"

  /** Сборка id контейнеров заголовков табов нод в рамках города. */
  def CITY_NODES_TAB_HEAD_ID(cityId: String, catId: Option[String] = None): String = {
    val id0 = CITY_TAB_HEAD_ID(cityId) + ".Ns"
    catId.fold(id0)(id0 + "." + _)
  }

  /** Сборка id контейнера одного тела нод в рамках группы узлов города. */
  def CITY_NODES_TAB_BODY_ID(cityId: String, catId: String): String = {
    CITY_TAB_BODY_ID(cityId) + ".Ns." + catId
  }

}
