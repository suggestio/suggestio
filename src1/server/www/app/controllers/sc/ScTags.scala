package controllers.sc

import io.suggest.model.n2.node.IMNodes
import io.suggest.model.n2.tag.MTagSearchResp
import io.suggest.stat.m.{MAction, MActionTypes, MComponents}
import io.suggest.util.logs.IMacroLogs
import models.mctx.Context
import models.msc.tag.MScTagsSearchQs
import play.api.libs.json.Json
import util.acl.IMaybeAuth
import util.geo.IGeoIpUtilDi
import util.showcase.IScTagsUtilDi
import util.stat.IStatUtil
import views.html.sc.search._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.09.15 16:37
 * Description: Аддон для экшенов, связанных с тегами, в выдаче.
 */
trait ScTags
  extends ScController
  with IMaybeAuth
  with IMNodes
  with IScTagsUtilDi
  with IGeoIpUtilDi
  with IStatUtil
  with IMacroLogs
{

  import mCommonDi._

  /**
    * Поиск тегов по названиям.
    *
    * @param qs Аргументы поиска из URL query string.
    * @return Рендер куска списка тегов, который раньше был списком узлов.
    */
  def tagsSearch(qs: MScTagsSearchQs) = maybeAuth().async { implicit request =>

    // Результат геолокации понадобиться как минимум для статистики. Запускаем в фоне.
    val geoIpResOptFut = geoIpUtil.findIpCached(
      geoIpUtil.fixedRemoteAddrFromRequest.remoteAddr
    )

    // Если переданные данные геолокации пусты, то запихать в них данные из geoip.
    val mGeoLocOptFut = geoIpUtil.geoLocOrFromIp( qs.locEnv.geoLocOpt )( geoIpResOptFut )

    val tagsFoundFut = for {
      mGeoLocOpt2 <- mGeoLocOptFut
      msearch     <- scTagsUtil.qs2NodesSearch(qs, mGeoLocOpt2)
      found       <- mNodes.dynSearch(msearch)
    } yield {
      found
    }

    // Асинхронный HTTP-ответ...
    val resFut = for {
      found       <- tagsFoundFut
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

    val _ctx = implicitly[Context]

    // Готовим статистику.
    val userSaOptFut = statUtil.userSaOptFutFromRequest()
    for {
      found         <- tagsFoundFut
      _userSaOpt    <- userSaOptFut
      geoIpResOpt   <- geoIpResOptFut
    } {
      val sstat = new statUtil.Stat2 {
        override def statActions: List[MAction] = {
          val acc0: List[MAction] = Nil

          // Добавить offset, если задан
          val acc1 = qs.offsetOpt.fold(acc0) { offset =>
            val limAction = MAction(
              actions = MActionTypes.SearchOffset :: Nil,
              count   = offset :: Nil
            )
            limAction :: acc0
          }

          // Добавить limit, если задан
          val acc2 = qs.limitOpt.fold(acc1) { limit =>
            val limAction = MAction(
              actions = MActionTypes.SearchLimit :: Nil,
              count   = limit :: Nil
            )
            limAction :: acc1
          }

          // Добавить tags-экшен в начало списка экшенов.
          val tAction = MAction(
            actions   = Seq(MActionTypes.ScTags),
            nodeId    = found.flatMap(_.id),
            nodeName  = found.flatMap(_.guessDisplayName),
            count     = Seq(found.size),
            // Поисковый запрос тегов, если есть.
            textNi    = qs.tagsQuery.toSeq
          )
          tAction :: acc2
        }
        override def userSaOpt    = _userSaOpt
        override def ctx          = _ctx
        override def locEnvOpt    = Some(qs.locEnv)
        override def geoIpLoc     = geoIpResOpt
        override def scComponents = MComponents.Tags :: super.scComponents
      }
      statUtil.saveStat(sstat)
        .onFailure { case ex: Throwable =>
          LOGGER.error(s"tagsSearch($qs): Failed to save tags stats", ex)
        }
    }

    // Возвращаем исходный асинхронный ответ.
    resFut
  }

}
