package io.suggest.sc.m.hdr

import io.suggest.spa.DAction

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.07.17 20:57
  * Description: Diode-экшены для событий в заголовке выдачи.
  */
sealed trait IScHdrAction extends DAction


/** Клик по логотипу или названию узла. */
case object HLogoClick extends IScHdrAction
