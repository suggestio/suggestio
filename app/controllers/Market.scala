package controllers

import io.suggest.util.MacroLogsImpl
import util.acl._
import views.html.market.showcase._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.14 11:37
 * Description: Выдача sio market
 */

object Market extends SioController with MacroLogsImpl {

  def index = MaybeAuth { implicit request =>
    Ok(indexTpl())
  }

}
