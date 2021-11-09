package io.suggest.id.login.m.reg.step3

import io.suggest.spa.FastEqUtil
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.19 13:52
  * Description: Состояние чек-боксов и прочего содержимого на странице галочек.
  */
object MReg3CheckBoxes {

  def empty = apply()

  implicit val reg3CheckboxesFeq = FastEqUtil[MReg3CheckBoxes] { (a, b) =>
    (a.cbStates ===* b.cbStates)
  }

  @inline implicit def univEq: UnivEq[MReg3CheckBoxes] = UnivEq.derive

  def cbStates = GenLens[MReg3CheckBoxes](_.cbStates)

  /** Default checkboxes state map. */
  def defaultMap: Map[MReg3CheckBoxType, MRegCheckBoxS] = {
    val cbState0 = MRegCheckBoxS.empty

    MReg3CheckBoxTypes
      .values
      .foldLeft( Map.empty[MReg3CheckBoxType, MRegCheckBoxS] ) { (acc0, cbType) =>
        acc0 + (cbType -> cbState0)
      }
  }

}


/** Контент под-формы финальных чекбоксов.
  *
  * @param cbStates State map of checkboxes.
  *                 Must always contain all keys from MReg3CheckBoxType.values
  */
case class MReg3CheckBoxes(
                            cbStates        : Map[MReg3CheckBoxType, MRegCheckBoxS]       = MReg3CheckBoxes.defaultMap,
                          ) {

  def canSubmit: Boolean = {
    cbStates
      .valuesIterator
      .forall( _.isChecked )
  }

}
