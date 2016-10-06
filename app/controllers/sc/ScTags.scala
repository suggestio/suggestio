package controllers.sc

import io.suggest.model.n2.node.IMNodes
import io.suggest.model.n2.tag.MTagSearchResp
import models.msc.tag.MScTagsSearchQs
import play.api.libs.json.Json
import util.acl.MaybeAuth
import util.geo.IGeoIpUtilDi
import util.showcase.IScTagsUtilDi
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
  with IScTagsUtilDi
  with IGeoIpUtilDi
{

  import mCommonDi._

  /**
    * Поиск тегов по названиям.
    *
    * @param qs Аргументы поиска из URL query string.
    * @return Рендер куска списка тегов, который раньше был списком узлов.
    */
  def tagsSearch(qs: MScTagsSearchQs) = MaybeAuth().async { implicit request =>

    // Если переданные данные геолокации пусты, то запихать в них данные из geoip.
    val mGeoLocOptFut = geoIpUtil.geoLocOrFromIp( qs.locEnv.geoLocOpt ) {
      geoIpUtil.findIpCached(
        geoIpUtil.fixedRemoteAddrFromRequest.remoteAddr
      )
    }

    for {
      mGeoLocOpt2 <- mGeoLocOptFut
      msearch     <- scTagsUtil.qs2NodesSearch(qs, mGeoLocOpt2)
      found       <- mNodes.dynSearch(msearch)
    } yield {

      // Запустить рендер, если найден хотя бы один тег.
      val htmlOpt = if (found.nonEmpty) {
        val html = htmlCompressUtil.html2str4json(
          _tagsListTpl(found)
        )
        Some( html )
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
