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


  object Tile {

    def TILE_DIV_ID = "smGridAds"



  }

}
