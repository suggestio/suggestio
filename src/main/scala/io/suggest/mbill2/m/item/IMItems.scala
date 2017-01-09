package io.suggest.mbill2.m.item

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.12.15 12:04
 * Description: Интерфейс для доступа к DI-полю с инстансом [[io.suggest.mbill2.m.item.MItems]].
 */
trait IMItems {

  /** DI-экземпляр slick-модели [[io.suggest.mbill2.m.item.MItems]]. */
  val mItems: MItems

}
