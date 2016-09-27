package controllers.sc

import java.util.NoSuchElementException

import _root_.util.di.{IScNlUtil, IScStatUtil}
import models.jsm.NodeListResp
import models.msc._
import models.req.IReq
import play.api.mvc.Result
import play.twirl.api.Html
import util.PlayMacroLogsI
import util.acl._
import views.html.sc.nav._
import play.api.libs.json._
import models._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.11.14 18:10
 * Description: Поддержка экшена доступа к списку узлов и логики генерации этого списка.
 */
trait ScNodesListBase
  extends ScController
  with PlayMacroLogsI
  with IScNlUtil
{

  import mCommonDi._

  /** Гибкая логика обработки запроса сбора списка узлов. */
  protected trait FindNodesLogic {

    def _nsArgs: MScNodeSearchArgs
    implicit def _request: IReq[_]

    lazy val gsiOptFut = _nsArgs.geoMode.geoSearchInfoOpt

    // Запуск детектора текущей ноды, если необходимо. Асинхронно возвращает (lvl, node) или экзепшен.
    // Экзепшен означает, что переключение нод отключено или не удалось найти текущую ноду.
    lazy val nextNodeSwitchFut: Future[GeoDetectResult] = if (_nsArgs.isNodeSwitch) {
      scNlUtil.detectCurrentNode(_nsArgs.geoMode, gsiOptFut)
    } else {
      val ex = new NoSuchElementException("Node detect disabled")
      Future.failed(ex)
    }

    /** Нода, которая будет отображена как текущая на грядущем шаге. */
    def nextNodeFut = {
      scNlUtil.detectRecoverGuessCurrentNode(gsiOptFut, _nsArgs.currAdnId)(nextNodeSwitchFut)
    }

    /** Привести nextNodeFut к формату nextNodeSwitchFut. */
    lazy val nextNodeWithLayerFut = {
      scNlUtil.nextNodeWithLvlOptFut(nextNodeSwitchFut, nextNodeFut)
    }

    /** Сырые навигационные слои узлов. */
    def nglsFut: Future[Seq[GeoNodesLayer]] = {
      nextNodeWithLayerFut.flatMap { nextNodeGdr =>
        scNlUtil.collectLayers(Some(_nsArgs.geoMode), nextNodeGdr.node, nextNodeGdr.ngl)
      }
    }

    /** Почищенные навигационные слои узлов, которые нужно отрендерить юзеру. */
    def gnls4RenderFut: Future[Seq[GeoNodesLayer]] = {
      for (nodesLays5 <- nglsFut) yield {
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
          override def apiVsn       = _nsArgs.apiVsn
          override def nodeLayers   = gnls4Render
          override def currNode: Option[MNode] = Some(nextNodeGdr.node)
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


/** Аддон к Showcase-контроллеру, добавляющий обработку запроса списка узлов. */
trait ScNodesList
  extends ScNodesListBase
  with IScStatUtil
  with MaybeAuth
{

  import mCommonDi._

  /** Кеш ответа findNodes() на клиенте. Это существенно ускоряет навигацию. */
  protected val FIND_NODES_CACHE_SECONDS: Int = {
    configuration
      .getInt("market.showcase.nodes.find.result.cache.seconds")
      .getOrElse {
        if (isProd)  120  else  10
      }
  }


  /** Поиск узлов в рекламной выдаче. */
  def findNodes(args: MScNodeSearchArgs) = MaybeAuth().async { implicit request =>
    // Загрубляем timestamp, на всякий случай.
    val tstamp = System.currentTimeMillis() / 50L
    val logic = FindNodesLogicV(tstamp, args)
    // заворачиваем в json результаты работы логики.
    val resultFut = logic.resultCachedFut
    //LOGGER.trace( s"findNodes($args): [$tstamp] remote = ${request.remoteAddress}; path=${request.uri}" )

    // Вернуть асинхронный результат.
    resultFut
  }


  /** Компаньон логик для разруливания версий логик обработки HTTP-запросов. */
  protected object FindNodesLogicV {
    /** Собрать необходимую логику обработки ответа в заисимости от версии API. */
    def apply(tstamp: Long, args: MScNodeSearchArgs)(implicit request: IReq[_]): FindNodesLogicV = {
      args.apiVsn match {
        case MScApiVsns.Sjs1 =>
          new FindNodesLogicV2(tstamp, args)
      }
    }
  }

  /** Расширение логики обработки запросов для поддержки версионизации API. */
  protected trait FindNodesLogicV extends super.FindNodesLogic {

    /** Временная отметка начала обработки запроса. */
    def timestamp: Long
    
    /** Отрендеренный в HTML список узлов, минифицированый и готовый к сериализации внутри JSON. */
    def nodeListHtmlJsStrFut: Future[JsString] = {
      nodesListRenderedFut
        .map { r => htmlCompressUtil.html2jsStr( r() ) }
    }

    /** Получение ответа, пригодного для сериализации в JSON. */
    def respArgsFut: Future[NodeListResp] = {
      val _nodeListHtmlJsStrFut = nodeListHtmlJsStrFut
      for {
        nextNodeLay <- nextNodeWithLayerFut
        rendered    <- _nodeListHtmlJsStrFut
      } yield {
        NodeListResp(
          status        = "ok",
          adnNode       = nextNodeLay.node,
          nodesListHtml = rendered,
          timestamp     = timestamp
        )
      }
    }

    /** Рендер HTTP-ответа (результата). Результат зависит от версии API. */
    def resultFut: Future[Result]

    def resultCachedFut: Future[Result] = {
      resultFut.map { result =>
        result.withHeaders(
          CACHE_CONTROL -> s"public, max-age=$FIND_NODES_CACHE_SECONDS"
        )
      }
    }
  }


  /** Реализация логики для SC API v2: ответы в чистом JSON. */
  protected class FindNodesLogicV2(val timestamp: Long, val _nsArgs: MScNodeSearchArgs)
                                  (implicit val _request: IReq[_]) extends FindNodesLogicV {
    override def resultFut: Future[Result] = {
      for (respArgs <- respArgsFut) yield {
        Ok(respArgs.toJson)
      }
    }
  }

}
