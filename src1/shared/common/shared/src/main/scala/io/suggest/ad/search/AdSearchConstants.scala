package io.suggest.ad.search

import io.suggest.sc.ScConstants

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.05.15 11:27
 * Description: Констатны поиска рекламных карточек.
 */
object AdSearchConstants {

  def PRODUCER_ID_FN      = "p"
  def LIMIT_FN            = "l"
  def OFFSET_FN           = "o"
  def RECEIVER_ID_FN      = "r"
  def GENERATION_FN       = "d"
  def SCREEN_INFO_FN      = "s"

  /** Модель данных физического окружения клиентского устройства. */
  def LOC_ENV_FN          = ScConstants.ReqArgs.LOC_ENV_FN

  /** id узла тега. */
  def TAG_NODE_ID_FN       = "t"


  /** 2015.aug.26: Название опционального поля с флагом того, разрешено ли серверу возвращать
    * index answer вместо focused ads. */
  def FOC_INDEX_ALLOWED_FN   = "n"

  /** @return Имя поля с кодом используемого режима lookup'а карточки. */
  def AD_LOOKUP_MODE_FN    = "k"

  /** v2
    * Имя поля, содержащее id карточки, которую нужно поискать в размещениях на указанном/текущем ресивере
    * (координатах), и определить offset/size самостоятельно.
    * Выставляется при переходе на карточку по id карточки, например при десериализации js-состояния из URL.
    */
  def LOOKUP_AD_ID_FN      = "j"

  /** Имя поля с текстовым запросом поиска.
    * Изначально, это было tagSearchQuery в классе tags search,
    * но теперь всё унифицировано.
    */
  def TEXT_QUERY_FN = "q"

}