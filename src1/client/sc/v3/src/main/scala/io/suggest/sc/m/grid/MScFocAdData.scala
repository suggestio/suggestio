package io.suggest.sc.m.grid

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.18 22:18
  * Description: Контейнер данных для рендера одной focused-карточки.
  */
case class MScFocAdData(
                         blkData  : MBlkRenderData,
                         canEdit  : Boolean
                       )
