package controllers

import io.suggest.adn.edit.{MAdnEditForm, MAdnEditFormConf, MAdnEditFormInit}
import io.suggest.es.model.MEsUuId
import io.suggest.init.routed.MJsiTgs
import io.suggest.util.logs.MacroLogsImpl
import javax.inject.Inject
import models.mctx.Context
import models.mproj.ICommonDi
import play.api.libs.json.Json
import util.acl.IsNodeAdmin
import views.html.lk.adn.edit._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.04.18 15:57
  * Description: Контроллер для react-формы редактирования метаданных ADN-узла.
  * Контроллер заменяет собой MarketLkAdnEdit, который нужен для
  */
class LkAdnEdit @Inject() (
                            isNodeAdmin               : IsNodeAdmin,
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

      // Собрать данные для инициализации начального состояния формы:
      val formInit = MAdnEditFormInit(
        conf = MAdnEditFormConf(
          nodeId = request.mnode.id.get
        ),
        form = MAdnEditForm(
          mmeta = request.mnode.meta.public
        )
      )
      // Сериализация состояния.
      val formInitStr = Json.toJson(formInit).toString()

      for {
        ctx <- ctxFut
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
