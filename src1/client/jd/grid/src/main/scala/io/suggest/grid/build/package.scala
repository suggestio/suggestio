package io.suggest.grid

import io.suggest.common.geom.d2.MSize2di

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.11.17 16:11
  */
package object build {

  /** Результат сборки плитки, публикуемый наружу через callback. */
  type GridBuildRes_t = MSize2di

  /** Callback, слушающий итоги построения сетки. */
  type OnLayoutF_t = GridBuildRes_t => _

}
