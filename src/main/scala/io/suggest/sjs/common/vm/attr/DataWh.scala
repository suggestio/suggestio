package io.suggest.sjs.common.vm.attr

import io.suggest.adv.ext.model.im.Size2di
import io.suggest.sc.ScConstants.{HEIGHT_ATTR, WIDTH_ATTR}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.08.15 13:40
 * Description: Утиль для поддержки data-width и data-height аттрибутов.
 */

/** Поддержка доступа к значению тега data-height. */
trait DataHeight extends AttrVmT {
  protected def getDataHeight = getIntAttribute(HEIGHT_ATTR)
}


/** Поддержка доступа к значению тега data-width. */
trait DataWidth extends AttrVmT {
  protected def getDataWidth = getIntAttribute(WIDTH_ATTR)
}


/** Поддержка работы с data-width и -height аттрибутами. */
trait DataWh extends DataHeight with DataWidth {

  /** Прочитать оба значения ширины и длины, вернув их в контейнере Size2d. */
  protected def getDataWh: Option[Size2di] = {
    for {
      h <- getDataHeight
      w <- getDataWidth
    } yield {
      Size2di(height = h, width = w)
    }
  }

}
