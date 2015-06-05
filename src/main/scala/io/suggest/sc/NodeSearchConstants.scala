package io.suggest.sc

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.06.15 14:48
 * Description: Константы для поиска узлов.
 */
object NodeSearchConstants {

  // Названия qs-полей для сборки запроса к списку гео-узлов.
  /** Текстовый поиск по имени узла. */
  def FTS_QUERY_FN            = "q"

  /** Геоданные для поиска. */
  def GEO_FN                  = "geo"

  /** Сдвиг результатов. */
  def OFFSET_FN               = "offset"

  /** Лимит результатов. */
  def LIMIT_FN                = "limit"

  /** id текущего узла. */
  def CURR_ADN_ID_FN          = "cai"

  /** Флаг активации поиска нового узла. */
  // TODO Выпилить этот дефектный флаг, он в index-запросе живёт, а не здесь.
  def NODE_SWITCH_FN          = "nodesw"

  /** Что-то связанное с соседними узлами... */
  def WITH_NEIGHBORS_FN       = "neigh"

  /** Версия API выдачи. */
  def API_VSN_FN              = "v"

}
