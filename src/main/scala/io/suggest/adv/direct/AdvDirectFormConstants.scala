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

  /** id всей формы прямого размещения. */
  def FORM_ID                 = "advsForm"

  /** id контейнера динамического списка выборки узлов. */
  def NODES_BAR_ID            = PREFIX + "N"

  /** id контейнера заголовков табов городов. */
  def CITIES_HEADS_CONT_ID    = NODES_BAR_ID + "CH"

  /** Контейнер тел содержимого городов. */
  def CITIES_BODIES_CONT_ID   = NODES_BAR_ID + "CB"

  /** Префикс id таба города. */
  def CITY_TAB_BODY_PREFIX    = CITIES_HEADS_CONT_ID + "b."

  /** Класс-пометка о том, что данный элемент является "кнопкой" города. */
  def CITY_TAB_HEAD_CLASS     = "adv-management_city-i"

  /** id элемента, содержащего название текущего (активного) города. */
  def CITY_CURR_TITLE_ID      = CITIES_HEADS_CONT_ID + "T"

  /** id таба одного города. */
  def CITY_TAB_HEAD_ID(cityId: String) = CITIES_HEADS_CONT_ID + "." + cityId

  /** сборка id тела данных по одному городу. */
  def CITY_TAB_BODY_ID(cityId: String) = CITY_TAB_BODY_PREFIX + cityId

  def ATTR_CITY_ID            = ATTR_NODE_ID
  def ATTR_CAT_ID             = ATTR_PREFIX + "ci"

  /** Класс заголовка вкладки группы (категории) узлов. */
  def NGRP_TAB_HEAD_CLASS     = "select-tab_i"

  /** Контейнер всех групп нод из всех городов. */
  def NGRPS_CONT_ID           = NODES_BAR_ID + "Ngc"


  /** Префикс id'шников заголовков табов групп узлов. */
  def CITY_NODES_TAB_HEAD_ID_PREFIX = CITIES_HEADS_CONT_ID + "Ns."

  /** Сборка id контейнеров заголовков табов нод в рамках города. */
  def CITY_NODES_TAB_HEAD_ID(cityId: String, catId: Option[String] = None): String = {
    val id0 = CITY_NODES_TAB_HEAD_ID_PREFIX + cityId
    catId.fold(id0)(id0 + "." + _)
  }

  /** Сборка id контейнера одного тела нод в рамках группы узлов города. */
  def CITY_NODES_TAB_BODY_ID(cityId: String, catId: String): String = {
    CITY_TAB_BODY_ID(cityId) + ".Ns." + catId
  }

  /** id контейнера всех групп узлов в рамках одного города. */
  def NGRPS_CITY_CONT_ID(cityId: String): String = {
    NGRPS_CONT_ID + "." + cityId
  }

  def NODE_ROW_ID_PREFIX = PREFIX + "O."

  /** id контейнера одного узла в списке узлов. */
  def NODE_ROW_ID(nodeId: String): String = {
    NODE_ROW_ID_PREFIX + nodeId
  }

  def NODE_CHECK_BOX_ID_PREFIX = NODE_ROW_ID_PREFIX + ".cb."

  def NODE_CHECK_BOX_ID(nodeId: String): String = {
    NODE_CHECK_BOX_ID_PREFIX + nodeId
  }

}
