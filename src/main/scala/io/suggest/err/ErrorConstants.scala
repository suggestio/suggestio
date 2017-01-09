package io.suggest.err

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.04.15 11:15
 * Description: Клиент-серверные константы, связанные с возникающими ошибками.
 */
object ErrorConstants {

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

  }

}
