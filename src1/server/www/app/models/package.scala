
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.02.14 17:47
 * Description: После переноса ряда моделей в sioutil, тут появились костыли.
 */

package object models {

  type NodeRightPanelLink   = NodeRightPanelLinks.T
  type BillingRightPanelLink= BillingRightPanelLinks.T
  type LkLeftPanelLink      = LkLeftPanelLinks.T


  /** Вызов на главную страницу. */
  def MAIN_PAGE_CALL        = controllers.routes.Sc.geoSite()


  type MNode                = io.suggest.model.n2.node.MNode

  type MEdge                = io.suggest.model.n2.edge.MEdge

}
