package controllers.sc

import models.MNode
import models.mtag.{MTagSearchResp, MTagSearch}
import play.api.libs.json.Json
import util.acl.MaybeAuth
import views.html.sc.tags._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.09.15 16:37
 * Description: Аддон для экшенов, связанных с тегами, в выдаче.
 */
trait ScTags
  extends ScController
  with MaybeAuth
{

  import mCommonDi._

  /**
   * Поиск тегов по названиям.
   * @param mts Аргументы поиска, сформированные на клиенте.
   * @return Рендер куска списка тегов, который раньше был списком узлов.
   */
  def tagsSearch(mts: MTagSearch) = MaybeAuth.async { implicit request =>
    val foundFut = MNode.dynSearch(mts)
    for {
      found <- foundFut
    } yield {
      val resp = MTagSearchResp(
        rendered    = if (found.nonEmpty)  Some( _listElsTpl(found) )  else  None,
        foundCount  = found.size
      )
      Ok( Json.toJson(resp) )
    }
  }

}
