package io.suggest.sc.styl

import io.suggest.color.{MColorData, MColors}
import io.suggest.css.ScalaCssDefaults._
import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.dev.MScreenInfo
import io.suggest.font.MFonts
import io.suggest.i18n.MsgCodes
import io.suggest.sc.ScConstants
import io.suggest.math.SimpleArithmetics._
import japgolly.univeq.UnivEq
import Css.__
import Css.Lk.{_SM_PREFIX_ => _SM_}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 15:01
  * Description: scalaCSS для выдачи s.io.
  */

object ScCss {

  // TODO Исходные цвета надо бы брать откуда-то, с сервера например.
  // TODO val - т.к. нужен твёрдый инстанс для eq-сравнивания. Но это нужно только на первых секундах выдачи.
  val COLORS_DFLT = MColors(
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
    val isSafari = cssEnv.platform.name.contains("Safari")
    /*
     * Мобильная сафари не может скроллить внутренний контейнер, когда можно скроллить внешний.
     * Нужно форсировать появление скроллбара у внутреннего контейнера.
     * TODO Проверить, нуждается ли ipad в таком костыле.
     * TODO Актуально ли это всё в 2017 году?
     * Кажется без этого вообще скроллинг в плитке не работает нигде. Как вариант, сделать overflow-y: auto.
     */
    isSafari &&
      cssEnv.platform.userAgent.exists( _.contains("Mobile/") )
  }

  /** Сдвиг по вертикали от начала наполнения search-панели.
    * Появился для выравнивания полосы input'а относительно полосы заголовка. */
  val SEARCH_TOP_OFFSET_PX = 4

  /**
    * Оффсет при наличии заголовка табов поиска.
    * Если нет заголовка табов, то можно делать бОльший offset.
    * if (tabsHdr.isEmpty) 115 else 165
    */
  val TABS_OFFSET_PX = 65 + SEARCH_TOP_OFFSET_PX

  /** Высота заголовка. */
  val HEADER_HEIGHT_PX = 50

  @inline implicit def univEq: UnivEq[ScCss] = UnivEq.derive

}


/** Статические стили ScCss, которые не меняются в ходе работы выдачи. */
object ScCssStatic extends StyleSheet.Inline {

  import dsl._

  //def _styleAddClasses(cssClasses: String*) = style( addClassNames(cssClasses: _*) )
  def _styleAddClass(cssClass: String) = style( addClassName(cssClass) )

  val overflowScrollingMx: StyleS = {
    if (ScCss.needOverrideScroll) {
      mixin(
        addClassName( _SM_ + "overflow-scrolling" )
      )
    } else {
      mixin(
        overflowY.auto
      )
    }
  }

  val smFlex = _styleAddClass( _SM_ + "flex" )

  object Body {

    /** Главный обязательный статический класс для body. */
    val smBody = style(
      //addClassName( _SM_ + "body" ),
      overflow.hidden
    )

  }

  /** Стили для #sioMartRoot и каких-то вложенных контейнеров */
  object Root {

    /** корневой div-контейнер. */
    val root = ScCssStatic._styleAddClass( _SM_ + "showcase" )

    /** Общий стиль для всех панелей. */
    val panelCommon = style(
      height( 100.%% ),
    )

    /** Фон одной панели. */
    val panelBg = {
      val pc100 = 100.%%
      val px0 = 0.px
      style(
        position.absolute,
        top(px0),
        left(px0),
        width( pc100 ),
        height( pc100 ),
        // 90% - эффекта затемнения сливается с незатемённой областью. 80% - эффект уже виден.
        filter := "brightness(80%)",
        zIndex(-1)
      )
    }

  }


  /** Стили для заголовка. */
  object Header {

    /** Корневое имя класса, от которого идёт остальное словообразование. */
    def HEADER = _SM_ + "producer-header"

    object Buttons {

      val btn = style(
        addClassNames(
          Css.Lk._SM_BUTTON,
          ScCssStatic.Header.HEADER + "_btn"
        )
      )

      /** Выравнивание кнопок. */
      object Align {
        // Новые стили
        val leftAligned = style(
          left( 5.px )
        )
        val rightAligned = style(
          left.auto,
          right( 5.px )
        )

      }

      /** Кнопка "назад" относительно кнопки меню. */
      val backBtn = style(
        addClassName( Css.Floatt.LEFT ),
        //left( 40.px ),
        //top( (args.screenInfo.unsafeOffsets.top + 1).px ),
        position.relative,
      )

      //val leftGeoBtn = _styleAddClasses( _btnMx, HEADER + "_geo-button", Align.LEFT )

      private def _btnClass(root: String): String =
        ScCssStatic.Header.HEADER + HtmlConstants.UNDERSCORE + root + HtmlConstants.MINUS + Css.Lk.`BUTTON`

      /** Стиль кнопки поиска. */
      val search = style(
        addClassNames(
          _btnClass("search"),
          Align.rightAligned.htmlClass,
          Css.Floatt.RIGHT,
          btn.htmlClass,
        ),
        position.relative,
        top( 5.px )
      )

      /** Стиль кнопки меню слева. */
      val menu = style(
        addClassNames(
          Align.leftAligned.htmlClass,
          btn.htmlClass,
          Css.Floatt.LEFT,
        ),
        position.relative,
        top( 7.px ),
      )

    }

    object Logo {

      object Img {

        val hdr = style(
          position.relative,
          // было 10px, но в итоге получилось 5 после унификацией с логотипами карты.
          padding( ((ScCss.HEADER_HEIGHT_PX - ScConstants.Logo.HEIGHT_CSSPX) / 2).px )
        )

      }

    }

  }


  /** Плитка. */
  object Grid {

    val _SM_GRID_ADS = _SM_ + "grid-ads"

    val container = style(
      addClassName( _SM_GRID_ADS + "_container" ),
      left(0.px),
      opacity(1),
      // 2017-12-14: Чтобы блоки могли за экран выезжать: нужно для широких карточек.
      overflow.initial
    )

  }


  /** Стили для нотификации. */
  object Notifies {

    /** Стиль для SnackbackContentText.action, чтобы кнопки отображались вертикально. */
    val snackActionCont = style(
      display.block
    )

    /** Внутренняя иконка кнопки требует выравнивания вручную. */
    val smallBtnSvgIcon = style(
      paddingRight( 4.px ),
      marginTop( -4.px )
    )

    /** Стиль для контейнера наполнения. */
    val content = style(
      marginTop(10.px)
    )

    /** Кнопка отмены справа наверху. */
    val cancel = style(
      float.right,
      top(-5.px),
    )

  }

  object Welcome {

    val _SM_WELCOME_AD = _SM_ + "welcome-ad"

    val welcome = style(
      addClassNames( _SM_WELCOME_AD ),
    )

  }


  object Search {

    /** Поля текстового поиска и контейнер оной. */
    object TextBar {

      // TODO Статический стиль - унести в статику.
      val bar = style(
        addClassName( _SM_ + "search-bar" ),
        // Равняем полосу input'а с полосой заголовка.
        marginTop( ScCss.SEARCH_TOP_OFFSET_PX.px ),
        display.inlineFlex,
      )

      val inputFormControl = style(
        flexDirection.initial,
        width(100.%%)
      )

    }

    /** Стили для списка найденных узлов (тегов и т.д.). */
    object NodesFound {

      val nodesList = {
        val zeroPx = 0.px
        style(
          paddingTop( zeroPx ),
          paddingBottom( zeroPx ),
          overflow.hidden
        )
      }

      /** Горизонтальный прогресс-бар запроса. */
      val linearProgress = {
        val h = 5.px
        style(
          marginTop( h ),
          height( h )
        )
      }

      /** Список тегов. */
      val listDiv = ScCssStatic._styleAddClass( "shops-list" )

      /** Стиль иконки узла в списке узлов. */
      val nodeLogo = style(
        verticalAlign.middle,
        marginLeft(6.px),
        maxHeight(30.px),
        maxWidth(140.px),
        // без disableGutters, нужно рубить правый отступ
        marginRight(0.px).important
      )

      /** Выставить сдвиги по бокам. gutters ставят 24px, без них просто 0px. А надо нечто среднее. */
      val adnNodeRow = {
        val px16 = 16.px
        style(
          paddingLeft(px16).important,
          paddingRight(px16).important
        )
      }

      /** Ряд тега. ruby для - вертикальной упаковки тегов. */
      val tagRow = style(
        display.inlineFlex,
        paddingTop( 2.px ),    // Было 6px, не помню уже почем
        paddingBottom( 0.px ),    // Было 6px, не помню уже почем
        width.auto,
        paddingLeft(8.px),
        paddingRight(4.px)
      )

      val tagRowText = style(
        paddingLeft( 8.px ),
        paddingRight(0.px)
      )

      val tagRowIcon = {
        val side = 0.7.em
        style(
          width( side ),
          height( side )
        )
      }

      /** div иконки тега. */
      val tagRowIconCont = style(
        fontSize(16.px),
        verticalAlign.middle,
        marginRight.initial,
      )

      /** Слишком большой шрифт у тегов, уменьшить. */
      val tagRowTextPrimary = style(
        fontSize( 0.9.rem )
      )

    }

  }


  object Menu {

    /** Стили строк меню */
    object Rows {

      val rowsContainer = style(
        position.relative
      )

      val rowLink = style(
        textDecoration := none
      )

      val rowText = style(
        height( 28.px )
      )

      /** стили для mui-switch в пунктах меню. */
      val switch = style(
        marginTop( -16.px )
      )

      /** стили для mui-switch base в пунктах меню. */
      val switchBase = style(
        height(28.px)
      )

      /** Стили строк меню. */
      val rowContent = style(
        position.relative,
        minWidth( 260.px ),
        margin(0.px, auto),
        fontFamily.attr := MFonts.OpenSansLight.fileName,
        fontSize( 14.px ),
        padding(12.px, 0.px),
        textTransform.uppercase
      )

    }

  }


  initInnerObjects(
    Body.smBody,
    Root.root,
    Header.Buttons.search,
    Header.Buttons.Align.leftAligned,
    Header.Logo.Img.hdr,
    Grid.container,
    Notifies.snackActionCont,
    Welcome.welcome,
    Search.NodesFound.listDiv,
    Search.TextBar.bar,
    Menu.Rows.rowContent,
  )

}


/** Интерфейс модели параметров для вызова ScCss. */
trait IScCssArgs {
  def customColorsOpt   : Option[MColors]
  def screenInfo        : MScreenInfo
  def wcBgWh            : Option[MSize2di]
  def wcFgWh            : Option[MSize2di]
}
object IScCssArgs {
  implicit def univEq: UnivEq[IScCssArgs] = UnivEq.force
}


/** Стили для выдачи.
  *
  * @param args Цвета и экран для оформления выдачи.
  */
case class ScCss( args: IScCssArgs )
  extends StyleSheet.Inline
{

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
          val wh2 = if (ISize2di.whRatio(wh0) < ISize2di.whRatio(args.screenInfo.screen)) {
            val w = args.screenInfo.screen.width
            MSize2di(
              width  = w,
              height = w * wh0.height / wh0.width
            )
          } else {
            val h = args.screenInfo.screen.height
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
          val margin2 = margin0.withHeight( margin0.height + 25 )
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

          val left = ScCssStatic._styleAddClass( __ + MsgCodes.`left` )
          val right = ScCssStatic._styleAddClass( __ + MsgCodes.`right` )
        }

      }

    }

  }


  /** Панель поиска. */
  object Search {

    def Z_INDEX = 11

    def PANEL_WIDTH_PX = 320

    /** CSS-класс div-контейнера правой панели. */
    val panel = style(
      width( PANEL_WIDTH_PX.px ),
    )

    val content = {
      val paddingTopPx = args.screenInfo.unsafeOffsets.top
      style(
        paddingTop( paddingTopPx.px ),
        maxHeight( (args.screenInfo.screen.height - paddingTopPx).px )
      )
    }

    /** CSS-класс заголовка внутри панели поиска. */
    //val panelHeader = _styleAddClasses( _PANEL + "_header" )


    /** Табы на поисковой панели. */
    object Tabs {

      /** Стили содержимого вкладки с гео-картой. */
      object MapTab {

        private val TAB_BODY_HEIGHT_PX = args.screenInfo.screen.height - args.screenInfo.unsafeOffsets.top

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
          val OUTER = _SM_ + "categories"
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

    private val _screenHeightPx = (args.screenInfo.screen.height - args.screenInfo.unsafeOffsets.top).px
    private val _screenHeight = height( _screenHeightPx )

    /** Крутилка внизу экрана. */
    val loader = style(
      left( (args.screenInfo.screen.width / 2).px ),
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
        maxHeight( (args.screenInfo.screen.height - paddingTopPx).px ),
        overflow.auto
      )
    }

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
