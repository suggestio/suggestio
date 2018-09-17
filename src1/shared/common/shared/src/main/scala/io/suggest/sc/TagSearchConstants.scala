package io.suggest.sc

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.09.15 17:33
 * Description: Константы протокола поиска тегов.
 */
object TagSearchConstants {

  /** Константы tag-реквестов. */
  object Req {

    /** Поле запроса полнотекстового запроса поиска тегов по имени. */
    def FACE_FTS_QUERY_FN = "q"

    /** Кол-во возвращаемых результатов. */
    def LIMIT_FN          = "l"

    /** Абсолютный сдвиг в результатах поиска. */
    def OFFSET_FN         = "o"

    /** Данные геолокации, если есть. */
    def LOC_ENV_FN        = ScConstants.ReqArgs.LOC_ENV_FN

    /** id узла-ресивера, в рамках которого надо искать. */
    def RCVR_ID_FN        = "r"

  }


  /** Константы tag-ответов сервера. */
  object Resp {

    def RENDERED_FN     = "a"

    def FOUND_COUNT_FN  = "b"

  }

}
