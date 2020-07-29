package io.suggest.sc.v.styl

import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.css.Css.Lk.{_SM_PREFIX_ => _SM_}
import io.suggest.css.ScalaCssDefaults._
import io.suggest.dev.MPlatformS
import io.suggest.font.MFonts
import io.suggest.math.SimpleArithmetics._
import io.suggest.sc.ScConstants
import scalacss.internal.DslBase.ToStyle
import japgolly.univeq._

import scala.language.postfixOps

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.06.2020 16:11
  * Description: Статические стили ScCss, которые не меняются в ходе работы выдачи.
  */
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

  val thinText = style(
    fontFamily.attr := MFonts.OpenSansLight.fileName,
  )

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
        zIndex(-1)
      )
    }

  }


  /** Стили для заголовка. */
  object Header {

    val border = style(
      borderBottom( 1.px, solid ),
    )

    val header = style(
      addClassNames(
        //ScCssStatic.Header.HEADER,
        Css.Position.ABSOLUTE
      ),
      // Для экранов с вырезами (iphone10) - расширяем заголовок вниз по вертикали:
      height( ScCss.HEADER_HEIGHT_PX.px ),
      // TODO На гориз.смартфоне криво, на декстопе - норм.
      //left( args.screenInfo.unsafeOffsets.left.px ),
      // При выезде левой панели, заголовок ужимается в несколько строчек. Нельзя так.
      minWidth( 200.px ),
      width( 100.%% ),
      zIndex( 10 ),
      textAlign.center,
      // Этот transform скопирован из showcase.styl. Не ясно, нужен ли он.
      transform := "translate3d(0, 0, 0)",
    )

    val progress = style(
      position.absolute,
      bottom(0.px),
      width( 100.%% ),
      height(1.px),
    )

    /** Корневое имя класса, от которого идёт остальное словообразование. */
    def HEADER = _SM_ + "producer-header"

    object Buttons {

      // Старые кнопки с plain-вёрсткой
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

    val snackMsg = style(
      width( 100.%% )
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

    /** Дополнение к cancel-стилю, чтобы к правому верхнему углу прижать. */
    val cancelTopRight = style(
      right( -10.px ),
    )

  }


  object Welcome {

    val _SM_WELCOME_AD = _SM_ + "welcome-ad"

    val welcome = style(
      addClassNames( _SM_WELCOME_AD ),
    )

  }


  object Search {

    def PANEL_WIDTH_PX = 320

    /** CSS-класс div-контейнера правой панели. */
    val panel = style(
      width( PANEL_WIDTH_PX.px ),
    )

    /** Поля текстового поиска и контейнер оной. */
    object TextBar {

      val _SM_SEARCH_BAR = _SM_ + "search-bar"

      val barHeight = style(
        height( ScCss.SEARCH_BAR_HEIGHT_PX.px )
      )

      // TODO Статический стиль - унести в статику.
      val bar = {
        var acc = List[ToStyle](
          padding( 2.px, 0.px, 0.px, 22.px ),
          addClassName( barHeight.htmlClass ),
          display.inlineFlex,
          width( 100.%% ),
        )

        val topOffPx = ScCss.SEARCH_TOP_OFFSET_PX
        if (topOffPx !=* 0)
          // Равняем полосу input'а с полосой заголовка.
          acc ::= marginTop( topOffPx.px )

        style( acc: _* )
      }

      val inputFormControl = style(
        flexDirection.initial,
        width(100.%%)
      )

      val inputsH = style(
        height( 48.px ),
      )

      val input100w = style(
        flexGrow( 100 )
      )

    }


    object Geo {

      /** Контейнер прицела центра карты. */
      val crosshair = style(
        width( 0.px ),
        position.relative,
        left(48.5 %%),
        zIndex(1000),
        userSelect.none,
      )

      /** Стиль контейнера карты. Контейнер порождается js'кой гео-карты, а не нами. */
      val geomap = {
        val pc100 = 100.%%
        style(
          userSelect.none,
          width( pc100 ),
          height( pc100 ),
        )
      }

    }


    /** Стили для списка найденных узлов (тегов и т.д.). */
    object NodesFound {

      val nothingFound = style(
        addClassName( TextBar._SM_SEARCH_BAR ),
      )

      /** CSS grid item для ADN-узлов (не тегов). */
      val gridRowAdn = style(
        width( 100.%% )
      )

      val nodesList = {
        val zeroPx = 0.px
        style(
          paddingTop( zeroPx ),
          paddingBottom( zeroPx ),
          overflow.hidden,
          // Закомменчено, было нужно для Mui popover, но и это надо будет спилить вместе с Popover'ом.
          //maxWidth( Search.PANEL_WIDTH_PX.px ),
        )
      }

      /** Горизонтальный прогресс-бар запроса. */
      val progress = {
        style(
          height( 5.px ),
          position.absolute,
          bottom( 1.px ),
          width( 100.%% ),
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
      val nodeRow = {
        val px16 = 16.px
        style(
          paddingLeft(px16).important,
          paddingRight(px16).important,
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
        minWidth.initial,
      )

      /** Слишком большой шрифт у тегов, уменьшить. */
      val tagRowTextPrimary = style(
        fontSize( 0.9.rem ),
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

      val rightIcon = style(
        position.absolute,
        right( 4.px ),
      )

      /** Стили строк меню. */
      val rowContent = style(
        addClassName( thinText.htmlClass ),
        position.relative,
        minWidth( 260.px ),
        margin(0.px, auto),
        fontSize( 0.8.rem ),
        padding(12.px, 0.px),
        textTransform.uppercase
      )

    }

    val version = style(
      position.absolute,
      //bottom( 0.px ),
      right( 0.px ),
      fontSize.xxSmall,
    )

  }


  object AppDl {

    val osFamily = style(
      padding(4.px),
      width( 200.px ),
    )

    val hardWordWrap = style(
      wordWrap.attr := "anywhere",
    )

    val dlLink = style(
      marginTop( 10.px ),
    )

    val dlLinkOrBtn = style(
      float.right,
    )

  }


  /** Компоненты. */
  object Settings {

    val kvLine = style(
      alignItems.center,
      display.flex,
    )

    val kvLineKey = style(
      flexGrow( 100 ),
    )

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

    Search.panel,
    Search.NodesFound.listDiv,
    Search.TextBar.bar,
    Search.Geo.crosshair,

    Menu.Rows.rowContent,
    Menu.version,

    AppDl.osFamily,
  )

}

