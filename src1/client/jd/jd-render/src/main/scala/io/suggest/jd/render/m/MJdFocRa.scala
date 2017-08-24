package io.suggest.jd.render.m

import io.suggest.jd.tags.{JsonDocument, Strip}
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.WarnMsgs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.17 15:17
  * Description: Интерфейс модели данных для рендера документа.
  */
object MJdFocRa extends Log {

  def firstBlockStripOrEmpty(tpl: JsonDocument): Strip = {
    tpl.firstBlockStrip
      .getOrElse {
        LOG.warn(WarnMsgs.UNEXPECTED_EMPTY_DOCUMENT, msg = tpl)
        Strip()()
      }
  }

}


/** Данные по документу для рендера всей карточки в открытом (focused) виде. */
case class MJdFocRa(
                     template  : JsonDocument,
                     common    : MJdCommonRa
                   )

