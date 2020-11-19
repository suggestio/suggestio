package controllers.sc

import controllers.SioControllerApi
import io.suggest.adn.{MAdnRight, MAdnRights}
import io.suggest.common.empty.OptionUtil
import io.suggest.es.model.EsModel
import io.suggest.n2.node.{MNodeType, MNodeTypes, MNodes}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.sc.index.MScIndexes
import io.suggest.streams.StreamsUtil
import io.suggest.util.logs.MacroLogsImpl
import javax.inject.Inject
import play.api.libs.json.Json
import util.acl.{BruteForceProtect, MaybeAuth}
import util.adv.geo.AdvGeoRcvrsUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.2020 17:30
  * Description: Всякое API для выдачи, которое пока недостойно отдельных sc-контроллеров.
  */
final class ScStuff @Inject()(
                               sioControllerApi   : SioControllerApi,
                             )
  extends MacroLogsImpl
{

  import sioControllerApi._
  import mCommonDi.current.injector
  import mCommonDi.{csrf, errorHandler}

  private lazy val maybeAuth = injector.instanceOf[MaybeAuth]
  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val advGeoRcvrsUtil = injector.instanceOf[AdvGeoRcvrsUtil]
  private lazy val streamsUtil = injector.instanceOf[StreamsUtil]
  private lazy val bruteForceProtect = injector.instanceOf[BruteForceProtect]


  /** Обработка списка узлов, чтобы был актуальный логотип, название, цвета.
    * Исходный список находится внутри тела реквеста в виде JSON.
    *
    * Используется, чтобы оживить сохранённый ранее список недавних узлов:
    * пока данных хранились, они могли измениться: лого поменялся, название узла и т.д.
    *
    * @return 200 OK chunked + заполненный спискок узлов в случайном порядке.
    */
  def fillNodesList = bruteForceProtect {
    csrf.Check {
      maybeAuth().async( parse.json[MScIndexes] ) { implicit request =>
        if (request.body.indexes.lengthIs >= 15) {
          errorHandler.onClientError( request, NOT_ACCEPTABLE, "Too many nodes in request" )

        } else {
          import esModel.api._
          import streamsUtil.Implicits._

          // Собираем id узлов:
          val nodeIds = request.body
            .indexes
            .iterator
            .flatMap { scInxInfo =>
              scInxInfo.indexResp.nodeId
                .orElse( scInxInfo.state.nodeId )
            }
            .toSet

          // TODO Заготовить эфемерные узлы, которые закодированы координатами без nodeId?
          LOGGER.trace(s"fillNodesList(): Body with ${request.body.indexes.length} items, found ${nodeIds.size} nodeIds: ${nodeIds.mkString(",")}")

          // Тут нельзя использовать multiGet, чтобы не сливались из базы произвольные элементы по их id/
          val searchSrc = mNodes.dynSearchSource {
            new MNodeSearch {
              override val withIds = nodeIds.toSeq
              override val nodeTypes = MNodeTypes.AdnNode :: Nil
              override def isEnabled = OptionUtil.SomeBool.someTrue
              // Не ясно, нужен ли тут limit, ведь withIds уже задан.
              override def limit = nodeIds.size
              override def testNode = OptionUtil.SomeBool.someFalse
              // Тут фильтрация для всякго невычищенного легаси в базе.
              // Потом это можно удалить вместе с моделью MAdnRigths, которая постепенно утратила актуальность.
              override def withAdnRights = MAdnRights.RECEIVER :: Nil
            }
          }

          val outSrc = advGeoRcvrsUtil
            .nodesAdvGeoPropsSrc(
              // Нельзя сорсить напрямую через search scroll, т.к. это нарушает порядок сортировки. Имитируем Source через dynSearch:
              searchSrc,
              wcAsLogo = false,
            )
            .map { case (_, inxInfo) =>
              Json.toJson( inxInfo )
            }
            .jsValuesToJsonArrayByteStrings

          Ok.chunked( outSrc, contentType = Some(JSON) )
        }
      }
    }
  }

}
