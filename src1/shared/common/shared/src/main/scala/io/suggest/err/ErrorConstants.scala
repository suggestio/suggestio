package io.suggest.err

import io.suggest.common.empty.EmptyUtil
import io.suggest.common.html.HtmlConstants.`.`
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.ueq.UnivEqUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.04.15 11:15
 * Description: Клиент-серверные константы, связанные с возникающими ошибками.
 */
object ErrorConstants {

  def EMSG_CODE_PREFIX = "e" + `.`


  /** Сборка частой функции, генерирующей msg-коды ошибок.
    * Обычно код начинается на "e.", затем идёт абстрактный идентификатор, затем суффикс конечного пояснения.
    * Например: "e.strip.name".
    */
  def emsgF(root: String): String => String = { suffix: String =>
    EMSG_CODE_PREFIX + root + `.` + suffix
  }

  /** Класс ошибки в формах. */
  def FORM_CSS_CLASS = "__error"

  def ERROR_MSG_CLASS = "error-msg"

  /** Ошибки, возвращаемые через JSON. */
  object Json {

    /** Имя JSON-поля с кодом ошибки. */
    def CODE_FN  = "c"

    /** Имя JSON-поля с сообещнием об ошибке, пригодным для отображения юзера. */
    def MSG_FN   = "m"

  }

  /** Клиент-серверные константы для моделей MRemoteError/MRme*. */
  object Remote {

    def MSG_FN = "msg"

    def URL_FN = "url"

    def ERROR_CODE_FN = "ec"

    def SEVERITY_FN   = "se"


    def MSG_LEN_MIN = 1
    def MSG_LEN_MAX = 5000

    def URL_LEN_MIN = 8
    def URL_LEN_MAX = 1024

  }


  /** Аналог scala.assert(), но без ELIDEABLE и без Error. */
  def assertArg(assertion: Boolean): Unit =
    assertArg(assertion: Boolean, "Assertion failed")
  def assertArg(assertion: Boolean, errMsg: => String): Unit = {
    if (!assertion)
      throw MCheckException( errMsg )
  }

  object Words {

    def EXPECTED    = "expected"
    def MISSING     = "missing"
    def TOO_MANY    = "too.many"
    def UNEXPECTED  = "un" + EXPECTED
    def INVALID     = "invalid"

  }

}


/** Exception, выстреливаемый из assertArg(). */
final case class MCheckException(
                                  override val getMessage  : String,
                                  fields                   : Seq[String]         = Nil,
                                  localizedMessage         : Option[String]      = None,
                                )
  extends IllegalArgumentException
{
  override def getLocalizedMessage: String =
    localizedMessage getOrElse super.getLocalizedMessage
}

object MCheckException {

  /** Поддержка JSON. */
  implicit def checkExceptionJson: OFormat[MCheckException] = (
    (__ \ "m").format[String] and
    (__ \ "f").formatNullable[Seq[String]]
      .inmap[Seq[String]](
        EmptyUtil.opt2ImplEmpty1F( Nil ),
        { xs => if (xs.isEmpty) None else Some(xs) }
      ) and
    (__ \ "l").formatNullable[String]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MCheckException] = UnivEq.derive

}
