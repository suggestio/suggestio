package controllers.sc

import java.util.NoSuchElementException
import _root_.util.jsa.{Js, SmRcvResp}
import models.jsm.NodeListResp
import play.twirl.api.Html
import util.PlayMacroLogsI
import _root_.util.showcase._
import util.acl._
import views.html.sc._
import play.api.libs.json._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import play.api.Play, Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.11.14 18:10
 * Description: Поддержка экшена доступа к списку узлов и логики генерации этого списка.
 */
trait ScNodesList extends ScController with PlayMacroLogsI {

  /** Кеш ответа findNodes() на клиенте. Это существенно ускоряет навигацию. */
  protected val FIND_NODES_CACHE_SECONDS: Int = {
    configuration.getInt("market.showcase.nodes.find.result.cache.seconds") getOrElse {
      if (Play.isProd)  120  else  10
    }
  }


  /** Поиск узлов в рекламной выдаче. */
  def findNodes(args: SimpleNodesSearchArgs) = MaybeAuth.async { implicit request =>
    // Для возможной защиты криптографических функций, использующий random, округяем и загрубляем timestamp.
    val tstamp = System.currentTimeMillis() / 50L

    lazy val logPrefix = s"findNodes(${System.currentTimeMillis}): "
    LOGGER.trace(logPrefix + "Starting with args " + args + " ; remote = " + request.remoteAddress + " ; path = " + request.path + "?" + request.rawQueryString)

    val logic = new FindNodesLogic {
      override def _request = request
      override def _nsArgs = args
    }

    // Запускаем получение  результатов из nodelist-логики
    val renderedFut = logic.nodesListRenderedFut
      .map { r => JsString(r()) }

    val respArgsFut: Future[NodeListResp] = for {
      nextNodeLay <- logic.nextNodeWithLayerFut
      rendered    <- renderedFut
    } yield {
      NodeListResp(
        status        = "ok",
        adnNode       = nextNodeLay.node,
        nodesListHtml = rendered,
        timestamp     = tstamp
      )
    }

    // заворачиваем в json результаты работы логики.
    val resultFut = for {
      respArgs  <- respArgsFut
    } yield {
      Ok( Js(8192, SmRcvResp(respArgs)) )
        .withHeaders(
          CACHE_CONTROL -> s"public, max-age=$FIND_NODES_CACHE_SECONDS"
        )
    }

    // Одновременно собираем статистику по текущему запросу:
    ScNodeListingStat(args, logic.gsiOptFut)
      .saveStats
      .onFailure { case ex =>
        LOGGER.warn("Failed to save stats", ex)
      }

    // Вернуть асинхронный результат.
    resultFut
  }



  /** Гибкая логика обработки запроса сбора списка узлов. */
  trait FindNodesLogic {

    def _nsArgs: SimpleNodesSearchArgs
    implicit def _request: AbstractRequestWithPwOpt[_]

    lazy val gsiOptFut = _nsArgs.geoMode.geoSearchInfoOpt

    // Запуск детектора текущей ноды, если необходимо. Асинхронно возвращает (lvl, node) или экзепшен.
    // Экзепшен означает, что переключение нод отключено или не удалось найти текущую ноду.
    lazy val nextNodeSwitchFut: Future[GeoDetectResult] = if (_nsArgs.isNodeSwitch) {
      ShowcaseNodeListUtil.detectCurrentNode(_nsArgs.geoMode, gsiOptFut)
    } else {
      Future failed new NoSuchElementException("Node detect disabled")
    }

    /** Нода, которая будет отображена как текущая на грядущем шаге. */
    def nextNodeFut = ShowcaseNodeListUtil.detectRecoverGuessCurrentNode(gsiOptFut, _nsArgs.currAdnId)(nextNodeSwitchFut)

    /** Привести nextNodeFut к формату nextNodeSwitchFut. */
    lazy val nextNodeWithLayerFut = ShowcaseNodeListUtil.nextNodeWithLvlOptFut(nextNodeSwitchFut, nextNodeFut)

    /** Сырые навигационные слои узлов. */
    def nglsFut: Future[Seq[GeoNodesLayer]] = {
      nextNodeWithLayerFut.flatMap { nextNodeGdr =>
        ShowcaseNodeListUtil.collectLayers(_nsArgs.geoMode, nextNodeGdr.node, nextNodeGdr.ngl)
      }
    }

    /** Почищенные навигационные слои узлов, которые нужно отрендерить юзеру. */
    def gnls4RenderFut: Future[Seq[GeoNodesLayer]] = {
      nglsFut map { nodesLays5 =>
        if (_nsArgs.isNodeSwitch && nodesLays5.nonEmpty) {
          // При переключении узла, переключение идёт на наиболее подходящий узел, который первый в списке.
          // Тогда этот узел НЕ надо отображать в списке узлов.
          val nodes0 = nodesLays5.head.nodes
          nodesLays5.head.copy(nodes = nodes0.tail) :: nodesLays5.toList.tail
        } else {
          // Нет переключения узлов. Рендерим все подходящие узлы.
          nodesLays5
        }
      }
    }

    /** Сборка контейнера с аргументами рендера шаблона. */
    def renderArgsFut: Future[NodeListRenderArgs] = {
      val _gnls4RenderFut = gnls4RenderFut
      for {
        nextNodeGdr <- nextNodeWithLayerFut
        gnls4Render <- _gnls4RenderFut
      } yield {
        new NodeListRenderArgs {
          override def nodeLayers: Seq[GeoNodesLayer] = gnls4Render
          override def currNode: Option[MAdnNode] = Some(nextNodeGdr.node)
        }
      }
    }

    /** Асинхронный результат рендера списка узлов. */
    def nodesListRenderedFut: Future[JsStateRenderWrapper] = {
      renderArgsFut map { renderArgs =>
        new JsStateRenderWrapper {
          override def apply(_jsStateOpt: Option[ScJsState]): Html = {
            val args1 = new NodeListRenderArgsWrapper {
              override def _nlraUnderlying: NodeListRenderArgs = renderArgs
              override def jsStateOpt = _jsStateOpt
            }
            _geoNodesListTpl(args1)
          }
        }
      }
    }

  }

}
