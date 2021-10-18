package io.suggest.bill.cart

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.02.16 22:59
  * Description: Enum-model for possible cart processing decisions.
  */
object MCartIdeas extends StringEnum[MCartIdea] {

  /** Nothing in cart to process (empty cart? already processed?) */
  case object NothingToDo extends MCartIdea("nothing_to_do")

  /** Cart processed using user's balance, no external money needed. */
  case object OrderClosed extends MCartIdea("order_closed")

  /** User need some external money via external pay-system. */
  case object NeedMoney extends MCartIdea("need_money")


  override def values = findValues

}


sealed abstract class MCartIdea(override val value: String) extends StringEnumEntry

object MCartIdea {

  @inline implicit def univEq: UnivEq[MCartIdea] = UnivEq.derive

  implicit def cartIdeaJson: Format[MCartIdea] =
    EnumeratumUtil.valueEnumEntryFormat( MCartIdeas )

}
