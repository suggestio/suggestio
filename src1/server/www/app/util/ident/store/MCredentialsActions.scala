package util.ident.store

import enumeratum.{Enum, EnumEntry}

object MCredentialsActions extends Enum[MCredentialsAction] {

  /** Search (read) credentials from storage. */
  case object Search extends MCredentialsAction

  /** Save credentials. */
  case object Write extends MCredentialsAction


  override def values = findValues

}

sealed abstract class MCredentialsAction extends EnumEntry
