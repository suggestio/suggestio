package controllers

import java.util.NoSuchElementException

import _root_.util.showcase._
import controllers.sc._
import io.suggest.event.SNStaticSubscriberDummy
import util._
import util.acl._
import views.html.market.showcase._
import play.api.libs.json._
import play.api.libs.Jsonp
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import SiowebEsUtil.client
import scala.concurrent.Future
import play.api.Play, Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.07.14 14:07
 * Description: Контроллер выдачи sio-market.
 * 2014.nov.10: Из-за активного наращивания функционала был разрезан на части, расположенные в controllers.sc.*.
 */
object MarketShowcase extends SioController with PlayMacroLogsImpl with SNStaticSubscriberDummy
with ScSiteNode with ScSiteGeo with ScNodeInfo with ScIndexGeo with ScIndexNode with ScSyncSiteGeo
with ScAdsTile with ScFocusedAds
{

  import LOGGER._

  val JSONP_CB_FUN = "siomart.receive_response"

  /** Кеш ответа findNodes() на клиенте. Это существенно ускоряет навигацию. */
  val FIND_NODES_CACHE_SECONDS: Int = configuration.getInt("market.showcase.nodes.find.result.cache.seconds") getOrElse {
    if (Play.isProd)  120  else  10
  }



  /**
   * Отрендерить одну указанную карточку как веб-страницу.
   * @param adId id рекламной карточки.
   * @return 200 Ок с отрендеренной страницей-карточкой.
   */
  def standaloneBlock(adId: String) = MaybeAuth.async { implicit request =>
    // TODO Вынести логику read-набега на карточку в отдельный ACL ActionBuilder.
    MAd.getById(adId) map {
      case Some(mad) =>
        val bc: BlockConf = BlocksConf(mad.blockMeta.blockId)
        // TODO Проверять карточку на опубликованность?
        cacheControlShort {
          Ok( bc.renderBlock(mad, blk.RenderArgs(isStandalone = true)) )
        }

      case None =>
        warn(s"AdId $adId not found.")
        http404AdHoc
    }
  }


  /** Поиск узлов в рекламной выдаче. */
  def findNodes(args: SimpleNodesSearchArgs) = MaybeAuth.async { implicit request =>
    lazy val logPrefix = s"findNodes(${System.currentTimeMillis}): "
    trace(logPrefix + "Starting with args " + args + " ; remote = " + request.remoteAddress + " ; path = " + request.path + "?" + request.rawQueryString)
    // Для возможной защиты криптографических функций, использующий random, округяем и загрубляем timestamp.
    val tstamp = System.currentTimeMillis() / 50L

    // Запуск детектора текущей ноды, если необходимо. Асинхронно возвращает (lvl, node) или экзепшен.
    // Экзепшен означает, что переключение нод отключено или не удалось найти текущую ноду.
    val gsiOptFut = args.geoMode.geoSearchInfoOpt
    val nextNodeSwitchFut: Future[GeoDetectResult] = if (args.isNodeSwitch) {
      ShowcaseNodeListUtil.detectCurrentNode(args.geoMode, gsiOptFut)
    } else {
      Future failed new NoSuchElementException("Node detect disabled")
    }

    // Нода, которая будет отображена как текущая при следующем набеге на выдачу.
    val nextNodeFut = ShowcaseNodeListUtil.detectRecoverGuessCurrentNode(gsiOptFut, args.currAdnId)(nextNodeSwitchFut)

    // Привести nextNodeFut к формату nextNodeSwitchFut.
    val nextNodeWithLayerFut = ShowcaseNodeListUtil.nextNodeWithLvlOptFut(nextNodeSwitchFut, nextNodeFut)

    // Когда все данные будут собраны, нужно отрендерить результат в виде json.
    val resultFut = for {
      nextNodeGdr <- nextNodeWithLayerFut
      nodesLays5  <- ShowcaseNodeListUtil.collectLayers(args.geoMode, nextNodeGdr.node, nextNodeGdr.ngl)
    } yield {
      import nextNodeGdr.{node => nextNode}
      // Рендер в json следующего узла, если он есть.
      val nextNodeJson = JsObject(Seq(
        "name"  -> JsString(nextNode.meta.name),
        "_id"   -> JsString(nextNode.id getOrElse "")
      ))
      // Список узлов, который надо рендерить юзеру.
      val nodesRendered: Seq[GeoNodesLayer] = if (args.isNodeSwitch && nodesLays5.nonEmpty) {
        // При переключении узла, переключение идёт на наиболее подходящий узел, который первый в списке.
        // Тогда этот узел НЕ надо отображать в списке узлов.
        val nodes0 = nodesLays5.head.nodes
        nodesLays5.head.copy(nodes = nodes0.tail) :: nodesLays5.toList.tail
      } else {
        // Нет переключения узлов. Рендерим все подходящие узлы.
        nodesLays5
      }
      val json = JsObject(Seq(
        "action"      -> JsString("findNodes"),
        "status"      -> JsString("ok"),
        "first_node"  -> nextNodeJson,
        "nodes"       -> _geoNodesListTpl(nodesRendered, Some(nextNode)),
        "timestamp"   -> JsNumber(tstamp)
      ))
      // Без кеша, ибо timestamp.
      Ok( Jsonp(JSONP_CB_FUN, json) )
        .withHeaders(
          CACHE_CONTROL -> s"public, max-age=$FIND_NODES_CACHE_SECONDS"
        )
    }

    // Одновременно собираем статистику по запросу:
    ScNodeListingStat(args, gsiOptFut)
      .saveStats
      .onFailure { case ex =>
        warn(logPrefix + "Failed to save stats", ex)
      }

    // Вернуть асинхронный результат.
    resultFut
  }

}

