package io.suggest.adv

import io.suggest.sc.ScConstants

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.01.16 14:34
  * Description: Констатны, общие для разных ADV-форм.
  */
object AdvConstants {

  /** Константы adv-форм для суперюзеров s.io. */
  object Su {

    def PREFIX            = "sua"

    /** id чекбокса, содержащего привелегированную галочку бесплатного размещения карточки. */
    def ADV_FOR_FREE_ID   = PREFIX + "fcb"

    /** Имя поля галочки бесплатного размещения, сабмиттящееся на сервер. */
    def ADV_FOR_FREE_NAME = "freeAdv"

  }


  /** Константы JSON-ответов сервера на запросы рассчета стоимости размещения. */
  object PriceJson {

    /** id инпута, который содержит URL для сабмита формы для рассчета цены и прочего. */
    def GET_PRICE_URL_INPUT_ID = "gpuiadv"

    /** Имя аттрибута, который содержит HTTP-метод для обращения к URL. */
    def ATTR_METHOD = ScConstants.CUSTOM_ATTR_PREFIX + "method"

    /** Имя поля в JSON-ответе цены, содержащее отрендеренную цену. */
    def PRICE_HTML_FN          = "a"

    /** Имя поля в JSON-ответе цены, содержащее отрендеренную инфу по периоду размещения. */
    def PERIOD_REPORT_HTML_FN  = "b"

  }


  /** Константы компонента периода размещения. */
  object Period {

    /** Имя поля верхнего уровня с периодом размещения. */
    def PERIOD_FN = "period"

  }


  /** Константы компонента карты размещения. */
  object RadMap {

    /** Имя поля верхнего уровня с данными карты Rad-map. */
    def RADMAP_FN = "map"

  }

}
