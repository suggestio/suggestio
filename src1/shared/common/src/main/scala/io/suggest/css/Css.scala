package io.suggest.css

import io.suggest.common.html.HtmlConstants._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.16 17:59
  * Description:
  */
object Css {

  /** Для рендера перечисления css-классов можно юзать этот метод. */
  final def flat(cssClasses: String*): String = {
    flat1(cssClasses)
  }

  final def flat1(cssClasses: Seq[String]): String = {
    cssClasses.mkString( SPACE )
  }

  object Calc {
    final def PREFIX = "calc("
    final def SUFFIX = `)`
    def apply(content: String): String = PREFIX + content + SUFFIX
  }

  /** Всякие строковые литералы внутри CSS должны быть в ковычках. */
  def quoted(s: String): String = {
    val q = "'"
    q + s + q
  }

  final def _BLOCK          = "block"

  object Display {
    final def HIDDEN        = "hidden"
    final def BLOCK         = _BLOCK
    final def DISPLAY_BLOCK = "display-" + BLOCK
    final def VISIBLE       = "visible"
    final def INVISIBLE     = "visibility-hidden"
    final def INLINE_BLOCK  = "inline-" + _BLOCK
  }

  object Position {
    final def RELATIVE      = "relative"
    final def ABSOLUTE      = "abs"
  }

  object Overflow {
    final def HIDDEN = "ovh"
  }

  final def INFO_BLACK = "info-black"

  final def JS_PREFIX = "js-"

  final def CLEAR     = "clear"
  final def CLEARFIX  = CLEAR + "fix"

  final def _TITLE      = "title"
  final def _WIDGET     = "widget"
  final def _PREFIX     = "prefix"
  final def _CONTAINER  = "container"
  final def _SLIDE      = "slide"
  final def NAME      = "name"
  final def VALUE     = "value"
  private final def _NODENAME_ = "nodename"

  private final def _CNT = "_cnt"
  private final def PAGE = "page"

  final def __ = "__"

  final def CLICKABLE = "clickable"

  object Text {
    final def CENTERED = "centered"
  }


  object Coord {
    final def TOP = "top"
    final def LEFT = "left"
  }

  object Lk {

    object Paddings extends _EuSizes {
      override def PREFIX_ROOT = __ + "padding"
    }

    object Page {
      final def VIEWPORT = "viewport"
      final def PAGE_CNT = PAGE + _CNT
      final def PAGE_TITLE = PAGE + UNDERSCORE + _TITLE
    }

    /** div class=color для круглого div'а, заливаемого через css bg-color. */
    def COLOR = "color"

    def LINK = "link"
    def BLUE_LINK = "blue-" + LINK

    def _SM_PREFIX_ = "sm-"
    def SM_NOTE = _SM_PREFIX_ + "note"

    def PREFIX = "lk-"
    def MINOR_TITLE = "minor-" + _TITLE
    def LK_FIELD = PREFIX + "field"
    def LK_FIELD_NAME = LK_FIELD + __ + NAME


    object Maps {
      def MAP_CONTAINER = "radmap-" + _CONTAINER
    }


    object Adv {
      def ADV_ = "adv-"
      final def FORM_PREFIX = ADV_ + "management"
      def FORM_OUTER_DIV = FORM_PREFIX
      def RIGHT_BAR = FORM_PREFIX + UNDERSCORE + Bars.RightBar.RIGHT_BAR
      def LEFT_BAR  = FORM_PREFIX + "_left-bar"

      object Geo {
        def RCVR_POPUP = "rcvr-pop"
        def NODE_NAME_CONT = _NODENAME_
      }

      object Su {
        def CONTAINER = "adv-su-inputs-wrap"
      }

      /** Инфа по рекламному размещению. */
      object NodeInfo {

        def TARIFF   = "tariff"
        def IN_POPUP = __ + "in-" + Popup.POPUP

        private def TARIFF_PHOTO = TARIFF + "_photo"
        def TARIFF_PHOTO_LIST = TARIFF_PHOTO + "-lst"
        def TARIFF_PHOTO_IMG = TARIFF_PHOTO + "-i"

        def TARIFF_ILLUSTRATION = TARIFF + "_illustration"
        def TARIFF_ILLUSTRATION_W = TARIFF_ILLUSTRATION + "-w"

        def TARIFF_INFO = TARIFF + "_info"
        def TARIFF_INFO_TITLE = TARIFF_INFO + "-" + _TITLE
        def TARIFF_INFO_VALUE = TARIFF_INFO + "-value"

        def TARIFF_GREEN = TARIFF + "_green"

      }

    }


    /** Названия стилей для LkAds */
    object Ads {
      /** Список карточек. */
      object AdsList {
        def ADS_LIST = Adv.ADV_ + "lst"
        def CREATE_AD_BTN = "add-" + Adv.ADV_ + "btn"
        def LINE_DELIMITER = ADS_LIST + "_delimiter"
        def FIRST_IN_LINE = __ + "first-in-line"

        /** Имена стилей для элементов списка. */
        object Item {
          def AD_ITEM = Adv.ADV_ + "item"
          def AD_ITEM_PREVIEW = AD_ITEM + "_preview"
          // TODO Удалить этот стиль, если не используется. См. коммент в [lk-ads-sjs] AdItemR
          def AD_ITEM_PREVIEW_CONTAINER = AD_ITEM_PREVIEW + "-container"
          def AD_ITEM_PREVIEW_BOTTOM_ZIGZAG = AD_ITEM_PREVIEW + "-border"
          def EDIT_BTN = AD_ITEM + "_edit-btn"
          def CONTROLS = "ads-list-block__controls"
        }
      }
    }


    object Bars {

      def BAR_SUFFIX = "-bar"

      object RightBar {
        def PREFIX = "right"
        def RIGHT_BAR = PREFIX + BAR_SUFFIX

        object Price {
          def _PRICE = "price"
          def WIDGET = RIGHT_BAR + MINUS + _PRICE + MINUS + _WIDGET
          def WIDGET_CNT = WIDGET + _CNT
          def WIDGET_LOADER = WIDGET + "_loader"
          def WIDGET_TITLE = WIDGET + UNDERSCORE + _TITLE
          def WIDGET_PRICE_VALUE = WIDGET + UNDERSCORE + _PRICE + "-" + VALUE
          def WIDGET_REQ_BTN     = WIDGET + UNDERSCORE + "request-btn"
        }
      }

      object LeftBar {
        def PREFIX = "left"
        def LEFT_BAR = PREFIX + BAR_SUFFIX
      }

      def ACT = __ + "act"

    }


    /** Css-стили для формы LkNodes. */
    object Nodes {

      def LKN = "lkn"

      object Inputs {
        def INPUT70 = Input.INPUT + "70"
        def INPUT90 = Input.INPUT + "90"
      }

      object Name {
        def NAME      = _NODENAME_

        def NORMAL    = __ + "normal"
        def DISABLED  = __ + "disabled"

        def EDITING   = __ + "editing"
        def SHOWING   = __ + "showing"

        def EDIT_BTN  = "edit-btn"
        def TITLE     = _TITLE
        def CONTENT   = "content"

        def EDITING_BTNS = "editing-btns"
      }

      /** Менюшка узла. */
      object Menu {
        def MENU = "menu"
        def BTN  = "menu-btn"
        def CONT = "cont"
        def ITEM = "item"
      }

      /** Табличка key-value. */
      object KvTable {
        def LKN_TABLE  = "table"
        object Td {
          /** Класс ячейки с ключом. */
          def KEY = "key"
          /** Класс ячейки со значением. */
          def VALUE = "value"
        }
      }

      def DELIM = "delim"

    }

    /** Стили для попапов. */
    object Popup {
      def CLOSE = Buttons.CLOSE
      def ALEFT = "aleft"   // align=left с поправкой на попап. Изначально было реализовано только для .lkn.popup.aleft
      def POPUP = "popup"
      def POPUPS = POPUP + "s"
      def POPUPS_CONTAINER = POPUPS + "-" + _CONTAINER
      def POPUP_HEADER = POPUP + "-header"
    }

    object BxSlider {
      def JS_PHOTO_SLIDER = "js-photo-slider"
    }


    object HrDelim {
      def DELIMITER = "delimiter"
      def LIGHT = __ + "light"
    }

    object AdEdit {

      object Image {
        def IMAGE = "image"
        def IMAGE_REMOVE_BTN = IMAGE + "_remove-" + Buttons.BTN
      }

    }

    object Submit {
      def SUBMIT = "submit"
      def SUBMIT_W = SUBMIT + "-w"
    }

    object SlideBlocks {
      private def _PREFIX = _SLIDE + MINUS + _BLOCK
      final def OUTER     = _PREFIX
      final def TITLE     = _PREFIX + "_title"
      final def TITLE_BTN = _PREFIX + "_btn"
      final def BODY      = _PREFIX + _CNT
      final def OPENED    = __ + "js-open"
    }

    object Uploads {
      final def ADD_FILE_INPUT = "add-file-" + Input.INPUT
      final def IMAGE_ADD_BTN  = "image_add-btn"
    }


    object Adn {
      private def PROFILE_ = "profile_"
      object Edit {
        private def _BAR = "-bar"
        object Logo {
          def LOGO_BAR = PROFILE_ + "logo" + _BAR
        }
        object Info {
          def INFO_BAR = PROFILE_ + "info" + _BAR
        }
        object Colors {
          def COLOR_TITLE = __ + "color-title"
        }
      }
    }

  } // Lk


  object Colors {
    def WHITE = "white"
    def RED   = "red"
    def GREEN = "green"
    private[css] def GRAY  = "gray"
    def LIGHT_GRAY  = "light-" + GRAY
    def DARK_GRAY   = "dark-" + GRAY
  }


  object Dt {
    def DT_WIDGET = "date-widget"
    def OPTIONS   = DT_WIDGET + "_options"
    def RESULT    = DT_WIDGET + "_result"
    def RESULT_VALUE = RESULT + MINUS + VALUE
  }


  object Block {
    def BLOCK = _BLOCK
    /** bg image блока */
    def BG = "bg"
  }

  /** Стили для пачки цветов. Например,  */
  object ColorsBlock {
    /** Квадратик одного цвета. */
    def COLOR_BLOCK       = "color-" + _BLOCK
    private def PREFIX_   = COLOR_BLOCK + UNDERSCORE
    /** Контейнер списка цветов. */
    def LIST              = PREFIX_ + "lst"
  }

  trait _EuSizes {
    def PREFIX_ROOT: String
    def PREFIX = __ + PREFIX_ROOT + MINUS
    def S = PREFIX + "S"
    def M = PREFIX + "M"
    def L = PREFIX + "L"
    def XS = PREFIX + "XS"
  }


  object Margin extends _EuSizes{
    override def PREFIX_ROOT = "margin"
  }


  object PropTable {
    def TABLE     = "prop"
    def TD_NAME   = TABLE + UNDERSCORE + NAME
    def TD_VALUE  = TABLE + UNDERSCORE + VALUE
  }


  object Input {
    def INPUT         = "input"
    def INPUT_SHADOW  = INPUT + "_shadow"
    def INPUT_W       = INPUT + "-w"
    def JS_INPUT_W    = JS_PREFIX + INPUT_W
    def ERROR         = __ + "error"

    def CHECKBOX          = "checkbox"
    def STYLED_CHECKBOX   = "styled-" + CHECKBOX
    def CHECKBOX_TITLE    = CHECKBOX + "_title"

    def REQUIRED_ICON     = "required-icon"
  }


  object Size extends _EuSizes {
    override def PREFIX_ROOT = "size"
    def XM = PREFIX + "XM"
  }


  object TagsEdit {
    def PREFIX = "tag"
    def CONTAINER = PREFIX + _CONTAINER
    def JS_TAG_EDITABLE = JS_PREFIX + PREFIX + "-editable"
    def JS_TAG_DELETE   = JS_PREFIX + PREFIX + "-delete"
  }


  object HintList {
    def PREFIX    = "hint-list_"

    def CONTAINER = PREFIX + _CONTAINER
    def OUTER     = PREFIX + "outer"
    def CONTENT   = PREFIX + "content"

    def ROW       = "row"
  }

  object Buttons {

    def BTN = "btn"
    def CLOSE = "close"
    def BTN_W = BTN + "-w"

    def PREFIX = __

    private def major = "major"
    def RADIAL_MAJOR = PREFIX + "radial-" + major
    def MAJOR     = PREFIX + major
    def MINOR     = PREFIX + "minor"
    def NEGATIVE  = PREFIX + "negative"
    def HELPER    = PREFIX + "helper"
    def DISABLED  = PREFIX + "disabled"

    def LIST      = PREFIX + "list"

  }


  /** CSS-классы для сборки таблицы, используемой в списке транзакций, в корзине, в других шаблонах. */
  object Table {

    def TABLE = "table"

    object Width {
      private def _PREFIX_ = __ + "width-"
      def XL = _PREFIX_ + "XL"
    }

    object Td {
      def TD = "td"
      def WHITE = __ + Colors.WHITE
      def GRAY  = __ + Colors.GRAY

      object Radial {
        private def _PREFIX_ = __ + "radial-"
        def FIRST = _PREFIX_ + "first"
        def LAST  = _PREFIX_ + "last"
      }
    }

  }


  /** Шрифты. */
  object Font {

    /** Размеры шрифтов.*/
    object Sz extends _EuSizes {
      override def PREFIX_ROOT = "ft"
      override def PREFIX = PREFIX_ROOT + "-"
    }

  }


  /** Позиционирование "плавающих" элементов. */
  object Floatt {
    private def _PREFIX_ = "f-"
    def LEFT  = _PREFIX_ + "left"
    def RIGHT = _PREFIX_ + "right"
  }


  object Cursor {
    final private def PREFIX_ = "cursor-"
    final def MOVE = PREFIX_ + "move"
    final def POINTER = PREFIX_ + "pointer"
    final def GRAB = PREFIX_ + "grab"
    final def GRABBING = PREFIX_ + "grabbing"
    final def VERTICAL_RESIZE = PREFIX_ + "resize-v"
  }


  object Anim {

    private def `3D` = "3d"

    /** @see [[https://developer.mozilla.org/en-US/docs/Web/CSS/transition]] */
    object Transition {

      object Properties {
        final def ALL = "all"
      }

      object TimingFuns {

        private def EASE = "ease"
        private def OUT  = "out"
        private def IN   = "in"

        final def EASE_OUT: String = EASE + MINUS + OUT
        final def EASE_IN_OUT: String = {
          val d = MINUS
          EASE + d + IN + d + OUT
        }
        final def EASE_IN: String = EASE + MINUS + IN

      }

      final def duration(durationSec: Double): String = {
        durationSec + "s"
      }

      final def all(durationSec: Double, timingFun: String): String = {
        val s = SPACE
        Properties.ALL + s + duration(durationSec) + s + timingFun
      }

    }


    object Origin {

      def TOP_LEFT = Coord.TOP + SPACE + Coord.LEFT

    }

    object Transform {

      final def TRANSLATE = "translate"
      final def TRANSLATE_3D = TRANSLATE + `3D`

      final def MATRIX = "matrix"
      final def MATRIX_3D = MATRIX + `3D`

    }

  }

}
