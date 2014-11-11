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
with ScAdsTile
{

  import LOGGER._

  val JSONP_CB_FUN = "siomart.receive_response"

  /** Максимальное кол-во магазинов, возвращаемых в списке ТЦ. */
  val MAX_SHOPS_LIST_LEN = configuration.getInt("market.frontend.subproducers.count.max") getOrElse 200


  /** Сколько секунд следует кешировать переменную svg-картинку блока карточки. */
  def BLOCK_SVG_CACHE_SECONDS = configuration.getInt("market.showcase.blocks.svg.cache.seconds") getOrElse 700


  /** Сколько нод максимум накидывать к списку нод в качестве соседних нод. */
  val NEIGH_NODES_MAX = configuration.getInt("market.showcase.nodes.neigh.max") getOrElse 20

  /** Кеш ответа findNodes() на клиенте. Это существенно ускоряет навигацию. */
  val FIND_NODES_CACHE_SECONDS: Int = configuration.getInt("market.showcase.nodes.find.result.cache.seconds") getOrElse {
    if (Play.isProd)  120  else  10
  }



  /** Экшен для рендера горизонтальной выдачи карточек.
    * @param adSearch Поисковый запрос.
    * @param h true означает, что нужна начальная страница с html.
    *          false - возвращать только json-массив с отрендеренными блоками, без html-страницы с первой карточкой.
    * @return JSONP с отрендеренными карточками.
    */
  def focusedAds(adSearch: AdSearch, h: Boolean) = MaybeAuth.async { implicit request =>
    // TODO Не искать вообще карточки, если firstIds.len >= adSearch.size
    // TODO Выставлять offset для поиска с учётом firstIds?
    val mads1Fut = {
      // Костыль, т.к. сортировка forceFirstIds на стороне ES-сервера всё ещё не пашет:
      val adSearch2 = if (adSearch.forceFirstIds.isEmpty) {
        adSearch
      } else {
        adSearch.copy(forceFirstIds = Nil, withoutIds = adSearch.forceFirstIds)
      }
      MAd.dynSearch(adSearch2)
    }
    val madsCountFut = MAd.dynCount(adSearch)  // В countAds() можно отправлять и обычный adSearch: forceFirstIds там игнорируется.
    val producersFut = MAdnNodeCache.multiGet(adSearch.producerIds)
    // Если выставлены forceFirstIds, то нужно подолнительно запросить получение указанных id карточек и выставить их в начало списка mads1.
    val mads2Fut: Future[Seq[MAd]] = if (adSearch.forceFirstIds.nonEmpty) {
      // Если заданы firstIds и offset == 0, то нужно получить из модели указанные рекламные карточки.
      val firstAdsFut = if (adSearch.offset <= 0) {
        MAd.multiGet(adSearch.forceFirstIds)
          .map { _.filter {
            mad => adSearch.producerIds contains mad.producerId
          } }
      } else {
        Future successful Nil
      }
      // Замёржить полученные first-карточки в основной список карточек.
      for {
        mads      <- mads1Fut
        firstAds  <- firstAdsFut
      } yield {
        // Нано-оптимизация.
        if (firstAds.nonEmpty)
          firstAds ++ mads
        else
          mads
      }
    } else {
      // Дополнительно выставлять первые карточки не требуется. Просто возвращаем фьючерс исходного списка карточек.
      mads1Fut
    }
    // Когда поступят карточки, нужно сохранить по ним статистику.
    mads2Fut onSuccess { case mads =>
      ScFocusedAdsStatUtil(adSearch, mads.flatMap(_.id), withHeadAd = h).saveStats
    }
    // Запустить рендер, когда карточки поступят.
    madsCountFut flatMap { madsCount =>
      val madsCountInt = madsCount.toInt
      producersFut flatMap { producers =>
        val producer = producers.head
        mads2Fut flatMap { mads =>
          // Рендерим базовый html подвыдачи (если запрошен) и рендерим остальные рекламные блоки отдельно, для отложенный инжекции в выдачу (чтобы подавить тормоза от картинок).
          val mads4renderAsArray = if (h) mads.tail else mads   // Caused by: java.lang.UnsupportedOperationException: tail of empty list
          val ctx = implicitly[Context]
          // Распараллеливаем рендер блоков по всем ядрам (называется parallel map). На 4ядернике (2 + HT) получается двукратный прирост на 33 карточках.
          val blocksHtmlsFut = parTraverseOrdered(mads4renderAsArray, startIndex = adSearch.offset) {
            (mad, index) =>
              ShowcaseUtil.focusedBrArgsFor(mad)(ctx) map { brArgs =>
                val res = _focusedAdTpl(mad, index + 1, producer, adsCount = madsCountInt, brArgs = brArgs)(ctx)
                JsString(res)
              }
          }
          // В текущем потоке рендерим основную HTML'ку, которая будет сразу отображена юзеру. (если запрошено через аргумент h)
          val htmlOptFut = if (h) {
            val madsHead = mads.headOption
            val firstMads = madsHead.toList
            val bgColor = producer.meta.color getOrElse SITE_BGCOLOR_DFLT
            val brArgsNFut = madsHead.fold
              { Future successful ShowcaseUtil.focusedBrArgsDflt }
              { ShowcaseUtil.focusedBrArgsFor(_)(ctx) }
            brArgsNFut map { brArgsN =>
              val html = _focusedAdsTpl(firstMads, adSearch, producer, bgColor, brArgs = brArgsN, adsCount = madsCountInt,  startIndex = adSearch.offset)(ctx)
              Some(JsString(html))
            }
          } else {
            Future successful  None
          }
          for {
            blocks  <- blocksHtmlsFut
            htmlOpt <- htmlOptFut
          } yield {
            cacheControlShort {
              jsonOk("producerAds", htmlOpt, blocks)
            }
          }
        }
      }
    }
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

