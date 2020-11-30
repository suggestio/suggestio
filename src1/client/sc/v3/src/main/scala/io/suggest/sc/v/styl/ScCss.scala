package io.suggest.sc.v.styl

import com.materialui.Mui
import io.suggest.color.{MColorData, MColors}
import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.css.Css
import io.suggest.css.ScalaCssDefaults._
import io.suggest.i18n.MsgCodes
import io.suggest.math.SimpleArithmetics._
import io.suggest.sc.ScConstants
import io.suggest.sc.m.styl.MScCssArgs
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import scalacss.internal.DslBase.ToStyle

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
  def SEARCH_TOP_OFFSET_PX = 0

  def SEARCH_BAR_HEIGHT_PX = 65

  /**
    * Оффсет при наличии заголовка табов поиска.
    * Если нет заголовка табов, то можно делать бОльший offset.
    * if (tabsHdr.isEmpty) 115 else 165
    */
  val TABS_OFFSET_PX = SEARCH_BAR_HEIGHT_PX + SEARCH_TOP_OFFSET_PX

  /** Высота заголовка. */
  def HEADER_HEIGHT_PX = 50

  @inline implicit def univEq: UnivEq[ScCss] = UnivEq.derive

  // z-index-костыли для расположения нескольких панелей и остального контента.
  // Жило оно одиноко в scCss, но пока унесено сюда после рефакторинга в DI/ScCss.
  def sideBarZIndex = 11

  // Значение "initial" для css-свойст.
  def css_initial = scalacss.internal.Literal.Typed.initial.value

  def args = GenLens[ScCss]( _.args )

}


/** Стили для выдачи.
  *
  * @param args Цвета и экран для оформления выдачи.
  */
final case class ScCss( args: MScCssArgs ) extends StyleSheet.Inline {

  import dsl._

  def _styleAddClass(cssClass: String) = style( addClassName(cssClass) )

  val (bgColorCss, fgColorCss) = {
    val colors = args.customColorsOpt getOrElse ScCss.COLORS_DFLT
    def _colorCss( colorOpt: Option[MColorData], dflt: => String ) =
      Color( (colorOpt getOrElse MColorData(dflt)).hexCode )

    val bg = _colorCss( colors.bg, ScConstants.Defaults.BG_COLOR )
    val fg = _colorCss( colors.fg, ScConstants.Defaults.FG_COLOR )
    (bg, fg)
  }

  /** Стиль цвета фона для произвольного элемента выдачи. */
  val bgColor = style(
    backgroundColor( bgColorCss ),
  )


  /** bgIsLight - Является ли фон светлым?
    * panelBg - стиль фона панелей.
    * panelBgHex - hex-код цвета фона панелей.
    */
  val (bgIsLight, panelBg, panelBgHex) = {
    // mubBgColor небезопасен, изменяется при darken/lighten() и др.функциях.
    val muiBgColor = Mui.Styles.decomposeColor( bgColorCss.value )
    val bgColorLuma = Mui.Styles.getLuminance( muiBgColor )

    val lightDarkLimit = 0.5
    val _bgIsLight = bgColorLuma > lightDarkLimit

    val modCoeff = 0.13

    // Засветлять или затемнять цвет? НЕ засветлять белый, и не затемнять чёрный.
    val isDoLighten = _bgIsLight match {
      case true  => bgColorLuma <= lightDarkLimit + modCoeff // 0.65
      case false => bgColorLuma < modCoeff // 0.15
    }

    val panelBgColorCss =
      if (isDoLighten) Mui.Styles.lighten( muiBgColor, modCoeff )
      else Mui.Styles.darken( muiBgColor, modCoeff )

    val panelBgStyl = style(
      backgroundColor( Color(panelBgColorCss) ),
    )
    val panelBgColorHex = Mui.Styles.rgbToHex( panelBgColorCss )

    (_bgIsLight, panelBgStyl, panelBgColorHex)
  }


  val fgColor = style(
    color( fgColorCss ),
  )

  val fgColorBg = style(
    backgroundColor( fgColorCss ),
  )

  private val _fgColorCss = borderColor( fgColorCss )
  val fgColorBorder = style(
    _fgColorCss,
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

        /** Явное имя класса, чтобы оно не дёргалось в зависимость от наличии/отсутствия wh */
        style("wBgI")(
          addClassName( _SM_WELCOME_AD + "_bg-img" ),
          whMx
        )
      }

    }


    /** Контейнер стилей элементов переднего плана экрана приветствия. */
    object Fg {

      /** Данные по картинки. */
      val fgImgWhOpt = for (wh0 <- args.wcFgWh) yield {
        if (args.wcFgVector) {
          // SVG-картинку растягивать по ширине принудительно.
          val widthPx2 = 280
          wh0.copy(
            width  = widthPx2,
            height = (wh0.height.toDouble * widthPx2.toDouble / wh0.width.toDouble).toInt,
          )
        } else {
          wh0 / 2
        }
      }

      /** Стили логотипа экрана приветствия. */
      val fgImg = {
        // Подгонка логотипа приветствия под текущий экран: центровка.
        val whMx = fgImgWhOpt.fold(StyleS.empty) { wh2 =>
          // Если векторная графика, то надо растянуть её до приемлемых размеров.
          val margin2 = MSize2di(
            height = wh2.height / -2 + 25,
            width  = wh2.width / -2,
          )
          _imgWhMixin( wh2, margin2 )
        }

        /** Явное имя класса, чтобы оно не дёргалось в зависимость от наличии/отсутствия wh */
        style("wFgI")(
          addClassName( _SM_WELCOME_AD + "_fg-img" ),
          whMx
        )
      }

      val fgText = {
        var acc: List[ToStyle] =
          addClassName( _SM_WELCOME_AD + "_fg-text" ) ::
          Nil

        // Отработать центровку по вертикали с учётом возможного растягивания векторного fgImg.
        for (fgImgWh <- fgImgWhOpt)
          acc ::= marginTop( fgImgWh.height.px )

        style( acc: _* )
      }

      val helper = _styleAddClass( _SM_WELCOME_AD + "_helper" )

    }

  }


  /** Стили для заголовка. */
  object Header {

    /** Подкрашивать заголовок? Не надо, когда нет цветов узла.
      * Само подкрашивание идёт на уровне шаблона HeaderR, т.к. динамические имена классов требуют лишних телодвижений.
      */
    def isColored: Boolean =
      args.customColorsOpt.nonEmpty

    /** Стили контейнера любого заголовка. */
    val header = style(
      paddingTop( args.screenInfo.unsafeOffsets.top.px )
    )

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

        /** Точки по краям названия узла. */
        object Dots {
          private val DOT = TXT_LOGO + "-dot"

          /** Стиль для одной точки. */
          val dot = style(
            addClassName( DOT ),
          )

          val left = _styleAddClass( Css.__ + MsgCodes.`left` )
          val right = _styleAddClass( Css.__ + MsgCodes.`right` )
        }

      }

    }

  }


  /** Панель поиска. */
  object Search {

    val content = {
      val paddingTopPx = args.screenInfo.unsafeOffsets.top
      style(
        paddingTop( paddingTopPx.px ),
        maxHeight( (args.screenInfo.screen.wh.height - paddingTopPx).px )
      )
    }


    object TextBar {
      val underline = style(
        addClassName( fgColorBorder.htmlClass ),
        &.before(
          _fgColorCss,
        ),
      )
    }


    object NodesFound {

      val nodeRow = style(
        &.hover(
          addClassName( fgColor.htmlClass ),
        )
      )

    }


    /** Стили содержимого вкладки с гео-картой. */
    object Geo {

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

      /** Контейнер прицела центра карты. */
      val crosshair = style(
        top( -(TAB_BODY_HEIGHT_PX / 2 + 12).px ),
      )

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
        paddingLeft( Math.max(minPaddingTopPx, uo.left).px ),
        maxHeight( (args.screenInfo.screen.wh.height - paddingTopPx).px ),
        overflow.auto
      )
    }

    val version = style(
      bottom( args.screenInfo.unsafeOffsets.bottom.px ),
    )

  }


  object Dialogs {

    /** На айфоне снизу экрана служебная область с вырезом-линией. Не надо на неё распространять диалог или иные элементы. */
    val unsafeBottom = {
      var acc = List.empty[ToStyle]
      for (bottomPx <- args.screenInfo.unsafeOffsets.bottomO)
        acc ::= paddingBottom( bottomPx.px )
      style( acc: _* )
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

    Search.Geo.inner,
    Search.NodesFound.nodeRow,
    Search.TextBar.underline,

    Grid.container,
    Menu.panel,
    Dialogs.unsafeBottom,
  )

}
