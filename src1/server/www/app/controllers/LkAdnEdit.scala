package controllers

import io.suggest.adn.edit.m.{MAdnEditForm, MAdnEditFormConf, MAdnEditFormInit, MAdnResView}
import io.suggest.es.model.MEsUuId
import io.suggest.init.routed.MJsiTgs
import io.suggest.jd.MJdEdgeId
import io.suggest.model.n2.edge.{MPredicate, MPredicates}
import io.suggest.util.logs.MacroLogsImpl
import javax.inject.Inject
import models.mctx.Context
import models.mproj.ICommonDi
import play.api.libs.json.Json
import util.acl.IsNodeAdmin
import util.ad.JdAdUtil
import util.cdn.CdnUtil
import views.html.lk.adn.edit._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.04.18 15:57
  * Description: Контроллер для react-формы редактирования метаданных ADN-узла.
  * Контроллер заменяет собой MarketLkAdnEdit, который нужен для
  */
class LkAdnEdit @Inject() (
                            isNodeAdmin               : IsNodeAdmin,
                            jdAdUtil                  : JdAdUtil,
                            cdnUtil                   : CdnUtil,
                            override val mCommonDi    : ICommonDi
                          )
  extends SioControllerImpl
  with MacroLogsImpl
{

  import mCommonDi._


  /** Экшен, возвращающий html-страницу с формой редактирования узла.
    * Начальные параметры
    *
    * @param nodeId id узла.
    * @return Страница, инициализирующая форму размещения узла-ресивера.
    */
  def editNodePage(nodeId: MEsUuId) = csrf.AddToken {
    isNodeAdmin(nodeId, U.Lk).async { implicit request =>
      // Запустить сборку контекста:
      val ctxFut = for (ctxData0 <- request.user.lkCtxDataFut) yield {
        implicit val ctxData1 = ctxData0.withJsiTgs(
          jsiTgs2 = MJsiTgs.LkAdnEditForm :: ctxData0.jsiTgs
        )
        implicitly[Context]
      }

      // Какие предикаты и эджи здесь интересуют?
      val nodeImgPredicates = MPredicates.Logo ::
        MPredicates.WcFgImg ::
        Nil
      val imgsEdges = jdAdUtil.collectImgEdges(request.mnode.edges, nodeImgPredicates)

      // Запустить сбор данных по интересующим картинкам:
      val imgMediasMapFut = jdAdUtil.prepareImgMedias( imgsEdges )
      val mediaNodesMapFut = jdAdUtil.prepareMediaNodes( imgsEdges, videoEdges = Nil )

      lazy val logPrefix = s"editNodePage($nodeId)#${System.currentTimeMillis()}:"

      val mediaHostsMapFut = for {
        imgMedias  <- imgMediasMapFut
        mediaHosts <- cdnUtil.mediasHosts( imgMedias.values )
      } yield {
        LOGGER.trace(s"$logPrefix For ${imgMedias.size} medias (${imgMedias.keysIterator.mkString(", ")}) found ${mediaHosts.size} media hosts = ${mediaHosts.values.flatMap(_.headOption).map(_.namePublic).mkString(", ")}")
        mediaHosts
      }

      // Скомпилить в jd-эджи собранную инфу, затем завернуть в edit-imgs:
      val jdEdgesFut = for {
        imgMediasMap  <- imgMediasMapFut
        mediaNodesMap <- mediaNodesMapFut
        mediaHostsMap <- mediaHostsMapFut
        ctx           <- ctxFut
      } yield {
        jdAdUtil.mkJdImgEdgesForEdit(
          imgsEdges  = imgsEdges,
          mediasMap  = imgMediasMap,
          mediaNodes = mediaNodesMap,
          mediaHosts = mediaHostsMap
        )(ctx)
      }

      // Синхронно подготовить прочие данные для инициализации:
      val mconf = MAdnEditFormConf(
        nodeId   = request.mnode.id.get
      )
      val mMetaPub = request.mnode.meta.public

      // Собрать данные для инициализации начального состояния формы:
      val formInitStrFut = for {
        jdEdges <- jdEdgesFut
      } yield {
        LOGGER.trace(s"$logPrefix Compiled jd-edges: $jdEdges")
        def __getImgEdge(pred: MPredicate) = jdEdges.find(_.predicate ==* pred).map(e => MJdEdgeId(e.id))

        val minit = MAdnEditFormInit(
          conf = mconf,
          form = MAdnEditForm(
            meta  = mMetaPub,
            edges = jdEdges,
            resView = MAdnResView(
              logo = __getImgEdge( MPredicates.Logo ),
              wcFg = __getImgEdge( MPredicates.WcFgImg )
            )
          )
        )
        // Сериализация состояния в строку:
        Json.toJson(minit)
          .toString()
      }

      // Сгенерить итоговый ответ сервера и ответить наконец:
      for {
        ctx         <- ctxFut
        formInitStr <- formInitStrFut
      } yield {
        val html = nodeEdit2Tpl(
          mnode         = request.mnode,
          formStateStr  = formInitStr
        )(ctx)
        Ok( html )
      }
    }
  }


  /** Экшен сохранения (обновления) узла.
    *
    * @param nodeId id узла.
    */
  def save(nodeId: MEsUuId) = csrf.Check {
    isNodeAdmin(nodeId).async { implicit request =>
      ???
    }
  }

}
