package io.suggest.adv.direct

import io.suggest.common.html.HtmlConstants
import io.suggest.sc.ScConstants

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.12.15 18:58
 * Description: Константы формы прямого размещения.
 */
object AdvDirectFormConstants {

  // TODO Какая-то свалка получилась. Сгруппировать бы это по inner-объектам...

  /** Разделитель кусков составных id. */
  def ID_DELIM                = "."

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
  def CITY_TAB_BODY_PREFIX    = CITIES_HEADS_CONT_ID + "b" + ID_DELIM

  /** Класс-пометка о том, что данный элемент является "кнопкой" города. */
  def CITY_TAB_HEAD_CLASS     = "adv-management_city-i"

  /** id элемента, содержащего название текущего (активного) города. */
  def CITY_CURR_TITLE_ID      = CITIES_HEADS_CONT_ID + "T"

  /** id таба одного города. */
  def CITY_TAB_HEAD_ID(cityId: String) = CITIES_HEADS_CONT_ID + ID_DELIM + cityId

  /** сборка id тела данных по одному городу. */
  def CITY_TAB_BODY_ID(cityId: String) = CITY_TAB_BODY_PREFIX + cityId

  def ATTR_CITY_ID            = HtmlConstants.ATTR_NODE_ID
  def ATTR_CAT_ID             = HtmlConstants.ATTR_PREFIX + "ci"

  /** Класс заголовка вкладки группы (категории) узлов. */
  def NGRP_TAB_HEAD_CLASS     = "select-tab_i"

  /** Контейнер всех групп нод из всех городов. */
  def NGRPS_CONT_ID           = NODES_BAR_ID + "Ngc"


  /** Префикс id'шников заголовков табов групп узлов. */
  def CITY_NODES_TAB_HEAD_ID_PREFIX = CITIES_HEADS_CONT_ID + "Ns" + ID_DELIM

  /** Сборка id контейнеров заголовков табов нод в рамках города. */
  def CITY_NODES_TAB_HEAD_ID(cityId: String, catId: Option[String] = None): String = {
    val id0 = CITY_NODES_TAB_HEAD_ID_PREFIX + cityId
    _maySuffixed(id0, catId)
  }


  /** Префикс id'шников чекбоксов на уровне табов. */
  def CITY_NODES_TAB_HEAD_CHECKBOX_ID_PREFIX = "Cb" + CITY_NODES_TAB_HEAD_ID_PREFIX
  /** Сборка id'шников чекбоксов на уровне табов. */
  def CITY_NODES_TAB_HEAD_CHECKBOX_ID(cityId: String, catId: Option[String] = None): String = {
    val id0 = CITY_NODES_TAB_HEAD_CHECKBOX_ID_PREFIX + cityId
    _maySuffixed(id0, catId)
  }


  /** Константы для табов, т.е. заголовков групп узлов. */
  object Tabs {

    /** Префикс id'шников контейнеров counter'ов, т.е. счетчиков выбранных. */
    def COUNTER_ID_PREFIX = "uctr" + CITY_NODES_TAB_HEAD_ID_PREFIX

    /** Сборка id'шников контейнеров counter'ов на вкладках. */
    def COUNTER_ID(cityId: String, catId: Option[String] = None): String = {
      val id0 = COUNTER_ID_PREFIX + cityId
      _maySuffixed(id0, catId)
    }

    /** В аттрибут тега сохраняется кол-во доступных для подсчета элементов. */
    def TOTAL_AVAIL_ATTR = ScConstants.CUSTOM_ATTR_PREFIX + "ta"

  }


  private def _maySuffixed(prefix: String, suffixOpt: Option[String]): String = {
    suffixOpt.fold(prefix)(prefix + ID_DELIM + _)
  }

  def CITY_CAT_NODES_ID_PREFIX = "Ccn" + CITY_TAB_BODY_PREFIX + ID_DELIM

  /** Сборка id контейнера одного тела нод в рамках группы узлов города. */
  def CITY_NODES_TAB_BODY_ID(cityId: String, catId: String): String = {
    CITY_CAT_NODES_ID_PREFIX + cityId + ID_DELIM + catId
  }


  def NGRPS_CITY_CONT_ID_PREFIX = NGRPS_CONT_ID + ID_DELIM
  /** id контейнера всех групп узлов в рамках одного города. */
  def NGRPS_CITY_CONT_ID(cityId: String): String = {
    NGRPS_CITY_CONT_ID_PREFIX + cityId
  }

  def NODE_ROW_ID_PREFIX = PREFIX + "O" + ID_DELIM

  /** id контейнера одного узла в списке узлов. */
  def NODE_ROW_ID(nodeId: String): String = {
    NODE_ROW_ID_PREFIX + nodeId
  }

  def NODE_CHECK_BOX_ID_PREFIX = NODE_ROW_ID_PREFIX + ID_DELIM + "cb" + ID_DELIM

  def NODE_CHECK_BOX_ID(nodeId: String): String = {
    NODE_CHECK_BOX_ID_PREFIX + nodeId
  }


  /** Контейнер для констант узлов. */
  object Nodes {

    def ATTR_NODE_ID            = HtmlConstants.ATTR_NODE_ID

  }

}
