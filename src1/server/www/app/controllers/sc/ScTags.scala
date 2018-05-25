package controllers.sc

import io.suggest.common.empty.OptionUtil
import io.suggest.model.n2.node.IMNodes
import io.suggest.sc.{MScApiVsn, MScApiVsns}
import io.suggest.sc.sc3.{MSc3Resp, MSc3RespAction, MScQs, MScRespActionTypes}
import io.suggest.sc.search.{MSc3Tag, MSc3TagsResp}
import io.suggest.stat.m.{MAction, MActionTypes, MComponents}
import io.suggest.util.logs.IMacroLogs
import models.req.IReq
import play.api.libs.json.Json
import play.api.mvc.Result
import util.acl.IMaybeAuth
import util.geo.IGeoIpUtilDi
import util.showcase.IScTagsUtilDi
import util.stat.IStatUtil
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
  trait ScTagsLogicBase extends LogicCommonT with IRespActionFut {

    lazy val logPrefix = s"${getClass.getSimpleName}#${System.currentTimeMillis()}:"

    def _qs: MScQs

    lazy val geoIpResOptFut = geoIpUtil.findIpCached(
      geoIpUtil.fixedRemoteAddrFromRequest.remoteAddr
    )

    lazy val mGeoLocOptFut = OptionUtil.maybeFut( _qs.search.rcvrId.isEmpty ) {
      geoIpUtil.geoLocOrFromIp( _qs.common.locEnv.geoLocOpt )( geoIpResOptFut )
    }

    lazy val tagsFoundFut = {
      for {
        mGeoLocOpt2 <- mGeoLocOptFut
        msearch     <- scTagsUtil.qs2NodesSearch(_qs, mGeoLocOpt2)
        found       <- mNodes.dynSearch(msearch)
      } yield {
        LOGGER.trace(s"$logPrefix tagsFound: ${found.length} tags\n geoLoc2=${mGeoLocOpt2.orNull}\n msearch=$msearch")
        found
      }
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
            val acc1 = _qs.search.offset.fold(acc0) { offset =>
              val limAction = MAction(
                actions = MActionTypes.SearchOffset :: Nil,
                count   = offset :: Nil
              )
              limAction :: acc0
            }

            // Добавить limit, если задан
            val acc2 = _qs.search.limit.fold(acc1) { limit =>
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
              textNi    = _qs.search.textQuery.toSeq
            )
            tAction :: acc2
          }
          override def userSaOpt    = _userSaOpt
          override def locEnvOpt    = Some( _qs.common.locEnv )
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
    def apply(qs: MScQs)(implicit request: IReq[_]): ScTagsHttpLogic
  }


  /** Реализация поддержки Sc APIv3. */
  case class ScTagsLogicV3(override val _qs: MScQs)
                          (override implicit val _request: IReq[_]) extends ScTagsHttpLogic {

    /** Сборка search-res-ответа без sc3Resp-обёртки. */
    override def respActionFut: Future[MSc3RespAction] = {
      // Запустить фоновые задачи.
      val _tagsFoundFut = tagsFoundFut

      // Запустить сбор статистики
      saveScStat()

      for {
        tags <- _tagsFoundFut
      } yield {
        LOGGER.trace(s"$logPrefix Found ${tags.size} tags")
        MSc3RespAction(
          acType = MScRespActionTypes.SearchRes,
          search = Some(
            MSc3TagsResp(
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
          )
        )
      }
    }

    override def execute(): Future[Result] = {
      for {
        sc3TagsRespAction <- respActionFut
      } yield {
        val respJson = Json.toJson(
          MSc3Resp(
            respActions = sc3TagsRespAction :: Nil
          )
        )
        Ok( respJson )
      }
    }

  }
  object ScTagsLogicV3 extends IScTagsHttpLogicCompanion


  /** Переключалка между различными логиками для разных версий Sc API. */
  def _apiVsn2logic(scApiVsn: MScApiVsn): IScTagsHttpLogicCompanion = {
    if (scApiVsn.majorVsn ==* MScApiVsns.ReactSjs3.majorVsn) {
      ScTagsLogicV3
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
  def tagsSearch(qs: MScQs) = maybeAuth().async { implicit request =>
    val logic = _apiVsn2logic( qs.common.apiVsn )
    logic(qs).execute()
  }

}
