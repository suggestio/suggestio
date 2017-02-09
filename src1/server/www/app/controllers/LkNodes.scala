package controllers

import com.google.inject.Inject
import io.suggest.bin.ConvCodecs
import io.suggest.init.routed.MJsiTgs
import io.suggest.lk.nodes.{MLknForm, MLknFormInit, MLknSubNodesResp, MLknTreeNode}
import io.suggest.model.n2.node.MNodes
import io.suggest.pick.{PickleSrvUtil, PickleUtil}
import io.suggest.util.logs.MacroLogsImpl
import models.mlk.nodes.MLkNodesTplArgs
import models.mproj.ICommonDi
import util.acl.IsAdnNodeAdmin
import util.lk.nodes.LkNodesUtil
import views.html.lk.nodes.nodesTpl

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.02.17 16:06
  * Description: Контроллер для системы управления деревом узлов.
  * Появился в контексте необходимости системы управления собственными маячками.
  * Маячки -- очень частный случай под-узла, поэтому тут скорее управление ресиверами.
  *
  * Ещё есть необходимость размещаться в маячках. Форма может работать как в контексте карточки,
  * так и в контексте узла.
  *
  * Контроллер также должен препятствовать нежелательной деятельности пользователя:
  * - массового создания маячков с целью занять чужие id'шники.
  */
class LkNodes @Inject() (
                          isAdnNodeAdmin            : IsAdnNodeAdmin,
                          lkNodesUtil               : LkNodesUtil,
                          pickleSrvUtil             : PickleSrvUtil,
                          mNodes                    : MNodes,
                          override val mCommonDi    : ICommonDi
                        )
  extends SioControllerImpl
  with MacroLogsImpl
{

  import mCommonDi._
  import pickleSrvUtil._

  /**
    * Рендер страницы с формой управления подузлами текущего узла.
    * Сама форма реализована через react, тут у нас лишь страничка-обёртка.
    *
    * @param nodeId id текущей узла, т.е. узла с которым идёт взаимодействие.
    * @return 200 + HTML, если у юзера достаточно прав для управления узлом.
    */
  def subNodesOf(nodeId: String) = isAdnNodeAdmin.Get(nodeId, U.Lk).async { implicit request =>

    // Запустить поиск узлов.
    val subNodesFut = mNodes.dynSearch {
      lkNodesUtil.subNodesSearch(nodeId)
    }

    // Рендер найденных узлов в данные для модели формы.
    val subNodesRespFut = for (subNodes <- subNodesFut) yield {
      MLknSubNodesResp(
        nodes = for (mnode <- subNodes) yield {
          MLknTreeNode(
            id                = mnode.id.get,
            name              = mnode.guessDisplayNameOrId.getOrElse("???"),
            ntypeId           = mnode.common.ntype.strId,
            childrenLoaded    = false,
            children          = Nil
          )
        }
      )
    }

    // Собрать модель данных инициализации формы с начальным состоянием формы. Сериализовать в base64.
    val formStateB64Fut = for {
      subNodesResp <- subNodesRespFut
    } yield {
      val minit = MLknFormInit(
        nodes0 = subNodesResp,
        // Собрать начальное состояние формы.
        form   = MLknForm()
      )
      PickleUtil.pickleConv[MLknFormInit, ConvCodecs.Base64, String](minit)
    }

    // Пока подготовить контекст рендера шаблона
    val ctxFut = for {
      lkCtxData <- request.user.lkCtxDataFut
    } yield {
      implicit val lkCtxData2 = lkCtxData.withJsiTgs(
        MJsiTgs.LkNodesForm :: lkCtxData.jsiTgs
      )
      getContext2
    }

    // Отрендерить и вернуть HTML-шаблон со страницей для формы.
    for {
      formStateB64    <- formStateB64Fut
      ctx             <- ctxFut
    } yield {
      val args = MLkNodesTplArgs(
        formState = formStateB64,
        mnode     = request.mnode
      )
      Ok( nodesTpl(args)(ctx) )
    }
  }

}
