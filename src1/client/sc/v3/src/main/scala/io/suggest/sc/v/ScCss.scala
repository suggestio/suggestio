package io.suggest.sc.v

import io.suggest.color.{MColorData, MColors}
import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.css.Css
import io.suggest.css.ScalaCssDefaults._
import io.suggest.i18n.MsgCodes
import io.suggest.math.SimpleArithmetics._
import io.suggest.sc.ScConstants
import io.suggest.sc.m.MScCssArgs
import japgolly.univeq.UnivEq

import scala.language.postfixOps

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 15:01
  * Description: scalaCSS для выдачи s.io.
  */

object ScCss {

  // TODO Исходные цвета надо бы брать откуда-то, с сервера например.
  // TODO val - т.к. нужен твёрдый инстанс для eq-сравнивания. Но это нужно только на первых секундах выдачи.
  def COLORS_DFLT = MColors(
    bg = Some(MColorData( "111111" )),
    fg = Some(MColorData.Examples.WHITE)
  )

  /**
    * Некоторые браузеры скроллят внешний контейнер вместо внутреннего, когда оба скроллабельны.
    * С помощью это флага можно активировать логику перезаписи scrollbar'а внутренним контейнером,
    * чтобы быстро исправить недоразумение.
    * @return true, когда требуется спровоцировать внутренний скроллбар через шаманство с высотами.
    *         false, когда костыли не нужны (по умолчанию).
    */
  lazy val needOverrideScroll: Boolean = {
    val isSafari = cssEnv.platform.name contains "Safari"
    /*
     * Мобильная сафари не может скроллить внутренний контейнер, когда можно скроллить внешний.
     * Нужно форсировать появление скроллбара у внутреннего контейнера.
     * TODO Проверить, нуждается ли ipad в таком костыле.
     * TODO Актуально ли это всё в 2017 году?
     * Кажется без этого вообще скроллинг в плитке не работает нигде. Как вариант, сделать overflow-y: auto.
     */
    isSafari &&
      cssEnv.platform.userAgent.exists( _ contains "Mobile/" )
  }

  /** Сдвиг по вертикали от начала наполнения search-панели.
    * Появился для выравнивания полосы input'а относительно полосы заголовка. */
  def SEARCH_TOP_OFFSET_PX = 4

  /**
    * Оффсет при наличии заголовка табов поиска.
    * Если нет заголовка табов, то можно делать бОльший offset.
    * if (tabsHdr.isEmpty) 115 else 165
    */
  val TABS_OFFSET_PX = 65 + SEARCH_TOP_OFFSET_PX

  /** Высота заголовка. */
  def HEADER_HEIGHT_PX = 50

  @inline implicit def univEq: UnivEq[ScCss] = UnivEq.derive

  // z-index-костыли для расположения нескольких панелей и остального контента.
  // Жило оно одиноко в scCss, но пока унесено сюда после рефакторинга в DI/ScCss.
  def sideBarZIndex = 11

  // Значение "initial" для css-свойст.
  def css_initial = scalacss.internal.Literal.Typed.initial.value

}


/** Стили для выдачи.
  *
  * @param args Цвета и экран для оформления выдачи.
  */
final case class ScCss( args: MScCssArgs ) extends StyleSheet.Inline {

  import dsl._

  private val (_bgColorCss, _fgColorCss) = {
    val colors = args.customColorsOpt getOrElse ScCss.COLORS_DFLT
    def _colorCss( colorOpt: Option[MColorData], dflt: => String ) =
      Color( colorOpt.fold(dflt)(_.hexCode) )
    val bg = _colorCss( colors.bg, ScConstants.Defaults.BG_COLOR )
    val fg = _colorCss( colors.fg, ScConstants.Defaults.FG_COLOR )
    (bg, fg)
  }

  /** Стиль цвета фона для произвольного элемента выдачи. */
  val bgColor = style(
    backgroundColor( _bgColorCss ),
  )

  val fgColor = style(
    color( _fgColorCss )
  )


  object Welcome {

    import ScCssStatic.Welcome._SM_WELCOME_AD

    private def _imgWhMixin(wh: ISize2di, margin: ISize2di) = {
      mixin(
        height( wh.height.px ),
        width( wh.width.px ),
        marginTop( margin.height.px ),
        marginLeft( margin.width.px )
      )
    }

    /** Контейнер стилей элементов фона экрана приветствия. */
    object Bg {

      /** Стили фонового изображения экрана приветствия */
      val bgImg = {
        // В зависимости от наличия или отсутствия размера welcome background, стили могут отличаться.
        val whMx = args.wcBgWh.fold( StyleS.empty ) { wh0 =>
          val screenWh = args.screenInfo.screen.wh
          val wh2 = if (ISize2di.whRatio(wh0) < ISize2di.whRatio(screenWh)) {
            val w = screenWh.width
            MSize2di(
              width  = w,
              height = w * wh0.height / wh0.width
            )
          } else {
            val h = screenWh.height
            MSize2di(
              width  = h * wh0.width / wh0.height,
              height = h
            )
          }
          val margin2 = wh2 / (-2)
          _imgWhMixin( wh2, margin2 )
        }

        style(
          addClassName( _SM_WELCOME_AD + "_bg-img" ),
          backgroundColor( _bgColorCss ),
          whMx
        )
      }

    }


    /** Контейнер стилей элементов переднего плана экрана приветствия. */
    object Fg {

      /** Стили логотипа экрана приветствия. */
      val fgImg = {
        // Подготнка логотипа приветствия под текущий экран: центровка.
        val whMx = args.wcFgWh.fold(StyleS.empty) { wh0 =>
          val wh2 = wh0 / 2
          val margin0 = wh2 / (-2)
          val margin2 = MSize2di.height.modify(_ + 25)(margin0)
          _imgWhMixin( wh2, margin2 )
        }

        style(
          addClassName( _SM_WELCOME_AD + "_fg-img" ),
          whMx
        )
      }

      val fgText = ScCssStatic._styleAddClass( _SM_WELCOME_AD + "_fg-text" )

      val helper = ScCssStatic._styleAddClass( _SM_WELCOME_AD + "_helper" )
    }

  }


  /** Стили для заголовка. */
  object Header {

    /** Стили для прогресс-бара. */
    val progress = style(
      position.absolute,
      bottom(0.px),
      width(100.%%),
      backgroundColor( _fgColorCss ),
      height(1.px),
    )

    /** Стили контейнера любого заголовка. */
    val header = {
      style(
        addClassNames(
          ScCssStatic.Header.HEADER,
          Css.Position.ABSOLUTE
        ),
        backgroundColor( _bgColorCss ),
        borderColor( _fgColorCss ),
        // Для экранов с вырезами (iphone10) - расширяем заголовок вниз по вертикали:
        height( ScCss.HEADER_HEIGHT_PX.px ),
        // TODO На гориз.смартфоне криво, на декстопе - норм.
        //left( args.screenInfo.unsafeOffsets.left.px ),
        // При выезде левой панели, заголовок ужимается в несколько строчек. Нельзя так.
        minWidth( 200.px ),
        paddingTop( args.screenInfo.unsafeOffsets.top.px )
      )
    }


    /** Доступ к стилям логотипа узла. */
    object Logo {

      /** Суффикс названия css-классов разных логотипов. */
      private def `logo_` = "logo"

      /** Стили текстового логотипа узла.*/
      object Txt {

        private val TXT_LOGO = ScCssStatic.Header.HEADER + "_txt-" + `logo_`

        val logo = style(
          addClassName( TXT_LOGO )
        )

        val colored = style(
          color( _fgColorCss ),
          borderColor( _fgColorCss )
        )

        /** Точки по краям названия узла. */
        object Dots {
          private val DOT = TXT_LOGO + "-dot"

          /** Стиль для одной точки. */
          val dot = style(
            addClassName( DOT ),
          )

          /** Цвет точки. */
          val dotColor = style(
            backgroundColor( _fgColorCss )
          )

          val left = ScCssStatic._styleAddClass( Css.__ + MsgCodes.`left` )
          val right = ScCssStatic._styleAddClass( Css.__ + MsgCodes.`right` )
        }

      }

    }

  }


  /** Панель поиска. */
  object Search {

    def PANEL_WIDTH_PX = 320

    /** CSS-класс div-контейнера правой панели. */
    val panel = style(
      width( PANEL_WIDTH_PX.px ),
    )

    val content = {
      val paddingTopPx = args.screenInfo.unsafeOffsets.top
      style(
        paddingTop( paddingTopPx.px ),
        maxHeight( (args.screenInfo.screen.wh.height - paddingTopPx).px )
      )
    }

    /** CSS-класс заголовка внутри панели поиска. */
    //val panelHeader = _styleAddClasses( _PANEL + "_header" )


    /** Табы на поисковой панели. */
    object Tabs {

      /** Стили содержимого вкладки с гео-картой. */
      object MapTab {

        private val TAB_BODY_HEIGHT_PX = {
          val si = args.screenInfo
          si.screen.wh.height - si.unsafeOffsets.top
        }

        private val TAB_BODY_HEIGHT    = height( TAB_BODY_HEIGHT_PX.px )


        /** Стиль внешнего контейнера. */
        val outer = style(
          ScCssStatic.smFlex,
          TAB_BODY_HEIGHT
        )

        /** Стиль wrap-контейнера. */
        val wrapper = style(
          ScCssStatic.overflowScrollingMx,
          ScCssStatic.smFlex,
          TAB_BODY_HEIGHT
        )

        val inner = {
          /** Форсировать скроллбар во внутреннем контейнере, если этого требует окружение. */
          val TAB_BODY_CONTENT_HEIGHT = if (ScCss.needOverrideScroll)
            height( (TAB_BODY_HEIGHT_PX + 1).px )
          else
            TAB_BODY_HEIGHT
          val OUTER = Css.Lk._SM_PREFIX_ + "categories"
          style(
            addClassName( OUTER + "_content" ),
            TAB_BODY_CONTENT_HEIGHT
          )
        }

        /** Стиль контейнера карты. Контейнер порождается js'кой гео-карты, а не нами. */
        val geomap = {
          val pc100 = 100.%%
          style(
            width( pc100 ),
            height( pc100 )
          )
        }

        /** Контейнер прицела центра карты. */
        val crosshair = style(
          position.relative,
          top( -(TAB_BODY_HEIGHT_PX / 2 + 12).px ),
          left(48.5 %%),
          zIndex(1000)
        )

      }

    }

  }


  /** Стили для плитки карточек, точнее для контейнеров этой плитки. */
  object Grid {

    import ScCssStatic.Grid._SM_GRID_ADS

    private val _screenHeightPx = {
      val si = args.screenInfo
      (si.screen.wh.height - si.unsafeOffsets.top).px
    }
    private val _screenHeight = height( _screenHeightPx )

    /** Крутилка внизу экрана. */
    val loader = style(
      left( (args.screenInfo.screen.wh.width / 2).px ),
      position.relative,
    )

    val outer = style(
      addClassName( _SM_GRID_ADS ),
      _screenHeight,
      backgroundColor( _bgColorCss ),
      paddingTop( args.screenInfo.unsafeOffsets.top.px ),
      // TODO Гориз. iphone10 - разъезжается.
      //paddingLeft( args.screenInfo.unsafeOffsets.left.px )
    )

    val wrapper = style(
      ScCssStatic.overflowScrollingMx,
      _screenHeight,
      overflowX.hidden
    )

    val content = style(
      addClassName( _SM_GRID_ADS + "_content" )
    )

    val container = style(
      minHeight( _screenHeightPx ),
    )

  }


  /** Стили для панели меню. */
  object Menu {

    val panel = style(
      width( (280 + args.screenInfo.unsafeOffsets.left).px )
    )

    val content = {
      val minPaddingTopPx = 5
      val uo = args.screenInfo.unsafeOffsets
      val paddingTopPx = uo.top + minPaddingTopPx
      style(
        paddingTop( paddingTopPx.px ),
        paddingLeft( Math.max(5, uo.left).px ),
        maxHeight( (args.screenInfo.screen.wh.height - paddingTopPx).px ),
        overflow.auto
      )
    }

    val version = style(
      bottom( args.screenInfo.unsafeOffsets.bottom.px ),
    )

  }




  /** Инициализация ленивых scala-объектов для заполнения стилей выдачи.
    *
    * Некоторые вещи не требуют явной инициализации, т.к. не содержат какой-либо стилистики.
    * Поэтому, тут перечислены объекты, стили которых подразумевают css-рендер.
    *
    * @see [[https://japgolly.github.io/scalacss/book/gotchas.html]]
    */
  initInnerObjects(
    Welcome.Bg.bgImg,
    Welcome.Fg.fgImg,

    Header.Logo.Txt.Dots.dot,

    Search.Tabs.MapTab.inner,

    Grid.container,
    Menu.panel,
  )

}
