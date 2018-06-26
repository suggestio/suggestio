package io.suggest.sc.styl

import io.suggest.color.MColorData
import io.suggest.css.ScalaCssDefaults._
import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.css.Css
import io.suggest.dev.MScreen
import io.suggest.font.MFonts
import io.suggest.i18n.MsgCodes
import io.suggest.model.n2.node.meta.colors.MColors
import io.suggest.sc.ScConstants
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 15:01
  * Description: scalaCSS для выдачи s.io.
  */

object ScCss {

  // TODO Исходные цвета надо бы брать откуда-то, с сервера например.
  def COLORS_DFLT = MColors(
    bg = Some(MColorData( "111111" )),
    fg = Some(MColorData.Examples.WHITE)
  )

  lazy val isSafari = cssEnv.platform.name.contains("Safari")

  /**
    * Некоторые браузеры скроллят внешний контейнер вместо внутреннего, когда оба скроллабельны.
    * С помощью это флага можно активировать логику перезаписи scrollbar'а внутренним контейнером,
    * чтобы быстро исправить недоразумение.
    * @return true, когда требуется спровоцировать внутренний скроллбар через шаманство с высотами.
    *         false, когда костыли не нужны (по умолчанию).
    */
  lazy val needOverrideScroll: Boolean = {
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

  /**
    * Оффсет при наличии заголовка табов поиска.
    * Если нет заголовка табов, то можно делать бОльший offset.
    * if (tabsHdr.isEmpty) 115 else 165
    */
  val TABS_OFFSET_PX = 115

  /** Высота заголовка. */
  val HEADER_HEIGHT_PX = 50

  implicit def univEq: UnivEq[ScCss] = UnivEq.derive

}


/** Интерфейс модели параметров для вызова ScCss. */
trait IScCssArgs {
  def customColorsOpt   : Option[MColors]
  def screen            : MScreen
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

  val colors = args.customColorsOpt.getOrElse( ScCss.COLORS_DFLT )

  /** Строковые обозначения стилей наподобии i.s.css.Css. */
  private def __ = Css.__
  private def _SM_ = Css.Lk._SM_PREFIX_

  val overflowScrollingMx: StyleS = {
    if (ScCss.needOverrideScroll) {
      mixin(
        addClassName( _SM_ + "overflow-scrolling"  )
      )
    } else {
      mixin(
        overflowY.auto
      )
    }
  }

  private def _styleAddClasses(cssClasses: String*) = style( addClassNames(cssClasses: _*) )
  private def _styleAddClass(cssClass: String) = style( addClassName(cssClass) )

  private def `BUTTON` = "button"
  private def _SM_BUTTON = _SM_ + `BUTTON`

  private def _colorCss( colorOpt: Option[MColorData], dflt: => String ) = Color( colorOpt.fold(dflt)(_.hexCode) )
  private val _bgColorCss = _colorCss( colors.bg, ScConstants.Defaults.BG_COLOR )
  private val _fgColorCss = _colorCss( colors.fg, ScConstants.Defaults.FG_COLOR )

  //val button = _styleAddClasses( _SM_BUTTON )

  val clear = _styleAddClass( Css.CLEAR )

  val smFlex = _styleAddClass( _SM_ + "flex" )


  /** Стили для html.body . */
  // TODO Этот код наверное не нужен. Т.к. оно вне react-компонента.
  object Body {

    /** Главный обязательный статический класс для body. */
    val smBody = style(
      //addClassName( _SM_ + "body" ),
      overflow.hidden
    )

    /** Фоновый SVG-логотип ЯПРЕДЛАГАЮ. Его в теории может и не быть, поэтому оно отдельно от класса body. */
    object BgLogo {
      lazy val ru = _styleAddClass( __ + "ru" )
      lazy val en = _styleAddClass( __ + "en" )
    }

    /** Исторически как-то сложилось, что активация body происходит через style-аттрибут. Но это плевать наверое. */
    val smBodyReady = style(
      backgroundColor.white
    )

  }


  /** Стили для #sioMartRoot и каких-то вложенных контейнеров */
  object Root {

    /** корневой div-контейнер. */
    val root = _styleAddClass( _SM_ + "showcase" )

    /** Общий стиль для всех панелей. */
    val panelCommon = mixin(
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
        background := _bgColorCss,
        // 90% - эффекта затемнения сливается с незатемённой областью. 80% - эффект уже виден.
        filter := "brightness(80%)",
        zIndex(-1)
      )
    }

  }


  object Welcome {

    private val _SM_WELCOME_AD = _SM_ + "welcome-ad"

    val welcome = style(
      addClassNames( _SM_WELCOME_AD ),
      backgroundColor( _bgColorCss )
    )

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
          val wh2 = if (ISize2di.whRatio(wh0) < ISize2di.whRatio(args.screen)) {
            val w = args.screen.width
            MSize2di(
              width  = w,
              height = w * wh0.height / wh0.width
            )
          } else {
            val h = args.screen.height
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

      val fgText = _styleAddClass( _SM_WELCOME_AD + "_fg-text" )

      val helper = _styleAddClass( _SM_WELCOME_AD + "_helper" )
    }

  }


  /** Стили для заголовка. */
  object Header {

    /** Корневое имя класса, от которого идёт остальное словообразование. */
    private def HEADER = _SM_ + "producer-header"

    /** Стили контейнера любого заголовка. */
    val header = style(
      addClassNames( HEADER, Css.Position.ABSOLUTE ),
      backgroundColor( _bgColorCss ),
      borderColor( _fgColorCss ),
      height( ScCss.HEADER_HEIGHT_PX.px )
    )

    object Buttons {

      private val _allBtnStylesNoAbs: List[String] = {
        _SM_BUTTON ::
          (HEADER + "_btn") ::
          Nil
      }
      /** Начальный список для сборки стилей разных кнопок на панели заголовка. */
      private val _allBtnStyles: List[String] = {
        Css.Position.ABSOLUTE :: _allBtnStylesNoAbs
      }


      /** Выравнивание кнопок. */
      object Align {
        private def __aligned(where: String) = __ + where + "-aligned"
        def LEFT  = __aligned( MsgCodes.`left` )
        def RIGHT = __aligned( MsgCodes.`right` )
      }

      //val leftGeoBtn = _styleAddClasses( _btnMx, HEADER + "_geo-button", Align.LEFT )

      private def _btnClass(root: String) = HEADER + "_" + root + "-" + `BUTTON`

      /** Стиль кнопки поиска. */
      val search = _styleAddClasses(
        _btnClass("search") :: Align.RIGHT :: _allBtnStyles: _*
      )

      /** Стиль кнопки меню слева. */
      val menu = _styleAddClasses(
        _btnClass("geo") :: Align.LEFT :: _allBtnStyles: _*
      )

      /** Стиль кнопки заголовка, который указывает вправо. */
      val rightCss = style(
        addClassNames(_allBtnStyles: _*),
        //left(auto),
        top( 14.px ),
        right( -2.px ),
        left.auto
      )

      /** Стиль кнопки заголовка, которая указывает влево. */
      val leftCss = _styleAddClasses(
        Align.LEFT :: _allBtnStylesNoAbs: _*
      )

    }


    /** Доступ к стилям логотипа узла. */
    object Logo {

      /** Суффикс названия css-классов разных логотипов. */
      private def `logo_` = "logo"

      /** Стили текстового логотипа узла.*/
      object Txt {

        private val TXT_LOGO = HEADER + "_txt-" + `logo_`

        val txtLogo = {
          style(
            addClassName( TXT_LOGO ),
            color( _fgColorCss ),
            borderColor( _fgColorCss )
          )
        }

        /** Точки по краям названия узла. */
        object Dots {
          private val DOT = TXT_LOGO + "-dot"

          /** Стиль для одной точки. */
          val dot = style(
            addClassName( DOT ),
            backgroundColor( _fgColorCss )
          )

          val left = _styleAddClass( __ + MsgCodes.`left` )
          val right = _styleAddClass( __ + MsgCodes.`right` )
          def allSides = left :: right :: Nil
        }

      }


      /** CSS для картинки-логотипа. */
      object Img {
        /** Алиас основного стиля логотипа. */
        val logo = _styleAddClass( HEADER + "_" + `logo_` )
        def IMG_HEIGHT_CSSPX = 30
      }

    }

  }


  /** Панель поиска. */
  object Search {

    private val _PANEL = _SM_ + "categories-screen"

    def Z_INDEX = 11

    def PANEL_WIDTH_PX = 320

    /** CSS-класс div-контейнера правой панели. */
    val panel = style(
      width( PANEL_WIDTH_PX.px ),
      Root.panelCommon
    )

    /** CSS-класс заголовка внутри панели поиска. */
    //val panelHeader = _styleAddClasses( _PANEL + "_header" )


    /** Поля текстового поиска и контейнер оной. */
    object SearchBar {

      private val _BAR = _SM_ + "search-bar"

      val bar       = _styleAddClass( _BAR )

      /** Сообщение, например "ничего не найдено". */
      val message   = _styleAddClass( _BAR + "_message" )

      /** CSS для текстовых полей поиска. */
      object Field {
        val field         = style(
          addClassName( _BAR + "_field" ),
          width( 245.px ),
          left( -7.px )
        )
        val active        = _styleAddClass( __ + "active" )
        val fieldWrapper  = _styleAddClass( _BAR + "_wrapper" )
        val input         = style(
          addClassName( _BAR + "_input" ),
          color( _fgColorCss )
        )
      }

    }


    /** Табы на поисковой панели. */
    object Tabs {

      private val _TABS = _PANEL + "_tabs"

      /** div-контейнер заголовков табов. */
      val tabs = _styleAddClass( _TABS )

      val tabsWrapper = _styleAddClass( _TABS + "-wrapper" )

      private val TAB_BODY_HEIGHT_PX = args.screen.height - ScCss.TABS_OFFSET_PX

      private val TAB_BODY_HEIGHT    = height( TAB_BODY_HEIGHT_PX.px )

      /** Форсировать скроллбар во внутреннем контейнере, если этого требует окружение. */
      private val TAB_BODY_CONTENT_HEIGHT = if (ScCss.needOverrideScroll) {
        height( (TAB_BODY_HEIGHT_PX + 1).px )
      } else {
        TAB_BODY_HEIGHT
      }


      /** Стили для одного таба. */
      object Single {

        val tabOuter = _styleAddClass( _PANEL + "_single-tab" )
        val tabInner = style(
          background := _fgColorCss,
          color(_bgColorCss),
          addClassName( _SM_ + "tab" )
        )
        val inactive = style(
          color(_fgColorCss),
          (background := none).important,
          addClassName( __ + "inactive" )
        )

        object Rounded {
          private val _ROUNDED_ = __ + "rounded-"
          val left  = _styleAddClass( _ROUNDED_ + "left" )
          val right = _styleAddClass( _ROUNDED_ + "right" )
        }

      }

      /** Стили содержимого вкладки с гео-картой. */
      object MapTab {

        private val OUTER = _SM_ + "categories"

        /** Стиль внешнего контейнера. */
        val outer = style(
          addClassName( OUTER ),
          smFlex,
          TAB_BODY_HEIGHT
        )

        /** Стиль wrap-контейнера. */
        val wrapper = style(
          overflowScrollingMx,
          smFlex,
          TAB_BODY_HEIGHT
        )

        val inner = style(
          addClassName( OUTER + "_content" ),
          TAB_BODY_CONTENT_HEIGHT
        )

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


      /** Стили для вкладки с тегами. */
      object TagsTag {

        private val TAGS = "shops"

        private val OUTER = _SM_ + TAGS

        val outer = style(
          addClassName(OUTER),
          smFlex,
          TAB_BODY_HEIGHT
        )

        val wrapper = style(
          overflowScrollingMx,
          smFlex,
          TAB_BODY_HEIGHT
        )

        val inner = style(
          addClassName( OUTER ),
          minHeight( TAB_BODY_HEIGHT_PX.px )
        )

        private val TAGS_LIST = TAGS + "-list"

        /** Список тегов. */
        val tagsList = _styleAddClass( TAGS_LIST )

        /** Один тег в списке тегов. */
        val tagRow = _styleAddClass( TAGS_LIST + "_row" )

        val oddRow  = _styleAddClass( __ + "odd" )
        val evenRow = _styleAddClass( __ + "even")

        val selected = _styleAddClass( __ + "selected" )

      }

    }

  }


  /** Стили для плитки карточек, точнее для контейнеров этой плитки. */
  object Grid {

    private val _SM_GRID_ADS = _SM_ + "grid-ads"

    private val _screenHeightPx = args.screen.height.px
    private val _screenHeight = height( _screenHeightPx )

    val outer = style(
      addClassName( _SM_GRID_ADS ),
      _screenHeight,
      backgroundColor( _bgColorCss )
    )

    val wrapper = style(
      overflowScrollingMx,
      _screenHeight,
      overflowX.hidden
    )

    val content = style(
      addClassName( _SM_GRID_ADS + "_content" )
    )

    val container = style(
      addClassName( _SM_GRID_ADS + "_container" ),
      minHeight( _screenHeightPx ),
      left(0.px),
      opacity(1),
      // 2017-12-14: Чтобы блоки могли за экран выезжать: нужно для широких карточек.
      overflow.initial
    )


    /** Стили для анимации подгрузки. */
    object Loaders {

      private val _SM_GRID_ADS_LOADER = _SM_GRID_ADS + "_loader"

      /* Это svg-изображение, с помощью которого рисовалась zip-линия отрыва внизу для ещё неподгруженной плитки.
      // TODO Это должно быть значением background-image для корневого div'а.
      private val enableBackgroundAttr = VdomAttr("enable-background")
      private def _renderBottomZipSvg(fillColor: String) = {
        val zero = 0
        val w = 10
        val h = 5
        val viewBox = zero + SPACE + zero + SPACE + w + SPACE + h
        <.svg(
          ^.x := 0.px,
          ^.y := 0.px,
          ^.width := w.px,
          ^.height := h.px,
          ^.viewBox := viewBox,
          // TODO Тут исторический костыль, и не ясно, нужен он или нет. Вроде бы этот аттрибут сдох (depreacted) и не особо реализован в браузерах.
          enableBackgroundAttr := ("new" + SPACE + viewBox),
          ^.xmlSpace := "preserve",
          <.polygon(
            ^.fill := Color(fillColor),
            ^.points := "10,3.16 5,0 0,3.16 0,5 5,1.84 10,5"
          )
        )
      }
      */

      /** Внешний контейнер спиннера. */
      val outer = style(
        addClassName( _SM_GRID_ADS_LOADER ),
        // TODO Надо собрать содержимое svg вручную через Vdom и загонять в строку.
        backgroundImage := ("""url('data:image/svg+xml;utf8,<svg version="1.1" id="Layer_1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px" width="10px" height="5px" viewBox="0 0 10 5" enable-background="new 0 0 10 5" xml:space="preserve"><polygon fill="#""" + _fgColorCss.value + """" points="10,3.16 5,0 0,3.16 0,5 5,1.84 10,5 "/></svg>')"""),
        backgroundRepeat := "repeat-x"
        // Ещё есть width(max grid outer width), но оно в шаблоне живёт.
      )

      val _SM_GRID_ADS_LOADER_SPINNER = _SM_GRID_ADS_LOADER + "-spinner"

      /** Внешний контейнер для спиннера. */
      val spinnerOuter = _styleAddClass( _SM_GRID_ADS_LOADER_SPINNER )

      /** Наконец, контейнер для анимированной SVG'шки. */
      val spinnerInner = _styleAddClass( _SM_GRID_ADS_LOADER_SPINNER + "-inner" )

    }

  }


  /** Стили для панели меню. */
  object Menu {

    val panel = style(
      Root.panelCommon,
      width( 280.px )
    )

    /** Стили строк меню */
    object Rows {

      val rowsContainer = style(
        position.relative
      )

      val rowLink = style(
        textDecoration := none
      )

      val rowOuter = style(
        cursor.pointer
      )

      val rowContent = style(
        color( _fgColorCss ),
        position.relative,
        width( 260.px ),
        margin(0.px, auto),
        fontFamily.attr := MFonts.OpenSansLight.fileName,
        fontSize( 14.px ),
        padding(12.px, 0.px),
        textTransform.uppercase
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
    Root.root,
    Body.BgLogo.ru,
    Welcome.Bg.bgImg,
    Welcome.Fg.fgImg,

    Header.Buttons.search,
    Header.Logo.Txt.Dots.dot,
    Header.Logo.Img.logo,

    Search.SearchBar.Field.active,
    Search.Tabs.Single.Rounded.right,
    Search.Tabs.MapTab.inner,
    Search.Tabs.TagsTag.inner,

    Grid.container,
    Grid.Loaders.spinnerInner,
    Menu.Rows.rowContent
  )

}
