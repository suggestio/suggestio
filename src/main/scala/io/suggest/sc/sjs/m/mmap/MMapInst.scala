package io.suggest.sc.sjs.m.mmap

import io.suggest.sc.sjs.vm.search.tabs.geo.SGeoContent
import io.suggest.sjs.mapbox.gl.map.GlMap

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.07.16 22:52
  * Description: Модель-контейнер данных по инстансу карты.
  */
case class MMapInst(
  glmap     : GlMap,
  container : SGeoContent
)
