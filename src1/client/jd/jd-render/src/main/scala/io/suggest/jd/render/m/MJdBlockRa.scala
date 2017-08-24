package io.suggest.jd.render.m

import io.suggest.jd.tags.Strip

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.17 15:56
  * Description: Данные по документу для рендера только верхнего блока (в плитке).
  */
case class MJdBlockRa(
                       templateBlockStrip : Strip,
                       common             : MJdCommonRa
                     )

