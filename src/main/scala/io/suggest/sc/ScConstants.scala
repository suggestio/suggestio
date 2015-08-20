package io.suggest.sc

import io.suggest.common.MHandsBaseT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.05.15 16:08
 * Description: Константы для выдачи.
 */
object ScConstants {

  /**
   * Имя js-роутера на странице. val потому что:
   * - в sjs используется в аннотации.
   * - в web21 будет постоянно использоваться с внедрением sjs-выдачи.
   */
  final val JS_ROUTER_NAME = "sioScJsRoutes"

  /** window.NAME - название функции function(), которая будет вызвана  */
  final val JS_ROUTER_ASYNC_INIT_FNAME = JS_ROUTER_NAME + "AsyncInit"

  def DIV_WRAPPER_SUFFIX   = "Wrapper"
  def DIV_CONTENT_SUFFIX   = "Content"
  def DIV_CONTAINER_SUFFIX = "Container"
  def DIV_LOADER_SUFFIX    = "Loader"

  /** Название класса активности.
    * Есть несколько css-классов с одинаковыми именами, но в разных scope'ах. */
  private def ACTIVE_CLASS = "__active"

  /** Имя css-класс с will-change для подготовки к translate3d. */
  def CLASS_WILL_TRANSLATE3D = "will-translate3d"

  /** Client/server констатны выдачи для моделей ScReqArgs. */
  object ReqArgs {

    def GEO               = "geo"
    def SCREEN            = "screen"
    def WITH_WELCOME      = "wc"
    def VSN               = "v"
    /** id предыдущего узла выдачи, т.е. как бы id узла-referrer'а. */
    def PREV_ADN_ID_FN    = "pr"
  }

  object Logo {

    /** Высота логотипа узла в css-пикселях. */
    def HEIGHT_CSSPX = 30

    /** Имя виртуального css-класса для логотипов в заголовоках. */
    def HDR_LOGO_DIV_CLASS = "js-hdr-logo"
  }


  /** Имя css-класса, полностью скрывающего элемент. */
  def HIDDEN_CSS_CLASS  = "hidden"

  /** css-класс для форсирования вертикального скроллинга во внутренних контейнерах. */
  def OVERFLOW_VSCROLL_CSS_CLASS = "sm-overflow-scrolling"

  /** Константы ответов сервера. */
  object Resp {

    /** Название поля, содержащего id экшена, по которому сгенерирован ответ. */
    final val ACTION_FN  = "action"

    /** Название поля ответа, содержащего строку с html версткой. */
    final val HTML_FN    = "html"

    /** Название поля, которое содержит флаг того, была ли использована геолокация для генерации результата? */
    final val IS_GEO_FN  = "is_geo"

    /** Название поля с id узла, к которому относится ответ. */
    final val ADN_ID_FN  = "curr_adn_id"

    /** Название поля, содержащее массив отрендеренных карточек плитки. */
    final val BLOCKS_FN  = "blocks"

    def MADS_FN          = "mads"

    /** Поле с таким названием содержит стили. */
    def CSS_FN           = "css"

    /** Имя поля, где какие-то параметры. Например, параметры сетки. */
    def PARAMS_FN        = "params"

    /** Имя поля с версткой списка узлов. */
    def NODE_LIST_HTML_FN = "nodes"

    /** Имя поля с временем генерации ответа. */
    def TIMESTAMP_FN      = "timestamp"

    /** Открытые карточки по API v2. */
    def FOCUSED_ADS_FN    = "fads"

    /** Имя поля с общим кол-вом чего-то во всей выборке. */
    def TOTAL_COUNT_FN    = "tc"

    def STYLES_FN         = "st"
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
    def ATTR_ADN_ID    = "data-adn-id"
  }


  /** Константы layout. */
  object Layout {

    /** id корневого div выдачи. */
    def ROOT_ID = "sioMartRoot"

    /** имя css-класса корневого элемента. */
    def ROOT_CSS_CLASS = "sm-showcase"

    def LAYOUT_ID = "sioMartLayout"

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

  }

  /** Константы для отрендеренных карточек. */
  object Block {

    /** Название аттрибута с длиной блока. */
    def BLK_WIDTH_ATTR    = "data-width"
    /** Название аттрибута с высотой блока. */
    def BLK_HEIGHT_ATTR   = "data-height"

    /** Имя аттрибута с порядковым номером в плитке от нуля. */
    def BLK_INDEX_ATTR    = "data-index"
    /** Имя аттрибута с id текущей рекламной карточки. */
    def MAD_ID_ATTR       = "data-mad-id"

    def ID_DELIM          = ":"
    /** id grid-блока формируется как-то так: "$madId$ID_SUFFIX". */
    def ID_SUFFIX         = "blk"
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
    def GNL_ATTR_LAYER_ID_INDEX   = "data-index"

    /** Класс для тела (подсписка) одного слоя узлов. */
    def GNL_BODY_CSS_CLASS        = "geo-nodes-list_rows"

    def GN_CONTAINER_ID           = "geoNodesListContainer"

    /** Префикс id контейнера списка узлов одного слоя. */
    def GNL_BODY_DIV_ID_PREFIX    = "geoLayerNodes"

    /** Класс контейнера для одного узла в списке узлов. */
    def GN_NODE_CSS_CLASS         = "js-geo-node"

    /** Название аттрибута с id узла в контенере узла в списке узлов. */
    def GN_ATTR_NODE_ID           = "data-id"

    /** Название аттрибута контейнера, содержащий общее кол-во слоёв. */
    def GN_ATTR_LAYERS_COUNT      = "data-layers-count"

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
    def TAB_IDS = List(Cats.TAB_BTN_ID, Nodes.TAB_BTN_ID)

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
    object Cats extends ITab {
      /** div id списка иконок категорий. Отображается под линейкой табов. */
      override def ROOT_DIV_ID = "smCategories"

      /** css-класс пометка, сообщающая о том, что данный элемент должен бы подхватываться js'ом. */
      def ONE_CAT_LINK_CSS_CLASS = "js-cat-link"

      /** Название аттрибута, которое содержит id категории. */
      def ATTR_CAT_ID = "data-cat-id"

      /** css-класс категории. Для выставления в header. */
      def ATTR_CAT_CLASS = "data-cat-class"
    }

    /** Сюда сгруппированы id, относящиеся к списку магазинов. */
    object Nodes extends ITab {
      /** div id списка магазинов. */
      override def ROOT_DIV_ID = "smShops"
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


    object FAd {
      /** Префикс для DOM ID'шников контейнеров focused-карточки. */
      def ID_PREFIX = "focusedAd"

      /** Суффикс id sm-block div для focused-карточек. */
      def BLOCK_ID_SUFFIX = "fblk"
    }

  }

}
