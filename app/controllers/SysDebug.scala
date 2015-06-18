package controllers

import com.google.inject.Inject
import play.api.i18n.MessagesApi
import util.acl.IsSuperuser
import util.health.AdnGeoParentsHealth
import views.html.sys1.debug._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.15 16:16
 * Description: Sys-контроллер для отладки.
 */
class SysDebug @Inject() (
  override val messagesApi: MessagesApi
) extends SioController {

  /** Экшен для отображения индексной страницы. */
  def index = IsSuperuser { implicit request =>
    Ok(indexTpl())
  }

  /**
   * Запуск теста geo-связности geo-узлов через geo.parents-поля.
   * @return 200 Ок со страницей-отчетом.
   */
  def testNodesAllGeoParents = IsSuperuser.async { implicit request =>
    // Организуем тестирование
    val testResultsFut = AdnGeoParentsHealth.testAll()

    // Запустить рендер, когда результаты тестирования будут готовы.
    testResultsFut.map { testResults =>
      val render = geo.parent.resultsTpl(testResults)
      Ok(render)
    }
  }

}
