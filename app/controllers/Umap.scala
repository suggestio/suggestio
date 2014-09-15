package controllers

import util.PlayMacroLogsImpl
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.acl.IsSuperuser
import views.html.umap._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.09.14 12:09
 * Description: Контроллер для umap-backend'ов.
 */
object Umap extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  /** Рендер карты с внутренней географией узлов. */
  def getAdnNodesMap = IsSuperuser { implicit request =>
    Ok(mapBaseTpl())
  }

  def saveMapSettingsSubmit = IsSuperuser { implicit request =>
    Ok("STUB")
  }

  def saveMapDataLayers = IsSuperuser { implicit request =>
    Ok("STUB")
  }

}
