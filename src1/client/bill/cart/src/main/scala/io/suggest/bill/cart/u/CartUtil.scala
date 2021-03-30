package io.suggest.bill.cart.u

import io.suggest.dev.MSzMults
import io.suggest.jd.render.m.MJdRuntime
import io.suggest.jd.render.u.JdUtil
import io.suggest.jd.{MJdConf, MJdDoc}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.18 16:57
  * Description: Остаточная утиль для jd-рендера в ячейке preview.
  */

object CartUtil {

  /** Инстанс jd-conf един среди всего компонента. */
  val JD_CONF = MJdConf(
    isEdit = false,
    szMult = MSzMults.`0.25`,
    gridColumnsCount = 2
  )

  /** Сборка пустого стиля для jd-рендера. */
  def mkJdRuntime(templates: LazyList[MJdDoc] = LazyList.empty): MJdRuntime = {
    JdUtil
      .prepareJdRuntime(JD_CONF)
      .docs(templates)
      .make
  }

}
