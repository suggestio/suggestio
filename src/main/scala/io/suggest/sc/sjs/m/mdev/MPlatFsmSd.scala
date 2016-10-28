package io.suggest.sc.sjs.m.mdev

import io.suggest.sjs.common.fsm.SjsFsm

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.10.16 11:52
  * Description: Контейнер данных состояния для [[io.suggest.sc.sjs.c.plat.PlatformFsm]].
  */
case class MPlatFsmSd(
  subscribers: Map[String, List[SjsFsm]] = Map.empty
) {

  def withSubscribers(subs2: Map[String, List[SjsFsm]]) = copy(subscribers = subs2)

}
