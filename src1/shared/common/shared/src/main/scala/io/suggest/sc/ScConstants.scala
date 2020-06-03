package io.suggest.sc

import io.suggest.common.html.HtmlConstants
import io.suggest.routes.JsRoutesConst

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.05.15 16:08
 * Description: Константы для выдачи.
 */
object ScConstants {

  def CUSTOM_ATTR_PREFIX = HtmlConstants.ATTR_PREFIX

  /** Название аттрибута с длиной. */
  def WIDTH_ATTR    = CUSTOM_ATTR_PREFIX + "width"
  /** Название аттрибута с высотой. */
  def HEIGHT_ATTR   = CUSTOM_ATTR_PREFIX + "height"


  /** Константы геолокации выдачи. */
  object ScGeo {

    /** Сколько миллисекунд ждать геолокации при запуске выдачи? */
    def INIT_GEO_LOC_TIMEOUT_MS = 5000

  }


  /** Какие-то дефолтовые настройки. */
  object Defaults {

    /** Дефолтовый цвет выдачи, если нет ничего. */
    def BG_COLOR = "333333"

    /** Дефолтовый цвет элементов переднего плана. */
    def FG_COLOR = "FFFFFF"


  }


  /** Client/server констатны выдачи для моделей ScReqArgs. */
  object ReqArgs {

    /** Имя поля с данными экрана клиентского устройства. */
    final def SCREEN_FN         = "s"

    /** Имя поля с флагом о том, требуется ли рендерить welome-screen для index-выдачи? */
    final def WITH_WELCOME_FN   = "wc"

    /** Имя поля с версией API выдачи. */
    final def VSN_FN            = "v"

    /** Имя поля с id текущего узла выдачи. Названа adn вместо node для однозначности, что это наверное ресивер. */
    final def NODE_ID_FN        = "n"

    /** Имя поля с данными физического окружения устройства клиента.
      * Пришло на смену старому кривому полю с geo mode. */
    final def LOC_ENV_FN        = "e"

    /** Имя поля для разрешения или запрета погружения в узле-ресивер. */
    final def GEO_INTO_RCVR_FN  = "r"

    /** Имя поля с флагом возврата данных геолокации клиента назад клиенту. */
    final def RET_GEO_LOC_FN    = "g"

  }


  /** Константы логотипа. */
  object Logo {

    /** Высота логотипа узла в css-пикселях. Изначально было 30, но для унификации с картой стало 40px. */
    def HEIGHT_CSSPX = 40

  }


  /** Константы layout. */
  object Layout {

    /** id корневого div выдачи. */
    def ROOT_ID = "sioMartRoot"

  }


  private def _ATTR_PRODUCER_ID = CUSTOM_ATTR_PREFIX + "producer-id"

  /** Константы для отрендеренных карточек. */
  object Block {

    /** Имя аттрибута с порядковым номером в плитке от нуля. */
    def BLK_INDEX_ATTR    = CUSTOM_ATTR_PREFIX + "index"
    /** Имя аттрибута с id текущей рекламной карточки. */
    def MAD_ID_ATTR       = CUSTOM_ATTR_PREFIX + "mad-id"

    def ID_DELIM          = ":"
    /** id grid-блока формируется как-то так: "...madId..." + ID_SUFFIX. */
    def ID_SUFFIX         = "blk"

    /** Имя аттрибута с id продьюсера указанной карточки. */
    def PRODUCER_ID_ATTR  = _ATTR_PRODUCER_ID


    object Text {

      /** Название может не совсем точно суть отражать, но это слово использовалось для сборки fieldId (fid). */
      def FIELD_TEXT_NAME = "title"

      def fieldTextId( entityId: Int ) = FIELD_TEXT_NAME + "-" + entityId
    }

  }


  /** Константы для рендера приветствия узла. */
  object Welcome {

    /** Начинать скрывать карточку приветствия через указанное время. */
    def HIDE_TIMEOUT_MS = 2500

    /** Оценочное максимальное реальное время анимации сокрытия приветствия.
      * Через это время элемент будет считаться скрытым. */
    def FADEOUT_TRANSITION_MS = 700


    /** CSS-классы для анимации через css-трансформации. */
    object Anim {
      /** css transition на запуск плавной анимации сокрытия приветствия. */
      def TRANS_02_CSS_CLASS = "__animated"

      /** Название класса конкретной анимации плавного сокрытия приветствия. */
      def FADEOUT_CSS_CLASS  = "__fade-out"

      /** Название css-класса для приветствия, добавляющего will-change, для подготовки к fadeout-анимации. */
      def WILL_FADEOUT_CSS_CLASS = "__will-fade-out"
    }

  }


  /** Константы для подвыдачи focused ads. */
  object Focused {

    object FAd {

      /** Суффикс id sm-block div для focused-карточек. */
      final def BLOCK_ID_SUFFIX = "fblk"

    }

    /** Надо ли автоматом фокусировать карточку, которая была кликнута, но привела к перескоку в другую выдачу? */
    final def AUTO_FOCUS_AFTER_FOC_INDEX = false

  }


  /** Константы для jsRouter'а выдачи. */
  object JsRouter {

    /** window.NAME - название функции function(), которая будет вызвана  */
    final val ASYNC_INIT_FNAME = JsRoutesConst.GLOBAL_NAME + "AsyncInit"

    /** id script-тега js-роутера. HTML5 разрешает script.id. */
    final def DOM_ID = "scJsRouterCont"

    /** relative-ссылка на скрипт js-роутера. */
    final def URI = "/sc/router.js"

  }

  /** ServiceWorker выдачи. */
  object Sw {

    /** id инпута в siteTpl, который содержит в себе URL для scsw.js.  */
    def URL_INPUT_ID = "scsw"

  }

  /**
    * Константы имён полей, связанных с состоянием client-side выдачи.
    * Они нужны для формирования и парсинга URL Hash'ей в выдаче,
    * и для восприятия состояния выдачи на стороне сервера.
    */
  object ScJsState {

    // Название qs-параметров, отражающих состояние выдачи. Не удалось их нормально с максом согласовать из-за
    // какой-то неизвестной науке паталогии копипастинга идентификаторов из окошка джаббер-клиента в файл showcase2.coffee.

    // Обозначены через val, потому что эти константы используют очень активно или не используют вообще.
    val NODE_ID_FN               = "m.id"
    val GENERATION_FN           = "a.gen"

    val CAT_SCR_OPENED_FN       = "s.open"

    val FOCUSED_AD_ID_FN        = "f.cur.id"
    val FADS_OFFSET_FN          = "f.off"
    val PRODUCER_ADN_ID_FN      = "f.pr.id"

    val GEO_SCR_OPENED_FN       = "n.open"

    /** Название флага для диалога первого запуска. */
    val FIRST_RUN_OPEN_FN       = "r1"

    // Панель поиска: гео-позишен, [гео]теги.
    final def LOC_ENV_FN              = ReqArgs.LOC_ENV_FN

    def TAG_INFO_FN             = "t"
    val TAG_NODE_ID_FN          = TAG_INFO_FN + ".i"
    val TAG_FACE_FN             = TAG_INFO_FN + ".f"

    /** Историческая проблема: названия qs-параметров оторваны от структуры qs-модели состояния.
      * js-роутер o2qs() неправильно сериализует названия qs-полей.
      * Тут фунция ремонта ссылки с состоянием, которая сгенерена через js-роутер и его функцию o2qs(). */
    def fixJsRouterUrl(url: String): String = {
      url.replaceAll("([?&])a\\.", "$1")
    }

  }


  object Mad404 {

    /** Префикс id узла, содержащего 404-карточки. */
    def NO_ADS_FOUND_404_RCVR_ID_PREFIX = ".___404___."

    /** Проверить, является ли id данного узла служебным, относящимся к 404-узлу.
      *
      * @param nodeId id узла.
      * @return true, если данный id узла относится к 404-узлу.
      */
    def is404Node(nodeId: String): Boolean =
      nodeId startsWith NO_ADS_FOUND_404_RCVR_ID_PREFIX

  }

}
