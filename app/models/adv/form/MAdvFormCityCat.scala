package models.adv.form

import models._

/** advForm: Описание одной вкладки группы узлов в рамках города. */
case class MAdvFormCityCat(
  shownType   : AdnShownType,
  nodes       : Seq[MAdvFormNode],
  name        : String,
  i           : Int,
  isSelected  : Boolean = false
)
