package io.suggest.adv.direct

import io.suggest.common.html.HtmlConstants
import io.suggest.common.qs.QsConstants
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
  private def ID_DELIM        = QsConstants.KEY_PARTS_DELIM_STR

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

  /** Префикс для DOM id табов. */
  def CITY_TAB_HEAD_PREFIX = CITIES_HEADS_CONT_ID + ID_DELIM

  def ATTR_CITY_ID            = HtmlConstants.ATTR_NODE_ID
  def ATTR_CAT_ID             = HtmlConstants.ATTR_PREFIX + "ci"

  /** Класс заголовка вкладки группы (категории) узлов. */
  def NGRP_TAB_HEAD_CLASS     = "select-tab_i"

  /** Контейнер всех групп нод из всех городов. */
  def NGRPS_CONT_ID           = NODES_BAR_ID + "Ngc"


  /** Префикс id'шников заголовков табов групп узлов. */
  def CITY_NODES_TAB_HEAD_ID_PREFIX = CITIES_HEADS_CONT_ID + "Ns" + ID_DELIM

  /** Контейнер переменных для id'шников, содержащих cityId и опциональный ngId.
    * Очень пригоден для рендера в суффикс DOM id. */
  case class CityNgIdOpt(cityId: String,  ngIdOpt: Option[String] = None) {
    override def toString: String = {
      _maySuffixed(cityId, ngIdOpt)
    }
  }


  /** Префикс id'шников чекбоксов на уровне табов. */
  def CITY_NODES_TAB_HEAD_CHECKBOX_ID_PREFIX = "Cb" + CITY_NODES_TAB_HEAD_ID_PREFIX


  /** Константы для табов, т.е. заголовков групп узлов. */
  object Tabs {

    /** Префикс id'шников контейнеров counter'ов, т.е. счетчиков выбранных. */
    def COUNTER_ID_PREFIX = "uctr" + CITY_NODES_TAB_HEAD_ID_PREFIX

    /** В аттрибут тега сохраняется кол-во доступных для подсчета элементов. */
    def TOTAL_AVAIL_ATTR = ScConstants.CUSTOM_ATTR_PREFIX + "ta"

  }


  private def _maySuffixed(prefix: String, suffixOpt: Option[String]): String = {
    suffixOpt.fold(prefix)(prefix + ID_DELIM + _)
  }

  /** Префикс DOM id для вкладок в рамках города: и заголовки, и их контент. */
  def CITY_CAT_NODES_ID_PREFIX = "Ccn" + CITY_TAB_BODY_PREFIX + ID_DELIM

  /** Контейнер идентификатора из двух частей: id города и id группы узлов.
    * Он сразу пригоден для рендера в строку-хвост id благодаря toString. */
  case class NgBodyId( cityId: String, ngId: String ) {
    override def toString = cityId + ID_DELIM + ngId
  }


  /** Префикс DOM id контейнера всех групп узлов в рамках одного города. */
  def NGRPS_CITY_CONT_ID_PREFIX = NGRPS_CONT_ID + ID_DELIM

  /** Префикс DOM id контейнера одного узла в списке узлов. */
  def NODE_ROW_ID_PREFIX = PREFIX + "O" + ID_DELIM

  def NODE_CHECK_BOX_ID_PREFIX = NODE_ROW_ID_PREFIX + ID_DELIM + "cb" + ID_DELIM


  /** Контейнер для констант узлов. */
  object Nodes {

    def ATTR_NODE_ID            = HtmlConstants.ATTR_NODE_ID

  }

}
