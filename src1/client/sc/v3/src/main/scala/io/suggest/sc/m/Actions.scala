package io.suggest.sc.m

import io.suggest.sjs.common.spa.DAction

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 12:01
  * Description: diode-экшены выдачи v3.
  */

/** Маркер-интерфейс для  */
trait ISc3Action extends DAction

/** Изменились параметры экрана устройства.
  * Экшен может приходить несколько раз или вообще оптом. */
case object ScreenReset extends ISc3Action

