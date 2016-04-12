package io.suggest.sc.sjs.m.mmap

import io.suggest.sjs.mapbox.gl.map.GlMap

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 19:13
  * Description: Модель состояния FSM, обслуживающего карту выдачи.
  * @param glmap Инстанс мапы.
  * @param ensure Если требование инициализации карты пришло слишком рано или совсем невовремя, то оно попадает сюда
  *               для дальнейшей обработки в последующих состояниях.
  */
case class MbFsmSd(
  glmap   : Option[GlMap]       = None,
  ensure  : Option[EnsureMap]   = None
)
