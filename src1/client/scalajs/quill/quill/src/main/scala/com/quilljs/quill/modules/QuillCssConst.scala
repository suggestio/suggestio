package com.quilljs.quill.modules

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.03.18 17:49
  * Description: Constants for Quill css styles.
  */
object QuillCssConst {

  def QL = "ql"

  def DELIM = "-"

  def QL_IMAGE = QL + DELIM + QuillModulesNames.Formats.IMAGE

  final val NAME_DELIM = "-"

  val QL_ = ".ql" + NAME_DELIM

  val QL_SNOW_CSS_SEL = QL_ + "snow"

  val QL_EDITOR_CSS_SEL = QL_ + "editor"

  val QL_PICKER_CSS_SEL = QL_ + "picker"

  val QL_TOOLTIP_CSS_SEL = QL_ + "tooltip"
  val QL_EDITING_CSS_SEL = QL_ + "editing"

  val LABEL = "label"
  val ITEM = "item"

}
