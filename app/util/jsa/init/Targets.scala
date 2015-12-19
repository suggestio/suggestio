package util.jsa.init

import models.jsm.init.{MTargets, MTarget}
import models.req.ISioReqHdr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.04.15 13:54
 * Description: Поддержка целей js-инициализации в контроллерах.
 */

/** Интерфейс для доступа к необходимым целям. */
trait ITargets {
  /** Вернуть список целей инициализации js. */
  def jsiTgs(req: ISioReqHdr): List[MTarget]
}



/**
 * Аддон для контроллера, чтобы можно было глобально объявлять цели для js-инициализации.
 * на уровне контроллера:
 *   override protected def _jsInitTargets0: List[MTarget] = {
 *     MTargets.SomeTarget :: super._jsInitTargets0
 *   }
 */
trait ITargetsEmpty extends ITargets {
  override def jsiTgs(req: ISioReqHdr): List[MTarget] = Nil
}



/** Поддержка добавления таргета для отображения flashing-уведомлений, если они есть. */
trait FlashingJsInit extends ITargets {
  override def jsiTgs(req: ISioReqHdr): List[MTarget] = {
    var tgs = super.jsiTgs(req)
    // Добавить flashing-цель, если есть flashing-уведомления в запросе.
    if (!req.flash.isEmpty)
      tgs ::= MTargets.Flashing
    tgs
  }
}


/** Аддон для контроллеров с дефолтовым набором трейтов для компиляции списка js init целей. */
trait CtlJsInitT
  extends ITargetsEmpty
  with FlashingJsInit
