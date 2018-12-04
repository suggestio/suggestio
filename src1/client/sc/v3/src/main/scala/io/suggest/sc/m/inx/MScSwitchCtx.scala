package io.suggest.sc.m.inx

import io.suggest.geo.MGeoLoc
import io.suggest.sc.index.MScIndexArgs
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.11.18 14:57
  * Description: Обновление всей выдачи (с узла на узел, например) - процесс в несколько этапов:
  * [геолокация], запрос индекса выдачи, [запрос подтверждения], [плитка].
  */

object MScSwitchCtx {

  @inline implicit def univEq: UnivEq[MScSwitchCtx] = UnivEq.derive

}


/** Инстанс модель живёт прямо в экшенах, но в некоторых случаях - в состоянии.
  *
  * @param focusedAdId Фокусироваться на id карточки.
  * @param demandLocTest Это процесс проверки смены локации после ре-активации существующей выдачи?
  * @param indexQsArgs Аргументы для запроса индекса.
  * @param forceGeoLoc Форсировать указанную геолокацию для запроса индекса.
  */
case class MScSwitchCtx(
                         indexQsArgs      : MScIndexArgs,
                         focusedAdId      : Option[String]    = None,
                         demandLocTest    : Boolean           = false,
                         forceGeoLoc      : Option[MGeoLoc]   = None,
                       )

