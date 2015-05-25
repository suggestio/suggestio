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

  }
  
  /** Классы для анимации через css-трансформации. */
  object CssAnim {
    
    /** css transition на 0.2сек. */
    def TRANS_02_CSS_CLASS = "__animated"
    
    /** Анимация плавного сокрытия. */
    def FADEOUT_CSS_CLASS  = "__fade-out"
    
  }

}
