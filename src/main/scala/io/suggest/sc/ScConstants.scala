package io.suggest.sc

import io.suggest.common.html.HtmlConstants

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.05.15 16:08
 * Description: Константы для выдачи.
 */
object ScConstants {

  def DIV_WRAPPER_SUFFIX   = "Wrapper"
  def DIV_CONTENT_SUFFIX   = "Content"
  def DIV_CONTAINER_SUFFIX = "Container"
  def DIV_LOADER_SUFFIX    = "Loader"

  /** Название класса активности.
    * Есть несколько css-классов с одинаковыми именами, но в разных scope'ах. */
  private def ACTIVE_CLASS = "__active"

  /** Имя css-класс с will-change для подготовки к translate3d. */
  def CLASS_WILL_TRANSLATE3D = "will-translate3d"

  def CUSTOM_ATTR_PREFIX = HtmlConstants.ATTR_PREFIX

  /** Название аттрибута с длиной. */
  def WIDTH_ATTR    = CUSTOM_ATTR_PREFIX + "width"
  /** Название аттрибута с высотой. */
  def HEIGHT_ATTR   = CUSTOM_ATTR_PREFIX + "height"


  /** Константы для sc-стадии site: ScSite, siteTpl. */
  object Site {

    /** DOM ID инпута, содержащего id узла, вокруг которого принудительно должна работать выдача. */
    def FORCE_ADN_ID_INPUT_ID = "faiii"

  }


  /** Варианты API выдачи. */
  object Vsns {

    /**
      * Дефолтовое API на сайте.
      * HTTP-ответы представляют из себя JSON-объекты (application/json) с отрендеренным html внутри полей.
      */
    def SITE_JSONHTML       = 2

    /**
      * Модифицированное API v2 для cordova-контейнера мобильного приложения.
      * API изначально было обратно совместимо с v2 (можно рендерить в обычном браузере), но есть отличия в нюансах.
      */
    def CORDOVA_JSONHTML    = 3

  }

  /** Константы геолокации. */
  object Geo {

    /** Сколько браузер клиента может кешировать результат геолокации. */
    def GEOLOC_MAXAGE_MS = 20000

  }


  /** Client/server констатны выдачи для моделей ScReqArgs. */
  object ReqArgs {

    /** Имя поля с данными экрана клиентского устройства. */
    def SCREEN_FN         = "screen"

    /** Имя поля с флагом о том, требуется ли рендерить welome-screen для index-выдачи? */
    def WITH_WELCOME_FN   = "wc"

    /** Имя поля с версией API выдачи. */
    def VSN_FN            = "v"

    /** Имя поля с id предыдущего узла выдачи, т.е. как бы id узла-referrer'а. */
    def PREV_ADN_ID_FN    = "pr"

    /** Имя поля с id текущего узла выдачи. Названа adn вместо node для однозначности, что это наверное ресивер. */
    def ADN_ID_FN         = "n"

    /** Имя поля с данными физического окружения устройства клиента.
      * Пришло на смену старому кривому полю с geo mode. */
    def LOC_ENV_FN        = "e"

  }


  /** Константы логотипа. */
  object Logo {

    /** Высота логотипа узла в css-пикселях. */
    def HEIGHT_CSSPX = 30

    /** Имя виртуального css-класса для логотипов в заголовоках. */
    def HDR_LOGO_DIV_CLASS = "js-hdr-logo"

    /** id контейнера логотипа. Рендерится не всегда. */
    def LOGO_CONT_ID       = "smHdrNodeLogo"
  }


  /** Имя css-класса, полностью скрывающего элемент. */
  def HIDDEN_CSS_CLASS  = "hidden"

  /** css-класс для форсирования вертикального скроллинга во внутренних контейнерах. */
  def OVERFLOW_VSCROLL_CSS_CLASS = "sm-overflow-scrolling"

  /** Константы ответов сервера. */
  object Resp {

    /** Название поля, содержащего id экшена, по которому сгенерирован ответ. */
    final val ACTION_FN  = "action"

    /** Название поля со списком sc-resp-экшенов. */
    final val RESP_ACTIONS_FN = "ra"

    /** Название поля ответа, содержащего строку с html версткой. */
    final val HTML_FN    = "html"

    /** Названия поля с геоточкой, к которому привязан sc-ответ. */
    final val GEO_POINT_FN = "gp"

    /** Название поля с id узла, к которому относится ответ. */
    final val ADN_ID_FN  = "n"

    /** Название поля с неким заголовком. */
    final val TITLE_FN   = "ti"


    /** Отрендеренные карточки (плитка). */
    final val MADS_FN          = "mads"

    /** Поле с таким названием содержит стили. Например, карточек плитки. */
    final val CSS_FN           = "css"

    /** Имя поля, где какие-то параметры. Например, параметры сетки. */
    final val PARAMS_FN        = "params"

    /** Имя поля с версткой списка узлов. */
    def NODE_LIST_HTML_FN = "nodes"

    /** Имя поля с временем генерации ответа. */
    def TIMESTAMP_FN      = "timestamp"

    /** Открытые карточки по API v2. */
    final val FOCUSED_ADS_FN    = "fads"

    /** Имя поля с общим кол-вом чего-то во всей выборке. */
    final val TOTAL_COUNT_FN    = "tc"

    final val STYLES_FN         = "st"

    final val INDEX_RESP_ACTION = "i"

    final val ADS_TILE_RESP_ACTION = "t"

    /** Экшен для focused-данных в ответе. */
    final val FOC_ANSWER_ACTION  = "focused"

  }


  /** Константы строки заголовка выдачи. */
  object Header {

    /** id div'а строки заголовка выдачи. */
    def ROOT_DIV_ID = "smRootProducerHeader"

    /** Название css-класса для отображения кнопки возврата на index выдачи. Появляется при открытой search-панели. */
    def INDEX_ICON_CSS_CLASS = "__w-index-icon"

    /** Название css-класса для режима глобальной категории. */
    def GLOBAL_CAT_CSS_CLASS = "__w-global-cat"

    /** Кнопка возвращения на плитку, отображается слева наверху в ряде случаев. */
    def SHOW_INDEX_BTN_ID = "smIndexButton"

    /** Контейнер базовых кнопок заголовка. */
    def BTNS_DIV_ID = "smRootProducerHeaderButtons"

    /** id кнопки перехода на индекс продьюсера. */
    def GO_TO_PRODUCER_INDEX_BTN_ID = "smProducerIndexBtn"

    /** id кнопки перехода на предыдущий узел.*/
    def PREV_NODE_BTN_ID   = "smNodePrevious"

    /** Название data-аттрибута с id узла. */
    def ATTR_ADN_ID    = CUSTOM_ATTR_PREFIX + "adn-id"
  }


  /** Константы layout. */
  object Layout {

    /** id корневого div выдачи. */
    def ROOT_ID = "sioMartRoot"

    /** имя css-класса корневого элемента. */
    def ROOT_CSS_CLASS = "sm-showcase"

    /** id div'а выдачи. */
    def LAYOUT_ID = "sioMartLayout"

    /** div id для fullscreen loader. */
    def FS_LOADER_ID = "fsLoaded"

  }


  /** Константы inline-ресурсов выдачи. */
  object Rsc {

    /** id контейнера common-ресурсов. */
    def COMMON_ID = "smResources"

    /** id контейнера focused-ресурсов. */
    def FOCUSED_ID = "smResourcesFocused"

  }


  /** Сетка выдачи. */
  object Grid {

    /** Вся структура сетки внутри этого div. */
    def ROOT_DIV_ID       = "smGridAds"

    def WRAPPER_DIV_ID    = ROOT_DIV_ID + DIV_WRAPPER_SUFFIX

    def CONTENT_DIV_ID    = ROOT_DIV_ID + DIV_CONTENT_SUFFIX

    /** Анимированная крутилка внизу списка рекламных карточек, когда ожидается подгрузка ещё карточек. */
    def LOADER_DIV_ID     = ROOT_DIV_ID + DIV_LOADER_SUFFIX

    /** Блоки карточек рендерятся сюда. */
    def CONTAINER_DIV_ID  = ROOT_DIV_ID + DIV_CONTAINER_SUFFIX

    /** Название CSS-класса для активации размывки ROOT. */
    def BLURRED_CLASS     = "__blurred"

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

  }


  /** Панель навигации по системе и её узлам. */
  object NavPane {

    /** Корневой id панели навигации. */
    def ROOT_ID       = "smGeoScreen"

    /** div id контейнера списка узлов. */
    def NODE_LIST_ID  = "smGeoNodes"

    /** content wrapper div id */
    def WRAPPER_ID    = NODE_LIST_ID + DIV_WRAPPER_SUFFIX

    /** content div id. */
    def CONTENT_ID    = NODE_LIST_ID + DIV_CONTENT_SUFFIX

    /** id кнопки ручного запуска геолокации. */
    def FIND_ME_BTN_ID = "smGeoLocationButton"

    /** id кнопки вызова панели навигации. */
    def SHOW_PANEL_BTN_ID = "smGeoScreenButton"

    /** id кнопки скрытия панели. */
    def HIDE_PANEL_BTN_ID = "smGeoScreenCloseButton"

    // GN_ = geo nodes list; GNL_ = GN_ layer

    /** В списке узлов строчка сокрытия-отображения слоя обозначается этим css-классом. */
    def GNL_CAPTION_CSS_CLASS     = "js-gnlayer"
    def GNL_CAPTION_DIV_ID_PREFIX = "geoLayer"

    def GNL_ACTIVE_CSS_CLASS      = ACTIVE_CLASS

    /** Для связывания caption'а слоя и его содержимого используются динамические id. */
    def GNL_ATTR_LAYER_ID_INDEX   = CUSTOM_ATTR_PREFIX + "index"

    /** Класс для тела (подсписка) одного слоя узлов. */
    def GNL_BODY_CSS_CLASS        = "geo-nodes-list_rows"

    def GN_CONTAINER_ID           = "geoNodesListContainer"

    /** Префикс id контейнера списка узлов одного слоя. */
    def GNL_BODY_DIV_ID_PREFIX    = "geoLayerNodes"

    /** Класс контейнера для одного узла в списке узлов. */
    def GN_NODE_CSS_CLASS         = "js-geo-node"

    /** Название аттрибута с id узла в контенере узла в списке узлов. */
    def GN_ATTR_NODE_ID           = CUSTOM_ATTR_PREFIX + "id"

    /** Название аттрибута контейнера, содержащий общее кол-во слоёв. */
    def GN_ATTR_LAYERS_COUNT      = CUSTOM_ATTR_PREFIX + "layers-count"

    /** css-класс, указывающий на скрытость указанного слоя. */
    def GNL_BODY_HIDDEN_CSS_CLASS = "__hidden"

    /** Префикс для id для div'ов узлов в списке узлов. */
    def GNL_NODE_ID_PREFIX        = "gn-"

    def SCREEN_OFFSET     = 129
    def GNL_DOM_HEIGHT    = 44
  }


  /** Константы для рендера приветствия узла. */
  object Welcome {

    /** id корневого div'а приветствия. */
    def ROOT_ID = "smWelcomeAd"

    /** id элемента фоновой картинки. */
    def BG_IMG_ID = "smWelcomeAdBgImage"

    /** id картинки переднего плана. */
    def FG_IMG_ID = "smWelcomeAdfgImage"

    /** id контейнера с логотипом/текстом и т.д. */
    def FG_INFO_DIV_ID = "smWelcomeAdfgText"

    /** Начинать скрывать карточку приветствия через указанное время. */
    def HIDE_TIMEOUT_MS = 1200

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


  /** Константы поисковой панели (справа). */
  object Search {

    /** div id кнопки открытия поисковой панели. */
    def SHOW_PANEL_BTN_ID = "smNavigationLayerButton"

    /** div id кнопки скрытия панели. */
    def HIDE_PANEL_BTN_ID = "smCategoriesScreenCloseButton"

    /** id корневого div'а панели, содержит все нижеперечисленные элементы. */
    def ROOT_DIV_ID = "smCategoriesScreen"

    /** Константы полнотекстового поиска. */
    object Fts {

      /** id контейнера всего fts-механизма. */
      def BAR_ID   = "smSearchBar"

      /** id контейнера для оформления input'а. */
      def INPUT_CONTAINER_ID = "smSearch"

      /** id инпута для полнотекстового поиска. */
      def INPUT_ID = "smSearchField"

      /** Название css-класса для активного поискового поля. */
      def ACTIVE_INPUT_CLASS = ACTIVE_CLASS

      /** Через сколько миллисекунд после окончания ввода запускать запрос текстового поиска. */
      def START_TIMEOUT_MS = 600
    }

    // Табы

    /** Список id табов в порядке их отображения на экране. */
    def TAB_IDS = List(MapTab.TAB_BTN_ID, TagsTab.TAB_BTN_ID)

    /** id div'а, содержащего кнопки всех tab'ов. */
    def TAB_BTNS_DIV_ID = "smNavLayerTabs"

    /** Класс неактивной кнопки таба. */
    def TAB_BTN_INACTIVE_CSS_CLASS = "__inactive"

    /** Суффикс для id div'а кнопки таба. */
    def TAB_BTN_ID_SUFFIX = "Tab"

    /** Интерфейс для id'шников таба. Используется в sc-sjs для полиморфного доступа к DOM-моделям табов с целью дедубликации кода. */
    sealed trait ITab {
      def ROOT_DIV_ID: String
      def TAB_BTN_ID     = ROOT_DIV_ID + TAB_BTN_ID_SUFFIX
      def WRAPPER_DIV_ID = ROOT_DIV_ID + DIV_WRAPPER_SUFFIX
      def CONTENT_DIV_ID = ROOT_DIV_ID + DIV_CONTENT_SUFFIX
    }

    /** Сюда сгруппированы id, относящиеся к категориям. */
    object MapTab extends ITab {

      /** div id списка иконок категорий. Отображается под линейкой табов. */
      override def ROOT_DIV_ID = "smCategories"

    }

    /** Сюда сгруппированы id, относящиеся к списку магазинов. */
    object TagsTab extends ITab {

      /** div id контейнера тела вкладки списка тегов. */
      override def ROOT_DIV_ID = "smShops"

      /** div id списка тегов. */
      def LIST_ID = ROOT_DIV_ID + "List"

      /** Название класса-метки одного ряда в списке тегов-узлов.
        * Такой тег содержит мета-данные о теге в data-аттрибутах.
        * SCv1 использовала одноимённый класс для раскрытия focused-выдачи. */
      def ROW_DIV_CLASS = "js-shop-link"

      /** Имя аттрибута, содержащее id узла графа N2. */
      def ATTR_NODE_ID = _ATTR_PRODUCER_ID

      /** Имя css для визульаного выделения текущего ряда. */
      def SELECTED_CSS_CLASS = "__selected"

      /** Префикс DOM id для рядов тегов. */
      def ROW_ID_PREFIX = "tag."

    }

  }


  /** Константы для уровней отображения выдачи. */
  object ShowLevels {

    /** Отображать на нулевом уровне, т.е. при входе в ТЦ/ресторан и т.д. */
    def ID_START_PAGE = "d"

    /** Отображать в каталоге продьюсеров. */
    def ID_CATS = "h"

    /** Отображать эту рекламу внутри каталога продьюсера. */
    def ID_PRODUCER = "m"
  }


  /** Константы для подвыдачи focused ads. */
  object Focused {

    /** id корневого контейнера. */
    def ROOT_ID = "smFocusedAds"

    /** Контейнер заголовка, стрелочек и прочего хлама, который сопутствует карточкам. */
    def CONTROLS_ID = ROOT_ID + "Controls"

    /** id focused-контейнера-карусели карточек. */
    def CONTAINER_ID = ROOT_ID + DIV_CONTAINER_SUFFIX

    /** id div со стрелкой, отображаемой под курсором мышки. */
    def ARROW_ID = ROOT_ID + "ArrowLabel"

    def arrowClass(handName: String): String = {
      "__" + handName + "-arrow"
    }

    /** Свиг в пикселях по X и Y относительно курсора мыши. */
    def ARROW_OFFSET_PX = 20

    def ARROW_CLASS_LEFT = "left"

    /** Время css-анимации одного слайдинга. */
    def SLIDE_ANIMATE_MS = 200

    /** Класс для анимируемых элементов. */
    def ANIMATED_CSS_CLASS = "__animated"

    /** Активация анимации внутри focused FRoot. */
    def ROOT_TRANSITION_CLASS = "transition-animated"

    /** css-класс анимации для исчезнования за экран. */
    def ROOT_DISAPPEAR_CLASS  = "fs-animated-start"

    /** css-класс анимации появления на экран. */
    def ROOT_APPEAR_CLASS     = "fs-animated-end"

    /** Имя css-класса кнопки закрытия focused-выдачи. */
    def CLOSE_BTN_CLASS       = "sm-producer-header_exit-button"

    /** Кол-во focused-карточек для опережающего preload'а за раз. */
    def SIDE_PRELOAD_MAX      = 2

    /** Кол-во focused-карточек для полного запроса вокруг текущей и включая текущую. */
    def AROUND_LOAD_LIMIT     = 3

    /** Лимит запроса focused-карточек в зависимости от запрашивания предшествующей карточки. */
    def getLimit(withPrevAd: Boolean): Int = {
      if (withPrevAd) AROUND_LOAD_LIMIT else SIDE_PRELOAD_MAX
    }

    /** При первичной фокусировке на карточке: запрашивать ли с сервера карточку, предшествующую текущей? */
    def isWithPrevAd(currIndex: Int): Boolean = {
      currIndex > 0
    }

    /** Подстроить значение индекса под значение isWithPrevAd.
      * Т.е. когда решено грабить вместе с предшествующей карточкой,
      * то нужно начинать получать карточки с предыдущего индекса.
      */
    def currIndex2Offset(currIndex: Int, withPrevAd: Boolean): Int = {
      if (withPrevAd) currIndex - 1 else currIndex
    }


    object FAd {
      /** Префикс для DOM ID'шников контейнеров focused-карточки. */
      def ID_PREFIX = "focusedAd"

      /** Суффикс id sm-block div для focused-карточек. */
      def BLOCK_ID_SUFFIX = "fblk"

      /** При скроллинге стрелками клавиатуры какой должен быть шаг скроллинга в пикселях? */
      def KBD_SCROLL_STEP_PX = 60

      /** Имя аттрибута, хранящего id карточки. */
      def MAD_ID_ATTR = Block.MAD_ID_ATTR

    }

  }

  /** Константы для jsRouter'а выдачи. */
  object JsRouter {

    /**
     * Имя js-роутера на странице. val потому что:
     * - в sjs используется в аннотации.
     * - в web21 будет постоянно использоваться с внедрением sjs-выдачи.
     */
    final val NAME = "sioScJsRoutes"

    /** window.NAME - название функции function(), которая будет вызвана  */
    final val ASYNC_INIT_FNAME = NAME + "AsyncInit"

    /** id script-тега js-роутера. HTML5 разрешает script.id. */
    def DOM_ID = "scJsRouterCont"

    /** relative-ссылка на скрипт js-роутера. */
    def URI = "/sc/router.js"

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
    val ADN_ID_FN               = "m.id"
    val GENERATION_FN           = "a.gen"
    val TILES_CAT_ID_FN         = "t.cat"

    val CAT_SCR_OPENED_FN       = "s.open"
    val SEARCH_TAB_FN           = "s.tab"

    val FADS_CURRENT_AD_ID_FN   = "f.cur.id"
    val FADS_OFFSET_FN          = "f.off"
    val PRODUCER_ADN_ID_FN      = "f.pr.id"

    val NAV_NGLS_STATE_MAP_FN   = "n.ngls"
    val GEO_SCR_OPENED_FN       = "n.open"

  }

}
