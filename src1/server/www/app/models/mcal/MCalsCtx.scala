package models.mcal

import de.jollyday.HolidayManager
import io.suggest.n2.node.MNode

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.12.15 16:41
 * Description: bill v1 контекст с календарями.
 */
object MCalsCtx {
  def empty = MCalsCtx( Map.empty )
}

/** Дефолтовая реализация модели контекстов календарей [[MCalsCtx]]. */
final case class MCalsCtx(
                           calsMap: Map[String, MCalCtx]
                         ) {
  override def toString =
    s"${getClass.getSimpleName}(${calsMap.size}cals)"
}


final case class MCalCtx(
                          calId  : String,
                          mcal   : MNode,
                          mgr    : HolidayManager,
                        )
