package models.adv.direct

import io.suggest.model.n2.node.MNode


/** advForm: Описание одного города в списке городов. */
case class MAdvFormCity(
  node        : MNode,
  cats        : Seq[MAdvFormCityCat],
  i           : Int,
  isSelected  : Boolean = false
)
