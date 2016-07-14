package io.suggest.ad.search

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.05.15 11:27
 * Description: Констатны поиска рекламных карточек.
 */
object AdSearchConstants {

  def PRODUCER_ID_FN     = "shopId"
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
  def WITHOUT_IDS_FN     = "woi"

  /** 2015.aug.26: Название опционального поля с флагом того, разрешено ли серверу возвращать
    * index answer вместо focused ads. */
  def FOC_JUMP_ALLOWED_FN   = "n"

  /**
   * v1
   * Название поля для focused-ads api.
   * Содержит флаг, сообщающий о необходимости особого рендера первой карточки.
   */
  def WITH_HEAD_AD_FN    = "h"


  /** @return Имя поля с кодом используемого режима lookup'а карточки. */
  def AD_LOOKUP_MODE_FN    = "k"

  /** v2
    * Имя поля, содержащее id карточки, которую нужно поискать в размещениях на указанном/текущем ресивере
    * (координатах), и определить offset/size самостоятельно.
    * Выставляется при переходе на карточку по id карточки, например при десериализации js-состояния из URL.
    */
  def AD_ID_LOOKUP_FN      = "j"

  /** AdvGeoPoint. Имя поля для текущей точки на карте, для которой делается запрос. */
  def AGP_POINT_FN         = "g"

}
