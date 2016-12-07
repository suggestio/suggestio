package io.suggest.css

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.16 17:59
  * Description:
  */
object Css {

  def JS_PREFIX = "js-"

  def CLEAR     = "clear"
  def CLEARFIX  = CLEAR + "fix"

  def _PREFIX     = "prefix"
  def _CONTAINER  = "container"
  def NAME      = "name"
  def VALUE     = "value"

  object Lk {

    def PREFIX = "lk-"
    def MINOR_TITLE = "minor-title"
    def LK_FIELD = PREFIX + "field"
    def LK_FIELD_NAME = LK_FIELD + "__" + NAME

    object Adv {
      def FORM_PREFIX = "adv-management"
      def FORM_OUTER_DIV = FORM_PREFIX
      def RIGHT_BAR = FORM_PREFIX + "_right-bar"
      def LEFT_BAR  = FORM_PREFIX + "_left-bar"

      object Geo {
        def MAP_CONTAINER = "radmap-" + _CONTAINER
      }

    }

  }


  object Project {
    def RED = "red"
    def GREEN = "green"
  }


  object Dt {
    def DT_WIDGET = "date-widget"
    def OPTIONS   = DT_WIDGET + "_options"
    def RESULT    = DT_WIDGET + "_result"
    def RESULT_VALUE = RESULT + "-" + VALUE
  }


  object Block {
    def BLOCK = "block"
  }


  trait _EuSizes {
    def PREFIX_ROOT: String
    def PREFIX = "__" + PREFIX_ROOT + "-"
    def S = PREFIX + "S"
    def M = PREFIX + "M"
  }


  object Margin extends _EuSizes{
    override def PREFIX_ROOT = "margin"
  }


  object PropTable {
    def TABLE     = "prop"
    def TD_NAME   = TABLE + "_" + NAME
    def TD_VALUE  = TABLE + "_" + VALUE
  }


  object Input {
    def INPUT         = "input"
    def INPUT_SHADOW  = INPUT + "_shadow"
    def INPUT_W       = INPUT + "-w"
    def JS_INPUT_W    = JS_PREFIX + INPUT_W
    def ERROR         = "__error"
  }


  object Size extends _EuSizes {
    override def PREFIX_ROOT = "size"
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

    def PREFIX = "__"

    def MAJOR     = PREFIX + "major"
    def MINOR     = PREFIX + "minor"
    def NEGATIVE  = PREFIX + "negative"
    def HELPER    = PREFIX + "helper"

  }

}
