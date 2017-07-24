package io.suggest.sc.styl

import ScScalaCssDefaults._
import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.css.Css
import io.suggest.dev.MScreen
import io.suggest.i18n.MsgCodes
import io.suggest.model.n2.node.meta.colors.{MColorData, MColors}
import io.suggest.sc.ScConstants

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
    fg = Some(MColorData( "ffffff" ))
  )

}


/** Интерфейс модели параметров для вызова ScCss. */
trait IScCssArgs {
  def customColorsOpt   : Option[MColors]
  def screen            : MScreen
  def wcBgWh            : Option[MSize2di]
  def wcFgWh            : Option[MSize2di]
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
  private def _NAME_TOKENS_DELIM = "-"
  private def _SM_ = Css.Lk._SM_PREFIX_

  private def _styleAddClasses(cssClasses: String*) = style( addClassNames(cssClasses: _*) )

  private def `BUTTON` = "button"
  private def _SM_BUTTON = _SM_ + `BUTTON`

  private def _colorCss( colorOpt: Option[MColorData], dflt: => String ) = Color( colorOpt.fold(dflt)(_.hexCode) )
  private val _bgColorCss = _colorCss( colors.bg, ScConstants.Defaults.BG_COLOR )
  private val _fgColorCss = _colorCss( colors.fg, ScConstants.Defaults.FG_COLOR )

  //val button = _styleAddClasses( _SM_BUTTON )

  val clear = _styleAddClasses( Css.CLEAR )

  val smFlex = _styleAddClasses( _SM_ + "flex" )
  val smOverflowScrolling = _styleAddClasses( _SM_ + "overflow-scrolling" )

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
      lazy val ru = _styleAddClasses( __ + "ru" )
      lazy val en = _styleAddClasses( __ + "en" )
    }

    /** Исторически как-то сложилось, что активация body происходит через style-аттрибут. Но это плевать наверое. */
    val smBodyReady = style(
      backgroundColor.white
    )

  }


  /** Стили для #sioMartRoot и каких-то вложенных контейнеров */
  object Root {

    /** корневой div-контейнер. */
    val root = _styleAddClasses( _SM_ + "showcase" )

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

    object Bg {

      /** Стили фонового изображения экрана приветствия */
      val bgImg = {
        // В зависимости от наличия или отсутствия размера welcome background, стили могут отличаться.
        val whMx = args.wcBgWh.fold( StyleS.empty ) { wh0 =>
          val wh2 = if (wh0.whRatio < args.screen.whRatio) {
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

    object Fg {

      /** Стили логотипа экрана приветствия. */
      val fgImg = {
        // Подготнка логотипа приветствия под текущий экран: центровка.
        val whMx = args.wcFgWh.fold( StyleS.empty ) { wh0 =>
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

      val fgText = _styleAddClasses( _SM_WELCOME_AD + "_fg-text" )

      val helper = _styleAddClasses( _SM_WELCOME_AD + "_helper" )
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
      borderColor( _fgColorCss )
    )

    object Buttons {

      /** Начальный список для сборки стилей разных кнопок на панели заголовка. */
      private val _allBtnStyles: List[String] = {
        _SM_BUTTON ::
          (HEADER + "_btn") ::
          Css.Position.ABSOLUTE ::
          Nil
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
      val right = _styleAddClasses(
        Align.RIGHT :: _allBtnStyles: _*
      )

      /** Стиль кнопки заголовка, которая указывает влево. */
      val left = _styleAddClasses(
        Align.LEFT :: _allBtnStyles: _*
      )

    }


    /** Доступ к стилям логотипа узла. */
    object Logo {

      /** Суффикс названия css-классов разных логотипов. */
      private def `-logo` = "-logo"

      /** Стили текстового логотипа узла.*/
      object Txt {

        private val TXT_LOGO = HEADER + "_txt" + `-logo`

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

          val left = _styleAddClasses( __ + MsgCodes.`left` )
          val right = _styleAddClasses( __ + MsgCodes.`right` )
          def allSides = left :: right :: Nil
        }

      }


      /** CSS для картинки-логотипа. */
      object Img {
        /** Алиас основного стиля логотипа. */
        val logo = _styleAddClasses( HEADER + `-logo` )
        def IMG_HEIGHT_CSSPX = 30
      }

    }

  }


  /** Панель поиска. */
  object Search {

    private val _PANEL = _SM_ + "categories-screen"

    /** CSS-класс div-контейнера правой панели. */
    val panel = _styleAddClasses( _PANEL )

    /** CSS-класс заголовка внутри панели поиска. */
    //val panelHeader = _styleAddClasses( _PANEL + "_header" )


    /** Поля текстового поиска и контейнер оной. */
    object SearchBar {

      private val _BAR = _SM_ + "search-bar"

      val bar       = _styleAddClasses( _BAR )

      /** Сообщение, например "ничего не найдено". */
      val message   = _styleAddClasses( _BAR + "_message" )

      /** CSS для текстовых полей поиска. */
      object Field {
        val field         = _styleAddClasses( _BAR + "_field" )
        val active        = _styleAddClasses( __ + "active" )
        val fieldWrapper  = _styleAddClasses( _BAR + "_wrapper" )
        val input         = _styleAddClasses( _BAR + "_input" )
      }

    }


    /** Табы на поисковой панели. */
    object Tabs {

      private val _TABS = _PANEL + "_tabs"

      /** div-контейнер заголовков табов. */
      val tabs = _styleAddClasses( _TABS )

      val tabsWrapper = _styleAddClasses( _TABS + "-wrapper" )

      /** Стили для одного таба. */
      object Single {

        val tabOuter = _styleAddClasses( _PANEL + "_single-tab" )
        val tabInner = _styleAddClasses( _SM_ + "tab" )
        val inactive = _styleAddClasses( __ + "inactive" )

        object Rounded {
          private val _ROUNDED_ = __ + "rounded-"
          val left  = _styleAddClasses( _ROUNDED_ + "left" )
          val right = _styleAddClasses( _ROUNDED_ + "right" )
        }

      }

      /** Стили содержимого вкладки с гео-картой. */
      object MapTab {
        private val OUTER = _SM_ + "categories"
        val outer = _styleAddClasses( OUTER )
        val inner = _styleAddClasses( OUTER + "_content" )
      }

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
    Body.BgLogo.ru,
    Welcome.Bg.bgImg,
    Welcome.Fg.fgImg,

    Header.Buttons.search,
    Header.Logo.Txt.Dots.dot,

    Search.SearchBar.Field.active,
    Search.Tabs.Single.Rounded.right,
    Search.Tabs.MapTab.inner
  )

}
