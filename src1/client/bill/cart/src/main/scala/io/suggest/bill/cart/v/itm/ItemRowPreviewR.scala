package io.suggest.bill.cart.v.itm

import io.suggest.dev.MSzMults
import io.suggest.jd.{MJdConf, MJdDoc}
import io.suggest.jd.render.m.MJdRuntime

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.18 16:57
  * Description: Остаточная утиль для jd-рендера в ячейке preview.
  */

object ItemRowPreviewR {

  /** Инстанс jd-conf един среди всего компонента. */
  val JD_CONF = MJdConf(
    isEdit = false,
    szMult = MSzMults.`0.25`,
    gridColumnsCount = 2
  )

  /** Сборка пустого стиля для jd-рендера. */
  def mkJdRuntime(templates: Stream[MJdDoc] = Stream.empty): MJdRuntime =
    MJdRuntime.make( templates, JD_CONF )

}
