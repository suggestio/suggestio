import controllers.routes
import models.usr.EmailPwConfirmInfo
import play.api.data.Form

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
  def MAIN_PAGE_CALL        = routes.Sc.geoSite()


  /** Тип формы для регистрации по email (шаг 1 - указание email). */
  type EmailPwRegReqForm_t  = Form[String]

  /** Тип формы для восстановления пароля. */
  type EmailPwRecoverForm_t = Form[String]

  /** Тип формы сброса забытого пароля. */
  type PwResetForm_t        = Form[String]

  /** Тип формы для второго шагоа регистрации по email: заполнение данных о себе. */
  type EmailPwConfirmForm_t = Form[EmailPwConfirmInfo]

  /** Тип формы для регистрации через внешнего провайдера. */
  type ExtRegConfirmForm_t  = Form[String]

  /** Тип формы создания нового узла-магазина силами юзера. */
  type UsrCreateNodeForm_t  = Form[String]

  type ContractForm_t       = Form[io.suggest.mbill2.m.contract.MContract]

  type MNode                = io.suggest.model.n2.node.MNode

  type MEdge                = io.suggest.model.n2.edge.MEdge

}
