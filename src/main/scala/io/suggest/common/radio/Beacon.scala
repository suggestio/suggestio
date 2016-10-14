package io.suggest.common.radio

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 16:26
  * Description: Трейт наблюдаемых данных по ровно одному маячку.
  * Изначально назывался IBeacon, но это слегка конфликтовало с ябло-брендом.
  */
trait Beacon extends IRadioSignalInfo {

  /** Некий внутренний уникальный id/ключ маячка, если он вообще есть.
    *
    * Option[], потому что в гугле смогли уродить маячки с каким-то кастрированными URL внутри фрейма.
    * Скорее всего, маячки без uid останутся бесполезны для s.io и в будущем.
    */
  def uid: Option[String]

}
