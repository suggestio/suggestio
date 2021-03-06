package controllers.sc

import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import io.suggest.maps.nodes.{MGeoNodePropsShapes, MGeoNodesResp}
import io.suggest.n2.node.MNode
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.sc.index.MSc3IndexResp
import io.suggest.sc.sc3.{MSc3RespAction, MScQs, MScRespActionTypes}
import io.suggest.sc.{MScApiVsn, MScApiVsns}
import io.suggest.stat.m.{MAction, MActionTypes, MComponents}
import io.suggest.util.logs.MacroLogsImpl
import japgolly.univeq._
import models.req.IReq
import util.showcase.MRadioBeaconsSearchCtx

import javax.inject.Inject
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.09.15 16:37
 * Description: Аддон для функция выдачи, связанных с поиском узлов, тегов и т.д. в выдаче.
 */
final class ScSearch @Inject()(
                                val scCtlUtil: ScCtlUtil,
                              )
  extends MacroLogsImpl
{

  import scCtlUtil._
  import scCtlUtil.sioControllerApi.ec
  import esModel.api._

  /** Общая логика обработки tags-запросов выдачи. */
  protected trait ScSearchLogic extends scCtlUtil.LazyContext with IRespActionFut { logic =>

    lazy val logPrefix = s"${getClass.getSimpleName}#${System.currentTimeMillis()}:"

    def _qs: MScQs
    def _radioSearchCtxFut: Future[MRadioBeaconsSearchCtx]
    def _geoIpInfo: ScCtlUtil#GeoIpInfo

    def nodeSearchOptFut: Future[Option[MNodeSearch]] = {
      (for {
        radioSearchCtx <- _radioSearchCtxFut
        mGeoLocs2 = _qs.common.locEnv.geoLoc
        if (radioSearchCtx.uidsClear.nonEmpty || mGeoLocs2.nonEmpty)
        mNodeSearch <- {
          LOGGER.trace(s"$logPrefix geoLoc = ${mGeoLocs2.length} geoLocs and ${radioSearchCtx.uidsClear.size} radio-beacons")
          scSearchUtil.qs2NodesSearch( _qs, mGeoLocs2, radioSearchCtx )
        }
      } yield {
        Some( mNodeSearch )
      })
        .recover { case _: NoSuchElementException =>
          LOGGER.trace(s"$logPrefix No search coords available: no qs.geolocations and no qs.beacons at all.")
          None
        }
    }

    /** Сборка sink'а для сохранения найденных узлов в статистику. */
    def saveScStatSink: Sink[(MNode, MSc3IndexResp), Future[_]] = {
      Flow[(MNode, MSc3IndexResp)]
        // Если результатов слишком много, то нет смысла их всех сохранять в статистику.
        .take( 10 )
        // Накопить данные с узлов, для отправки в статистику:
        .toMat {
          // TODO Opt Использовать Set.builder?
          Sink.fold {
            val ss = Set.empty[String]
            (ss, ss)
          } { case ((ids0, names0), (mnode, _)) =>
            val ids2 = mnode.id.fold(ids0)(ids0 + _)
            val names2 = mnode.guessDisplayName.fold(names0)(names0 + _)
            (ids2, names2)
          }
        }( Keep.right )
        // Собранные данные по узлам сохранить в статистику:
        .mapMaterializedValue { idsAndNamesFut =>
          val _userSaOptFut = statUtil.userSaOptFutFromRequest()
          val _geoIpResOptFut = _geoIpInfo.geoIpResOptFut
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
    def nodeInfosSrc: Source[MGeoNodePropsShapes, _] = {
      val srcFut = for {
        msearchOpt <- nodeSearchOptFut
      } yield {
        msearchOpt.fold [Source[MGeoNodePropsShapes, _]] ( Source.empty ) { msearch =>
          // Организовать чтение найденных узлов из БД:
          val src0 = advGeoRcvrsUtil
            .nodesAdvGeoPropsSrc(
              // Нельзя сорсить напрямую через search scroll, т.к. это нарушает порядок сортировки. Имитируем Source через dynSearch:
              mNodes.dynSearchSource( msearch ),
            )
            // Ответвление: Данные для статистики - материализовать, mat-итог запихать в статистику:
            .alsoTo( saveScStatSink )

          advGeoRcvrsUtil
            .withNodeLocShapes( src0 )
            .map { case (_, advNodePropsShapes) =>
              // TODO Не рендерить гео-данные для тегов!
              advNodePropsShapes
            }
        }
      }
      Source.futureSource( srcFut )
    }

  }
  object ScSearchLogic {
    def apply(scApiVsn: MScApiVsn): IScSearchLogicCompanion = {
      if (scApiVsn.majorVsn ==* MScApiVsns.ReactSjs3.majorVsn) {
        ScSearchLogicV3
      } else {
        throw new UnsupportedOperationException("Unknown API vsn: " + scApiVsn)
      }
    }
  }


  /** Интерфейс для объекта-компаньона реализаций ScTagsLogic. */
  protected trait IScSearchLogicCompanion {
    def apply(qs: MScQs,
              radioSearchCtxFut: Future[MRadioBeaconsSearchCtx],
              geoIpInfo: ScCtlUtil#GeoIpInfo,
             )(implicit request: IReq[_]): ScSearchLogic
  }


  /** Реализация поддержки Sc APIv3. */
  case class ScSearchLogicV3(override val _qs: MScQs,
                             override val _radioSearchCtxFut: Future[MRadioBeaconsSearchCtx],
                             override val _geoIpInfo: ScCtlUtil#GeoIpInfo,
                            )(override implicit val _request: IReq[_])
    extends ScSearchLogic
  {

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
          acType = MScRespActionTypes.SearchNodes,
          search = Some(
            MGeoNodesResp(
              nodes = nodesFound
            )
          )
        )
      }
    }

  }
  object ScSearchLogicV3 extends IScSearchLogicCompanion

}
