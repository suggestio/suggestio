package util.ident.store

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq._
import play.api.libs.json.Format

/** Credential types model, previosly was inlined into MPredicates.Ident.* predicates. */
object MCredentialTypes extends StringEnum[MCredentialType] {

  /** Email address. */
  case object Email extends MCredentialType("email")

  /** Phone number, usually - mobile phone number. */
  case object Phone extends MCredentialType("phone")

  /** User id on external identification provider (OAuth2, OpenID, etc). */
  case object ExternalService extends MCredentialType("extsvc")

  /** Password data. */
  case object Password extends MCredentialType("password")

  /** Language.
    * Stored here for API simplification (lang is wanted during new user creation stage).
    */
  case object Language extends MCredentialType("lang")


  override def values = findValues

}


/** Identity type marker. */
sealed abstract class MCredentialType(override val value: String) extends StringEnumEntry

object MCredentialType {

  @inline implicit def univEq: UnivEq[MCredentialType] = UnivEq.derive

  implicit def credTypeJson: Format[MCredentialType] =
    EnumeratumUtil.valueEnumEntryFormat( MCredentialTypes )

}