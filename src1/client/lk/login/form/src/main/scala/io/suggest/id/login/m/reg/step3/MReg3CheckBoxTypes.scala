package io.suggest.id.login.m.reg.step3

import enumeratum.{Enum, EnumEntry}
import japgolly.univeq.UnivEq

object MReg3CheckBoxTypes extends Enum[MReg3CheckBoxType] {

  case object UserAgreement extends MReg3CheckBoxType
  case object PrivacyPolicy extends MReg3CheckBoxType
  case object PersonalData extends MReg3CheckBoxType

  override def values = findValues

}


sealed abstract class MReg3CheckBoxType extends EnumEntry

object MReg3CheckBoxType {

  @inline implicit def univEq: UnivEq[MReg3CheckBoxType] = UnivEq.derive

}