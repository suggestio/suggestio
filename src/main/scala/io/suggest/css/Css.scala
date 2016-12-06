package io.suggest.css

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.16 17:59
  * Description:
  */
object Css {

  object Lk {

    def PREFIX = "lk-"

    def MINOR_TITLE = "minor-title"

    def LK_FIELD = PREFIX + "field"

    def LK_FIELD_NAME = LK_FIELD + "__name"

    object Adv {

      def FORM_PREFIX = "adv-management"

      def RIGHT_BAR = FORM_PREFIX + "_right-bar"

      def LEFT_BAR  = FORM_PREFIX + "_left-bar"

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

    def RESULT_VALUE = RESULT + "-value"

  }


  object Block {

    def BLOCK = "block"

  }

  trait _EuSizes {

    def PREFIX_ROOT: String

    def PREFIX = "__" + PREFIX_ROOT + "-"

    def S = PREFIX + "S"

  }


  object Margin extends _EuSizes{

    override def PREFIX_ROOT = "margin"

  }


  object PropTable {

    def TABLE     = "prop"

    def TD_NAME   = TABLE + "_name"

    def TD_VALUE  = TABLE + "_value"

  }


  object Input {

    def INPUT = "input"

  }

  object Size extends _EuSizes {

    override def PREFIX_ROOT = "size"

  }

}
