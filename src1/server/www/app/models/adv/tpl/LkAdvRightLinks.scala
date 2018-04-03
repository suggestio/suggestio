package models.adv.tpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.11.15 15:24
 * Description: Модель ссылок правой панели в ЛК при размещении карточки.
 */
object LkAdvRightLinks extends Enumeration {

  type T = Value

  val GEO, EXT, AD_NODES = Value : T

}
