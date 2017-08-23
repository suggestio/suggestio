package models.mbase

import io.suggest.model.n2.node.MNode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.16 16:30
  * Description: Интерфейс к полю с инстансом рекламной карточки.
  */
trait IMad {

  /** Рекламная карточка. */
  def mad: MNode

}
