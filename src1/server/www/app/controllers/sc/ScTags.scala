package controllers.sc

import io.suggest.model.n2.node.IMNodes
import io.suggest.model.n2.tag.MTagSearchResp
import io.suggest.sc.sc3.{MSc3Tag, MSc3TagsResp}
import io.suggest.stat.m.{MAction, MActionTypes, MComponents}
import io.suggest.util.logs.IMacroLogs
import models.msc.{MScApiVsn, MScApiVsns}
import models.msc.tag.MScTagsSearchQs
import models.req.IReq
import play.api.libs.json.Json
import play.api.mvc.Result
import util.acl.IMaybeAuth
import util.geo.IGeoIpUtilDi
import util.showcase.IScTagsUtilDi
import util.stat.IStatUtil
import views.html.sc.search._
import japgolly.univeq._

import scala.concurrent.Future

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


  /** Общая логика обработки tags-запросов выдачи. */
  trait ScTagsLogicBase extends LogicCommonT {

    def _qs: MScTagsSearchQs

    lazy val geoIpResOptFut = geoIpUtil.findIpCached(
      geoIpUtil.fixedRemoteAddrFromRequest.remoteAddr
    )

    lazy val mGeoLocOptFut = geoIpUtil.geoLocOrFromIp( _qs.locEnv.geoLocOpt )( geoIpResOptFut )

    lazy val tagsFoundFut = for {
      mGeoLocOpt2 <- mGeoLocOptFut
      msearch     <- scTagsUtil.qs2NodesSearch(_qs, mGeoLocOpt2)
      found       <- mNodes.dynSearch(msearch)
    } yield {
      found
    }

    /** Контекстно-зависимая сборка данных статистики. */
    override def scStat: Future[Stat2] = {
      val userSaOptFut = statUtil.userSaOptFutFromRequest()
      for {
        found         <- tagsFoundFut
        _userSaOpt    <- userSaOptFut
        geoIpResOpt   <- geoIpResOptFut
      } yield {
        new Stat2 {
          override def statActions: List[MAction] = {
            val acc0: List[MAction] = Nil

            // Добавить offset, если задан
            val acc1 = _qs.offsetOpt.fold(acc0) { offset =>
              val limAction = MAction(
                actions = MActionTypes.SearchOffset :: Nil,
                count   = offset :: Nil
              )
              limAction :: acc0
            }

            // Добавить limit, если задан
            val acc2 = _qs.limitOpt.fold(acc1) { limit =>
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
              textNi    = _qs.tagsQuery.toSeq
            )
            tAction :: acc2
          }
          override def userSaOpt    = _userSaOpt
          override def locEnvOpt    = Some( _qs.locEnv )
          override def geoIpLoc     = geoIpResOpt
          override def components   = MComponents.Tags :: super.components
        }
      }
    }

  }



  /** Логика для http-ответов для разных версий Sc API. */
  abstract class ScTagsHttpLogic extends ScTagsLogicBase {
    def execute(): Future[Result]
  }

  /** Интерфейс для объекта-компаньона реализаций [[ScTagsHttpLogic]]. */
  protected trait IScTagsHttpLogicCompanion {
    def apply(qs: MScTagsSearchQs)(implicit request: IReq[_]): ScTagsHttpLogic
  }


  /** ScTags для выдачи второго поколения. */
  case class ScTagsV2(override val _qs: MScTagsSearchQs)
                     (override implicit val _request: IReq[_]) extends ScTagsHttpLogic {

    override def execute(): Future[Result] = {
      // Запустить фоновые задачи.
      val _tagsFoundFut = tagsFoundFut

      // Запустить сбор статистики.
      saveScStat()

      // Сформировать ответ, когда всё будет готово.
      for {
        found       <- _tagsFoundFut
      } yield {
        // Запустить рендер, если найден хотя бы один тег.
        val htmlOpt = if (found.nonEmpty) {
          val html = htmlCompressUtil.html2str4json(
            _tagsListTpl(found)(ctx)
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
  object ScTagsV2 extends IScTagsHttpLogicCompanion


  /** Реализация поддержки Sc APIv3. */
  case class ScTagsV3(override val _qs: MScTagsSearchQs)
                     (override implicit val _request: IReq[_]) extends ScTagsHttpLogic {

    override def execute(): Future[Result] = {
      // Запустить фоновые задачи.
      val _tagsFoundFut = tagsFoundFut

      // Запустить сбор статистики
      saveScStat()

      for {
        tags <- _tagsFoundFut
      } yield {
        val resp = MSc3TagsResp(
          tags = {
            val iter = for {
              tagNode <- tags.iterator
              name    <- tagNode.guessDisplayName.iterator
              nodeId  <- tagNode.id.iterator
            } yield {
              MSc3Tag(
                name   = name,
                nodeId = nodeId
              )
            }
            iter.toSeq
          }
        )
        val respJson = Json.toJson( resp )
        Ok( respJson )
      }
    }

  }
  object ScTagsV3 extends IScTagsHttpLogicCompanion


  /** Переключалка между различными логиками для разных версий Sc API. */
  def _apiVsn2logic(scApiVsn: MScApiVsn): IScTagsHttpLogicCompanion = {
    if (scApiVsn.majorVsn ==* MScApiVsns.Sjs1.majorVsn) {
      ScTagsV2
    } else if (scApiVsn.majorVsn ==* MScApiVsns.ReactSjs3.majorVsn) {
      ScTagsV3
    } else {
      throw new UnsupportedOperationException("Unknown API vsn: " + scApiVsn)
    }
  }


  /**
    * Поиск тегов по названиям.
    *
    * @param qs Аргументы поиска из URL query string.
    * @return Рендер куска списка тегов, который раньше был списком узлов.
    */
  def tagsSearch(qs: MScTagsSearchQs) = maybeAuth().async { implicit request =>
    val logic = _apiVsn2logic( qs.apiVsn )
    logic(qs).execute()
  }

}
