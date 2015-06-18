package io.suggest.ad.search

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.05.15 11:27
 * Description: Констатны поиска рекламных карточек.
 */
object AdSearchConstants {

  def PRODUCER_ID_FN     = "shopId"
  def CAT_ID_FN          = "catId"
  def LEVEL_ID_FN        = "level"
  def FTS_QUERY_FN       = "q"
  def RESULTS_LIMIT_FN   = "size"
  def RESULTS_OFFSET_FN  = "offset"
  def RECEIVER_ID_FN     = "rcvr"
  def FIRST_AD_ID_FN     = "firstAdId"
  def GENERATION_FN      = "gen"
  def GEO_MODE_FN        = "geo"
  def SCREEN_INFO_FN     = "screen"
  def LAST_PROD_ID_FN    = "lpi"
  def API_VSN_FN         = "v"

  /**
   * v1
   * Название поля для focused-ads api.
   * Содержит флаг, сообщающий о необходимости особого рендера первой карточки. */
  def WITH_HEAD_AD_FN    = "h"

  /**
   * v2
   * id продьюсера, на котором закончилась предыдущая цепочка карточек.
   * Поле пришло на смену слишком нефункционального флага with-head-ad.
   */
  def FADS_LAST_PROD_ID_FN    = "y"

  /** Имя поля, содержащее id карточки, которая должна быть обязательно в focused-ответе. */
  def ONLY_WITH_AD_ID_FN      = "j"

}
