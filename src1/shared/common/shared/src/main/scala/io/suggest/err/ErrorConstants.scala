package io.suggest.err

import io.suggest.common.html.HtmlConstants.`.`

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

    def STATE_FN = "state"

    def ERROR_CODE_FN = "ec"

    def SEVERITY_FN   = "se"


    def MSG_LEN_MIN = 1
    def MSG_LEN_MAX = 5000

    def URL_LEN_MIN = 8
    def URL_LEN_MAX = 1024

    def STATE_LEN_MAX = 4096

  }


  /** Аналог scala.assert(), но без ELIDEABLE и без Error. */
  def assertArg(assertion: Boolean): Unit = {
    if (!assertion)
      throw AssertArgException
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
case object AssertArgException extends IllegalArgumentException
