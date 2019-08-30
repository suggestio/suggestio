package io.suggest.sc.m.grid

import io.suggest.jd.render.m.MJdDataJs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.18 22:18
  * Description: Контейнер данных для рендера одной focused-карточки.
  *
  * @param userFoc User-focused?
  *                true - Фокусировка по просьбе самого юзера.
  *                false - Юзер не фокусировал эту карточку, она пришла открытой по какой-то причине.
  */
case class MScFocAdData(
                         blkData  : MJdDataJs,
                         canEdit  : Boolean,
                         userFoc  : Boolean
                       )
