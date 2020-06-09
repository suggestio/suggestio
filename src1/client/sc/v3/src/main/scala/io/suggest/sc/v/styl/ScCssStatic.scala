package io.suggest.sc.v.styl

import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.css.Css.Lk.{_SM_PREFIX_ => _SM_}
import io.suggest.css.ScalaCssDefaults._
import io.suggest.font.MFonts
import io.suggest.math.SimpleArithmetics._
import io.suggest.sc.ScConstants

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

      /* Оставлено - вдруг понадобится.
      val rowText = style(
        height( 28.px )
      )
      */

      val rightIcon = style(
        position.absolute,
        right( 4.px ),
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
    Menu.version,
    AppDl.osFamily,
  )

}
