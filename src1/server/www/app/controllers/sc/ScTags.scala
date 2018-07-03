package controllers.sc

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import io.suggest.common.empty.OptionUtil
import io.suggest.maps.nodes.MAdvGeoMapNodeProps
import io.suggest.model.n2.node.search.MNodeSearch
import io.suggest.model.n2.node.{IMNodes, MNode}
import io.suggest.sc.{MScApiVsn, MScApiVsns}
import io.suggest.sc.sc3.{MSc3Resp, MSc3RespAction, MScQs, MScRespActionTypes}
import io.suggest.sc.search.{MSc3NodeInfo, MSc3NodeSearchResp}
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
import util.adv.geo.IAdvGeoRcvrsUtilDi

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
  with IAdvGeoRcvrsUtilDi
{

  import mCommonDi._


  /** Общая логика обработки tags-запросов выдачи. */
  trait ScTagsLogicBase extends LazyContext with IRespActionFut { logic =>

    lazy val logPrefix = s"${getClass.getSimpleName}#${System.currentTimeMillis()}:"

    def _qs: MScQs

    lazy val geoIpResOptFut = geoIpUtil.findIpCached(
      geoIpUtil.fixedRemoteAddrFromRequest.remoteAddr
    )

    def mGeoLocOptFut = OptionUtil.maybeFut( _qs.search.rcvrId.isEmpty ) {
      geoIpUtil.geoLocOrFromIp( _qs.common.locEnv.geoLocOpt )( geoIpResOptFut )
    }

    def nodesSearch: Future[MNodeSearch] = {
      mGeoLocOptFut.flatMap { mGeoLocOpt2 =>
        val msearch = scTagsUtil.qs2NodesSearch(_qs, mGeoLocOpt2)
        LOGGER.trace(s"$logPrefix geoLoc2 = ${mGeoLocOpt2.orNull}\n msearch = $msearch")
        msearch
      }
    }

    /** Сборка sink'а для сохранения найденных узлов в статистику. */
    def saveScStatSink: Sink[(MNode, MAdvGeoMapNodeProps), Future[_]] = {
      Flow[(MNode, MAdvGeoMapNodeProps)]
        // Накопить данные с узлов, для отправки в статистику:
        .toMat(
          Sink.fold( (Set.empty[String], Set.empty[String]) ) { case ((ids0, names0), (mnode, _)) =>
            val ids2 = mnode.id.fold(ids0)(ids0 + _)
            val names2 = mnode.guessDisplayName.fold(names0)(names0 + _)
            (ids2, names2)
          }
        )( Keep.right )
        // Собранные данные по узлам сохранить в статистику:
        .mapMaterializedValue { idsAndNamesFut =>
          val _userSaOptFut = statUtil.userSaOptFutFromRequest()
          val _geoIpResOptFut = geoIpResOptFut
          for {
            _userSaOpt    <- _userSaOptFut
            geoIpResOpt   <- _geoIpResOptFut
            (nodeIds, nodeNames)  <- idsAndNamesFut
            // Собрать инстанс данных для статистики:
            stat2 = new statUtil.Stat2 {
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
                  actions   = MActionTypes.ScTags :: Nil,
                  nodeId    = nodeIds.toSeq,
                  nodeName  = nodeNames.toSeq,
                  count     = nodeIds.size :: Nil,
                  // Поисковый запрос тегов, если есть.
                  textNi    = _qs.search.textQuery.toSeq
                )
                tAction :: acc2
              }
              override def ctx = logic.ctx
              override def userSaOpt    = _userSaOpt
              override def locEnvOpt    = Some( _qs.common.locEnv )
              override def geoIpLoc     = geoIpResOpt
              override def components   = MComponents.Tags :: super.components
            }
            // Выполнить сохранение статистики:
            _ <- statUtil.saveStat( stat2 )
          } yield {
            None
          }
        }
    }

    /** Реактивный поиск и json-рендер тегов и узлов, вместо старого обычного поиска тегов. */
    def nodeInfosSrc: Source[MSc3NodeInfo, Future[NotUsed]] = {
      val srcFut = for {
        msearch <- nodesSearch
      } yield {
        advGeoRcvrsUtil
          .nodesAdvGeoPropsSrc(msearch, wcAsLogo = false)
          // Ответвление: Данные для статистики - материализовать, mat-итог запихать в статистику:
          .alsoTo( saveScStatSink )
          // Далее, надо рендерить в JSON для ответа сервера:
          .map { case (mnode, advNodeInfo) =>
            MSc3NodeInfo(
              props     = advNodeInfo,
              nodeType  = mnode.common.ntype
            )
          }
          /*
          // TODO Для chunked-выхлопа можно задействовать этот код в будущем. Пока что ScUniApi не поддерживает chunked-ответ, поэтому не нужно.
          .jsValuesToJsonArrayByteStrings
          // Надо запихать в JSON-ответ в формате sc3-resp-action.
          .jsonEmbedIntoEmptyArrayIn(
            MSc3Resp(
              respActions = MSc3RespAction(
                acType = MScRespActionTypes.SearchRes,
                search = Some(
                  MSc3NodeSearchResp(
                    // Сюда будет отрендерен весь предшествующий json-array:
                    results = Nil
                  )
                )
              ) :: Nil
            )
          )
          */
      }
      Source.fromFutureSource( srcFut )
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
      // Запустить фоновые задачи:
      val nodesFoundFut = nodeInfosSrc
        // Статистика собирается уже внутри src сама.
        .toMat( Sink.seq )(Keep.right)
        .run()

      for {
        nodesFound <- nodesFoundFut
      } yield {
        LOGGER.trace(s"$logPrefix Found ${nodesFound.size} nodes")
        MSc3RespAction(
          acType = MScRespActionTypes.SearchRes,
          search = Some(
            MSc3NodeSearchResp(
              results = nodesFound
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
