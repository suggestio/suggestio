package controllers.sc

import io.suggest.model.n2.node.IMNodes
import models.mtag.{MTagSearch, MTagSearchResp}
import play.api.libs.json.Json
import util.acl.MaybeAuth
import views.html.sc.search._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.09.15 16:37
 * Description: Аддон для экшенов, связанных с тегами, в выдаче.
 */
trait ScTags
  extends ScController
  with MaybeAuth
  with IMNodes
{

  import mCommonDi._

  /**
   * Поиск тегов по названиям.
    *
    * @param mts Аргументы поиска, сформированные на клиенте.
   * @return Рендер куска списка тегов, который раньше был списком узлов.
   */
  def tagsSearch(mts: MTagSearch) = MaybeAuth().async { implicit request =>
    val foundFut = mNodes.dynSearch(mts.toEsSearch)
    for {
      found <- foundFut
    } yield {
      val htmlOpt = if (found.nonEmpty) {
        val htmlRaw = _tagsListTpl(found)
        Some( htmlCompressUtil.html2str4json(htmlRaw) )
      } else {
        None
      }

      val resp = MTagSearchResp(
        rendered    = htmlOpt,
        foundCount  = found.size
      )
      Ok( Json.toJson(resp) )
    }
  }

}
