package io.suggest.sc.sjs.m.mtags

import io.suggest.sjs.common.fsm.IFsmMsg

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.10.16 18:46
  */

/** Описательная инфа по тегу. */
case class MTagInfo(
  nodeId  : String,
  face    : String
)

/** Сигнал о новом текущем теге. */
case class TagSelected(info: Option[MTagInfo])
  extends IFsmMsg
