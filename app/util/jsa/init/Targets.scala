package util.jsa.init

import models.Context
import models.jsm.init.{MTargets, MTarget}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.04.15 13:54
 * Description: Поддержка целей js-инициализации в контроллерах.
 */

/** Интерфейс для доступа к необходимым целям. */
trait ITargets {

  /** Вернуть список целей инициализации js. */
  def getJsInitTargets(ctx: Context): List[MTarget]

}



/**
 * Аддон для контроллера, чтобы можно было глобально объявлять цели для js-инициализации.
 * на уровне контроллера:
 *   override protected def _jsInitTargets0: List[MTarget] = {
 *     MTargets.SomeTarget :: super._jsInitTargets0
 *   }
 */
trait JsInitTargetsDfltT extends ITargets {

  /** Постоянный список целей в рамках контроллера или иной реализации. */
  protected def _jsInitTargets0: List[MTarget] = Nil
  override def getJsInitTargets(ctx: Context): List[MTarget] = _jsInitTargets0
}

object JsInitTargetsDflt extends JsInitTargetsDfltT



/** Поддержка добавления таргета для отображения flashing-уведомлений, если они есть. */
trait WithFlashingJsInit extends ITargets {
  abstract override def getJsInitTargets(ctx: Context): List[MTarget] = {
    var tgs = super.getJsInitTargets(ctx)
    // Добавить flashing-цель, если есть flashing-уведомления в запросе.
    if (!ctx.request.flash.isEmpty)
      tgs ::= MTargets.Flashing
    tgs
  }
}


/**
 * Доставать из экшена контроллера extra js init.
 *   implicit val s = Seq(MTargets.SomeTarget)
 *   Ok(indextTpl())
 */
trait WithExtraJsInit extends ITargets {
  /** Вернуть список целей инициализации js. */
  abstract override def getJsInitTargets(ctx: Context): List[MTarget] = {
    var tgs = super.getJsInitTargets(ctx)
    val tgsExtra = ctx.jsInitTargetsExtra
    if (tgsExtra.nonEmpty)
      tgs ++= tgsExtra
    tgs
  }
}


/** Аддон для контроллеров с дефолтовым набором трейтов для компиляции списка js init целей. */
trait CtlJsInitT
  extends JsInitTargetsDfltT
  with WithFlashingJsInit
  with WithExtraJsInit
