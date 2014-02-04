package io.suggest.ym.model

import io.suggest.util.CascadingFieldNamer
import cascading.tuple.{TupleEntry, Tuple, Fields}
import com.scaleunlimited.cascading.BaseDatum

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.02.14 14:01
 * Description: Данные по логину-паролю для запроса прайса хранятся в этом датуме.
 */
object AuthInfoDatum extends CascadingFieldNamer {

  val VERSION_FN  = fieldName("version")
  val USERNAME_FN = fieldName("username")
  val PASSWORD_FN = fieldName("password")

  val FIELDS = new Fields(VERSION_FN, USERNAME_FN, PASSWORD_FN)

  val VERSION_DFLT = 1

  /** Сборка кортежей на случай появления разных версий в хранилищах. */
  def apply(t: Tuple) = {
    val vsn: Int = t getInteger 0
    if (vsn == 1) {
      new AuthInfoDatum(t)
    } else {
      throw new IllegalArgumentException(s"Unknown ${classOf[AuthInfoDatum].getSimpleName} version: $vsn")
    }
  }
}

import AuthInfoDatum._

class AuthInfoDatum extends BaseDatum(FIELDS) {

  def this(t: Tuple) = {
    this
    setTuple(t)
  }

  def this(te: TupleEntry) = {
    this
    setTupleEntry(te)
  }

  def this(username:String, password:String) = {
    this
    this.version  = VERSION_DFLT
    this.username = username
    this.password = password
  }

  def version = _tupleEntry getInteger VERSION_FN
  def version_=(vsn: Int) = _tupleEntry.setInteger(VERSION_FN, vsn)

  def username = _tupleEntry getString USERNAME_FN
  def username_=(username: String) = _tupleEntry.setString(USERNAME_FN, username)

  def password = _tupleEntry getString PASSWORD_FN
  def password_=(password: String) = _tupleEntry.setString(PASSWORD_FN, password)
}
