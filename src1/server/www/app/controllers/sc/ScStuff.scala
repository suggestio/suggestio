package controllers.sc

import io.suggest.adn.MAdnRights
import io.suggest.common.empty.OptionUtil
import io.suggest.es.model.EsModel
import io.suggest.n2.node.{MNodeTypes, MNodes}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.sc.index.MScIndexes
import io.suggest.streams.StreamsUtil
import io.suggest.util.logs.MacroLogsImpl
import play.api.i18n.{Lang, Messages, MessagesImpl}

import javax.inject.Inject
import play.api.libs.json.Json
import util.acl.{BruteForceProtect, MaybeAuth, SioControllerApi}
import util.adv.geo.AdvGeoRcvrsUtil
import util.i18n.JsMessagesUtil

import scala.util.Try

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
  private lazy val jsMessagesUtil = injector.instanceOf[JsMessagesUtil]


  /** Обработка списка узлов, чтобы был актуальный логотип, название, цвета.
    * Исходный список находится внутри тела реквеста в виде JSON.
    *
    * Используется, чтобы оживить сохранённый ранее список недавних узлов:
    * пока данных хранились, они могли измениться: лого поменялся, название узла и т.д.
    *
    * @return 200 OK chunked + заполненный спискок узлов в случайном порядке.
    */
  def fillNodesList() = bruteForceProtect {
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


  /** Return play messages for current language in pure JSON format.
    *
    * @param langOpt Language code, or session default.
    * @return JSON messages for lang choosen.
    *         404, if language not found.
    */
  def scMessagesJson(langOpt: Option[String]) = {
    // TODO Maybe to somehow compile messages string into assets? So everything will be rendered and compressed once in compile-time.
    maybeAuth().async { implicit request =>
      lazy val logPrefix = s"scMessagesJson(${langOpt.orNull}):"

      langOpt
        .fold [Option[Messages]] { Some(request2Messages) } { langCode =>
          Lang
            .get( langCode )
            .map { lang =>
              MessagesImpl( lang, messagesApi )
            }
        }
        .flatMap { messages =>
          val tryRes = Try( jsMessagesUtil.sc.messagesString( messages ) )

          if (LOGGER.underlying.isDebugEnabled)
            for (ex <- tryRes.failed)
              LOGGER.debug( s"$logPrefix Failed to generate messages.json", ex )

          tryRes.toOption
        }
        // messagesString() returns "{}", if no data found.
        .filter(_.length > 3)
        .fold {
          LOGGER.trace(s"$logPrefix Not found messages for lang, 404")
          errorHandler.onClientError( request, NOT_FOUND )
        } { jsonStr =>
          Ok( jsonStr )
            .as( JSON )
            // TODO Implement aggressive caching (with cache key inside URL). See previous TODO for possible implementation.
            .cacheControl( 3600 )
            .withHeaders(
              CONTENT_DISPOSITION -> s"""inline; filename="messages${langOpt.fold("")("-" + _)}.json"""",
            )
        }
    }
  }

}
