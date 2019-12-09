package io.suggest.tags

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


    /** Макс.длина текстового поискового запроса. */
    def TAGS_QUERY_MAXLEN   = 64

    /** Максимальное значение limit в qs. */
    def LIMIT_MAX           = 50

    /** Максимальное значение offset в qs. */
    def OFFSET_MAX          = 200

  }


  /** Константы tag-ответов сервера. */
  object Resp {

    def RENDERED_FN     = "a"

    def FOUND_COUNT_FN  = "b"

  }

}
