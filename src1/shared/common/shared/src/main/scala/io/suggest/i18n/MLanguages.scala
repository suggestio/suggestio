package io.suggest.i18n

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/** Enum describing supported/known languages. */
object MLanguages extends StringEnum[MLanguage] {

  // Keep alphabetical order here.

  case object English extends MLanguage("en") {
    override def countryFlagEmoji = "\uD83C\uDDEC\uD83C\uDDE7"
    override def singularNative = singularMsgCode
    override def singularMsgCode = "English"
  }

  case object Russian extends MLanguage("ru") {
    override def countryFlagEmoji = "\uD83C\uDDF7\uD83C\uDDFA"
    override def singularNative = "Русский"
    override def singularMsgCode = "Russian"
  }

  override def values = findValues

  final def default = English

  def byCode(langCode: String): MLanguage = {
    val langCodeLc = langCode.toLowerCase
    if (
      ("ru" :: "be" :: "uk" :: Nil)   // TODO Need more languages here...
        .exists(langCodeLc startsWith _)
    )
      Russian
    else
      English
  }

}


sealed abstract class MLanguage(override val value: String) extends StringEnumEntry {

  /** Country Flag UTF emoji. */
  def countryFlagEmoji: String

  /** Singular name in this native language. Displayed for end-user as-is, without translation. */
  def singularNative: String

  /** Code string for translatable value name. */
  def singularMsgCode: String

}

object MLanguage {

  @inline implicit def univEq: UnivEq[MLanguage] = UnivEq.derive

  implicit def languageFormat: Format[MLanguage] =
    EnumeratumUtil.valueEnumEntryFormat( MLanguages )

}
