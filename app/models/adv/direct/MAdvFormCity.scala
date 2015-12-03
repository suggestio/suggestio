package models.adv.direct

import models._

/** advForm: Описание одного города в списке городов. */
case class MAdvFormCity(
  node        : MNode,
  cats        : Seq[MAdvFormCityCat],
  i           : Int,
  isSelected  : Boolean = false
)
