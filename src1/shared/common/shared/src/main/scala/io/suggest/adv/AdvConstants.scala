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
    def ADV_FOR_FREE_FN   = "freeAdv"

  }


  /** Константы заливки шейпов на карте текущих (уже существующих) гео-размещений. */
  object CurrShapes {

    /** Цвет заливки запрошенных размещений. */
    def REQ_COLOR = "#4C88A9"

    /** Цвет заливки принятых размещений. */
    def OK_COLOR  = "#1ABC8A"

    /** Прозрачность заливки info-шейпов. */
    def OPACITY   = 0.5

  }


  /** Константы JSON-ответов сервера на запросы рассчета стоимости размещения. */
  object Price {

    /** id контейнера для price-виджета. Используется в react. */
    def OUTER_CONT_ID = "prouc"

    object Json {

      /** id инпута, который содержит URL для сабмита формы для рассчета цены и прочего. */
      def GET_PRICE_URL_INPUT_ID = "gpuiadv"

      /** Имя аттрибута, который содержит HTTP-метод для обращения к URL. */
      def ATTR_METHOD = ScConstants.CUSTOM_ATTR_PREFIX + "method"

      /** Имя поля в JSON-ответе цены, содержащее отрендеренную цену. */
      def PRICE_HTML_FN = "a"

      /** Имя поля в JSON-ответе цены, содержащее отрендеренную инфу по периоду размещения. */
      def PERIOD_REPORT_HTML_FN = "b"

    }

  }


  /** Имя поля верхнего уровня с периодом размещения. */
  def PERIOD_FN = "period"


  object DtPeriod {

    def QUICK_PERIOD_FN     = PERIOD_FN

    def DATES_INTERVAL_FN   = "date"

    def START_FN            = "start"

    def END_FN              = "end"

  }

}
