package io.suggest.sc

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


  /** Client/server констатны выдачи для моделей ScReqArgs. */
  object ReqArgs {

    final val GEO               = "geo"
    final val SCREEN            = "screen"
    final val WITH_WELCOME      = "wc"
    final val VSN               = "v"

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

  }


  /** Константы строки заголовка выдачи. */
  object Header {

    /** id div'а строки заголовка выдачи. */
    def ROOT_DIV_ID = "smRootProducerHeader"

    /** Название css-класса для отображения кнопки возврата на index выдачи. Появляется при открытой search-панели. */
    def INDEX_ICON_CSS_CLASS = "__w-index-icon"

    /** Кнопка возвращения на плитку, отображается слева наверху в ряде случаев. */
    def SHOW_INDEX_BTN_ID = "smIndexButton"

  }


  /** Константы layout. */
  object Layout {

    /** id корневого div выдачи. */
    val ROOT_ID = "sioMartRoot"

    /** имя css-класса корневого элемента. */
    val ROOT_CSS_CLASS = "sm-showcase"

    val LAYOUT_ID = "sioMartLayout"

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

    def WRAPPER_DIV_ID    = "smGridAdsWrapper"

    def CONTENT_DIV_ID    = "smGridAdsContent"

    /** Анимированная крутилка внизу списка рекламных карточек, когда ожидается подгрузка ещё карточек. */
    def LOADER_DIV_ID     = "smGridAdsLoader"

    /** Блоки карточек рендерятся сюда. */
    def CONTAINER_DIV_ID  = "smGridAdsContainer"

  }

  /** Константы для отрендеренных карточек. */
  object Block {

    /** Название аттрибута с длиной блока. */
    def BLK_WIDTH_ATTR    = "data-width"
    /** Название аттрибута с высотой блока. */
    def BLK_HEIGHT_ATTR   = "data-height"

  }


  /** Панель навигации по системе и её узлам. */
  object NavPane {

    /** Корневой id панели навигации. */
    def ROOT_ID       = "smGeoScreen"

    /** div id контейнера списка узлов. */
    def NODE_LIST_ID  = "smGeoNodes"

    /** content wrapper div id */
    def WRAPPER_ID    = "smGeoNodesWrapper"

    /** content div id. */
    def CONTENT_ID    = "smGeoNodesContent"

    /** id кнопки ручного запуска геолокации. */
    def FIND_ME_BTN_ID = "smGeoLocationButton"

    /** id кнопки вызова панели навигации. */
    def SHOW_PANE_BTN_ID = "smGeoScreenButton"

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

    /** id инпута для полнотекстового поиска. */
    def FTS_FIELD_ID = "smSearchField"

    /** Список id табов в порядке их отображения на экране. */
    def TAB_IDS = List(Cats.TAB_BTN_ID, Nodes.TAB_BTN_ID)

    /** id div'а, содержащего кнопки всех tab'ов. */
    def TAB_BTNS_DIV_ID = "smNavLayerTabs"

    /** Интерфейс для id'шников таба. Используется в sc-sjs для полиморфного доступа к DOM-моделям табов с целью дедубликации кода. */
    sealed trait ITab {
      def ROOT_DIV_ID: String
      def TAB_BTN_ID     = ROOT_DIV_ID + "Tab"
      def WRAPPER_DIV_ID = ROOT_DIV_ID + "Wrapper"
      def CONTENT_DIV_ID = ROOT_DIV_ID + "Content"
    }

    /** Сюда сгруппированы id, относящиеся к категориям. */
    object Cats extends ITab {
      /** div id списка иконок категорий. Отображается под линейкой табов. */
      override def ROOT_DIV_ID = "smCategories"
    }

    /** Сюда сгруппированы id, относящиеся к списку магазинов. */
    object Nodes extends ITab {
      /** div id списка магазинов. */
      override def ROOT_DIV_ID = "smShops"
    }

  }

}
