package controllers.sysctl.domain

import controllers.{ISioControllerApi, routes}
import io.suggest.es.model.EsModelDi
import io.suggest.n2.extra.domain.MDomainExtra
import io.suggest.n2.node.{IMNodes, MNode}
import io.suggest.util.logs.IMacroLogs
import models.msys.{MSysNodeDomainCreateFormTplArgs, MSysNodeDomainEditFormTplArgs}
import models.req.INodeReq
import play.api.data.Form
import play.api.mvc.Result
import play.twirl.api.Html
import util.acl.IIsSuNodeDi
import util.sys.ISysMarketUtilDi
import views.html.sys1.domains._

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.09.16 16:00
  * Description: Аддон для [[controllers.SysMarket]] для экшенов управления списком связанных доменов узла.
  */
trait SmDomains
  extends ISioControllerApi
  with IMacroLogs
  with IIsSuNodeDi
  with ISysMarketUtilDi
  with IMNodes
  with EsModelDi
{

  import sioControllerApi._
  import mCommonDi.{csrf, ec, errorHandler}
  import esModel.api._


  /** Запрос страницы добавления домена к узлу. */
  def createNodeDomain(nodeId: String) = csrf.AddToken {
    isSuNode(nodeId) { implicit request =>
      Ok( _createNodeDomainBody(sysMarketUtil.mDomainExtraFormM) )
    }
  }


  /** Рендер страницы с формой создания домена.
    * Это общий код GET и POST экшенов создания домена для узла. */
  private def _createNodeDomainBody(form: Form[MDomainExtra])(implicit request: INodeReq[_]): Html = {
    val args = MSysNodeDomainCreateFormTplArgs(
      mnode = request.mnode,
      form  = form
    )
    createNodeDomainTpl(args)
  }


  /** Сабмит формы добавления домена к узлу. */
  def createNodeDomainFormSubmit(nodeId: String) = csrf.Check {
    isSuNode(nodeId).async { implicit request =>
      lazy val logPrefix = s"createNodeDomainFormSubmit($nodeId):"
      sysMarketUtil.mDomainExtraFormM.bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.debug(s"$logPrefix Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
          NotAcceptable( _createNodeDomainBody(formWithErrors) )
        },
        {mdx =>
          val saveFut = mNodes.tryUpdate(request.mnode) { mnode =>
            _updateNodeDomains(mnode) {
              mnode.extras.domains
                .iterator
                .filter { _.dkey != mdx.dkey }
                .++( Iterator.single(mdx) )
                .toSeq
            }
          }

          // Отрендерить HTTP-ответ клиенту
          for (_ <- saveFut) yield
            _rdrSuccess(nodeId, s"Домен ${mdx.dkey} добавлен к узлу.")
        }
      )
    }
  }


  /**
    * Эешен рендера страницы с формой редактирования домена на указанном узле.
    * @param nodeId id текущего узла.
    * @param dkey ключ редактируемого узла.
    * @return 200, 403/302.
    */
  def editNodeDomain(nodeId: String, dkey: String) = csrf.AddToken {
    isSuNode(nodeId).async { implicit request =>
      _editNodeDomainBody(dkey, Ok) { sysMarketUtil.mDomainExtraFormM.fill }
    }
  }

  private def _editNodeDomainBody(dkey: String, rs: Status)(formF: MDomainExtra => Form[MDomainExtra])(implicit request: INodeReq[_]): Future[Result] = {
    request.mnode.extras.domains
      .find(_.dkey == dkey)
      .fold {
        errorHandler.onClientError(request, NOT_FOUND, s"Domain '$dkey' not found on node ${request.mnode.id.orNull}.")
      } { mdx =>
        val args = MSysNodeDomainEditFormTplArgs(
          mdx     = mdx,
          form    = formF(mdx),
          mnode   = request.mnode
        )
        rs( editNodeDomainTpl(args) )
      }
  }

  /** Реакция на сабмит формы редактирования одного домена, относящегося к узлу. */
  def editNodeDomainFormSubmit(nodeId: String, dkey: String) = csrf.Check {
    isSuNode(nodeId).async { implicit request =>
      lazy val logPrefix = s"editNodeDomainFormSubmit($nodeId, $dkey):"
      sysMarketUtil.mDomainExtraFormM.bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.debug(s"$logPrefix Failed to bind form: ${formatFormErrors(formWithErrors)}")
          _editNodeDomainBody(dkey, NotAcceptable)(_ => formWithErrors)
        },
        {mdx2 =>
          val dkeysFilteredOut = Set(dkey, mdx2.dkey)
          val mnode2Fut = mNodes.tryUpdate(request.mnode) { mnode =>
            _updateNodeDomains(mnode) {
              mnode.extras.domains
                .iterator
                .filter { mdx => !dkeysFilteredOut.contains(mdx.dkey) }
                .++( Iterator.single(mdx2) )
                .toSeq
            }
          }

          for (_ <- mnode2Fut) yield
            _rdrSuccess(nodeId, s"Обновлён домен $dkey.")
        }
      )
    }
  }


  /** Реакция на сабмит формы-кнопки удаления домена из узла. */
  def deleteNodeDomainFormSubmit(nodeId: String, dkey: String) = csrf.Check {
    isSuNode(nodeId).async { implicit request =>
      val mnode2Fut = mNodes.tryUpdate(request.mnode) { mnode =>
        _updateNodeDomains(mnode) {
          mnode.extras.domains
            .iterator
            .filter { _.dkey != dkey }
            .toSeq
        }
      }

      // Когда всё будет выполнено, отправить юзера на страницу узла.
      for (_ <- mnode2Fut) yield
        _rdrSuccess(nodeId, s"Домен $dkey удалён из узла.")
    }
  }


  /** Вызов двух copy() при обновлении узла вынесен сюда для облегчения скомпиленного байт-кода. */
  private def _updateNodeDomains(mnode: MNode)(domains2: Seq[MDomainExtra]): MNode = {
    mnode.copy(
      extras = mnode.extras.copy(
        domains = domains2
      )
    )
  }

  /** Дедубликация кода типичного положительного ответа во всех POST-экшенах этого трейта. */
  private def _rdrSuccess(nodeId: String, msg: String): Result = {
    Redirect( routes.SysMarket.showAdnNode(nodeId) )
      .flashing(FLASH.SUCCESS -> msg)
  }

}
