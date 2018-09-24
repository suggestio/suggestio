package util.jsa.init

import io.suggest.init.routed.{MJsInitTarget, MJsInitTargets}
import models.req.IReqHdr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.04.15 13:54
 * Description: Поддержка целей js-инициализации в контроллерах.
 */

/** Интерфейс для доступа к необходимым целям. */
trait ITargets {
  /** Вернуть список целей инициализации js. */
  def jsiTgs(req: IReqHdr): List[MJsInitTarget]
}



/**
 * Аддон для контроллера, чтобы можно было глобально объявлять цели для js-инициализации.
 * на уровне контроллера:
 *   override protected def _jsInitTargets0: List[MTarget] = {
 *     MTargets.SomeTarget :: super._jsInitTargets0
 *   }
 */
trait ITargetsEmpty extends ITargets {
  override def jsiTgs(req: IReqHdr): List[MJsInitTarget] = Nil
}



/** Поддержка добавления таргета для отображения flashing-уведомлений, если они есть. */
trait FlashingJsInit extends ITargetsEmpty {
  override def jsiTgs(req: IReqHdr): List[MJsInitTarget] = {
    var tgs = super.jsiTgs(req)
    // Добавить flashing-цель, если есть flashing-уведомления в запросе.
    if (!req.flash.isEmpty)
      tgs ::= MJsInitTargets.Flashing
    tgs
  }
}


/** Аддон для контроллеров с дефолтовым набором трейтов для компиляции списка js init целей. */
trait CtlJsInitT
  extends FlashingJsInit
