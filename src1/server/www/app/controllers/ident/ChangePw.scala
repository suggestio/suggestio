package controllers.ident

import controllers.ISioControllerApi
import models.req.IReq
import play.api.data._
import play.api.data.Forms._
import util.acl._
import util._
import play.api.mvc._
import japgolly.univeq._

import scala.concurrent.Future
import FormUtil.{passwordM, passwordWithConfirmM}
import io.suggest.es.model.EsModelDi
import io.suggest.model.n2.edge.{MEdge, MEdgeInfo, MNodeEdges, MPredicates}
import io.suggest.model.n2.node.{IMNodes, MNode}
import io.suggest.sec.util.IScryptUtilDi
import io.suggest.util.logs.IMacroLogs
import util.ident.IIdentUtil
import views.html.ident.changePasswordTpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.15 16:30
 * Description: Поддержка смены пароля для Ident контроллера и других контроллеров.
 */

object ChangePw {

  /** Маппинг формы смены пароля. */
  def changePasswordFormM = Form(tuple(
    "old" -> passwordM,
    "new" -> passwordWithConfirmM
  ))

}


import ChangePw._


/** Ident-контроллер придерживается этих экшенов. */
trait ChangePw
  extends ChangePwAction
  with IIsAuth
{
  import sioControllerApi._

  /** Страница с формой смены пароля. */
  def changePassword = isAuth() { implicit request =>
    Ok(changePasswordTpl(changePasswordFormM))
  }

  def changePasswordSubmit(r: Option[String]) = isAuth().async { implicit request =>
    _changePasswordSubmit(r) { formWithErrors =>
      NotAcceptable(changePasswordTpl(formWithErrors))
    }
  }

}


/** Контексто-зависимое тело экшена, которое реализует смену пароля у пользователя.
  * Реализации должны оборачивать логику экшена в экшен, выставляя обработчики для ошибок и успехов. */
trait ChangePwAction
  extends ISioControllerApi
  with IMacroLogs
  with IIdentUtil
  with IScryptUtilDi
  with EsModelDi
  with IMNodes
{

  import sioControllerApi._
  import mCommonDi._
  import esModel.api._

  /** Если неясно куда надо редиректить юзера, то что делать? */
  def changePwOkRdrDflt(implicit request: IReq[AnyContent]): Future[Call] = {
    // TODO Избавится от get, редиректя куда-нить в другое место.
    identUtil.redirectCallUserSomewhere(request.user.personIdOpt.get)
  }

  /** Сабмит формы смены пароля. Нужно проверить старый пароль и затем заменить его новым. */
  def _changePasswordSubmit(r: Option[String])(onError: Form[(String, String)] => Future[Result])
                           (implicit request: IReq[AnyContent]): Future[Result] = {
    val personId = request.user.personIdOpt.get
    lazy val logPrefix = s"_changePasswordSubmit($personId): "
    changePasswordFormM.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(logPrefix + "Failed to bind form:\n " + formatFormErrors(formWithErrors))
        onError(formWithErrors)
      },
      {case (oldPw, newPw) =>
        // 2018-02-27 Менять пароль может только юзер, уже имеющий логин. Остальные - идут на госуслуги.
        val resOkFut = for {
          personNode0 <- request.user.personNodeFut

          p = MPredicates.Ident

          epwPreds = p.Email :: p.Password :: Nil

          identEdges = personNode0.edges.withPredicateIter( epwPreds: _* )
            .toList

          // Проверить наличие email-идента для логина, и парольного идента для сверки пароля.
          if identEdges.exists(_.predicate ==* p.Email) &&
             identEdges.exists { e =>
               (e.predicate ==* p.Password) &&
               e.info.textNi.exists( scryptUtil.checkHash(oldPw, _) )
             }

          // Сохранить новый пароль в эдж:
          identEdges2 = for (e <- identEdges) yield {
            if (e.predicate ==* p.Password) {
              MEdge.info
                .composeLens( MEdgeInfo.textNi )
                .set( Some(scryptUtil.mkHash(newPw)) )(e)
            } else {
              e
            }
          }

          // Залить обновлённые эджи в узел юзера:
          _ <- mNodes.tryUpdate(personNode0) { mnode0 =>
            MNode.edges.modify { edges0 =>
              MNodeEdges.out.set(
                MNodeEdges.edgesToMap1(
                  edges0
                    .withoutPredicateIter( epwPreds: _* )
                    .++( identEdges2 )
                )
              )(edges0)
            }(mnode0)
          }

          rdr <- RdrBackOrFut(r)(changePwOkRdrDflt)

        } yield {
          rdr.flashing(FLASH.SUCCESS -> "New.password.saved")
        }

        resOkFut.recoverWith { case ex: Throwable =>
          LOGGER.warn(s"$logPrefix Failed to update password for user#${request.user.personIdOpt.orNull}", ex)
          val formWithErrors = changePasswordFormM.withGlobalError("error.password.invalid")
          onError(formWithErrors)
        }
      }
    )
  }

}


