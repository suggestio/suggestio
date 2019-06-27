package controllers.ident

import controllers.ISioControllerApi
import io.suggest.es.model.EsModelDi
import io.suggest.id.IdentConst
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.model.n2.node.{IMNodes, MNodeTypes}
import io.suggest.sec.util.IScryptUtilDi
import io.suggest.session.{LongTtl, MSessionKeys, ShortTtl, Ttl}
import io.suggest.util.logs.IMacroLogs
import models.req.IReq
import models.usr._
import play.api.data._
import play.api.data.Forms._
import util.acl._
import play.api.mvc._
import util.xplay.SetLangCookieUtil
import views.html.ident.login.epw._

import scala.concurrent.Future
import util.FormUtil.passwordM
import util.ident.IIdentUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.15 16:18
 * Description: Поддержка сабмита формы логина по email и паролю.
 */

trait EmailPwLogin
  extends ISioControllerApi
  with IMacroLogs
  with IBruteForceProtect
  with SetLangCookieUtil
  with IIsAnonAcl
  with IIdentUtil
  with IMNodes
  with IScryptUtilDi
  with EsModelDi
{

  import sioControllerApi._
  import mCommonDi._
  import esModel.api._

  val mPersonIdentModel: MPersonIdentModel
  import mPersonIdentModel.api._

  /** Форма логина по email и паролю. */
  def emailPwLoginFormM: EmailPwLoginForm_t = {
    Form(
      mapping(
        "email"       -> email,
        "password"    -> passwordM,
        "remember_me" -> boolean.transform[Ttl](
          { rme => if (rme) LongTtl else ShortTtl },
          { _ == LongTtl }
        )
      )
      { EpwLoginFormBind.apply }
      { EpwLoginFormBind.unapply }
    )
  }

  /** Маппинг формы epw-логина с забиванием дефолтовых значений. */
  def emailPwLoginFormStubM(implicit request: RequestHeader): Future[EmailPwLoginForm_t] = {
    // Пытаемся извлечь email из сессии.
    // Подразумевается, что в истёкшей сессии может быть personId. Не ясно, актуально ли это ещё (конфилкт с SessionExpire?).
    val emailFut: Future[String] = request.session.get(MSessionKeys.PersonId.value) match {
      case Some(personId) =>
        for {
          nodeOpt <- mNodes.dynSearchOne {
            new MNodeSearchDfltImpl {
              override val withIds = personId :: Nil
              override def limit = 1
              override val nodeTypes = MNodeTypes.Person :: Nil
              override val outEdges: Seq[Criteria] = {
                val cr = Criteria(
                  predicates = MPredicates.Ident.Email :: Nil
                )
                cr :: Nil
              }
            }
          }
        } yield {
          val emailsIter = for {
            mnode <- nodeOpt.iterator
            medge <- mnode.edges.withPredicateIter( MPredicates.Ident.Email )
            email <- medge.nodeIds
            if email.nonEmpty
          } yield {
            email
          }
          emailsIter
            .buffered
            .headOption
            .getOrElse("")
        }

      case None =>
        Future.successful("")
    }
    val ttl = Ttl(request.session)
    for (email1 <- emailFut) yield {
      emailPwLoginFormM.fill( EpwLoginFormBind(email1, "", ttl) )
    }
  }


  def emailSubmitOkCall(personId: String)(implicit request: IReq[_]): Future[Call] = {
    identUtil.redirectCallUserSomewhere(personId)
  }

  /** Самбит формы логина по email и паролю. */
  def emailPwLoginFormSubmit(r: Option[String]) = csrf.Check {
    bruteForceProtect {
      isAnon().async { implicit request =>
        val formBinded = emailPwLoginFormM.bindFromRequest()
        formBinded.fold(
          {formWithErrors =>
            LOGGER.debug("emailPwLoginFormSubmit(): Form bind failed:\n" + formatFormErrors(formWithErrors))
            emailSubmitError(formWithErrors, r)
          },
          {binded =>
            // Унести этот код в API, т.к. поиском юзера по email-паролю так же занимается и другой модуль.
            mNodes
              .findUsersByEmailPhoneWithPw( binded.email )
              .flatMap { nodesFound =>
                lazy val logPrefix = s"epwLoginFormSubmit(${binded.email}):"

                nodesFound
                  .onlyWithPassword( binded.password )
                  .headOption
                  .fold {
                    LOGGER.debug(s"$logPrefix Password does not match for users##[${nodesFound.iterator.flatMap(_.id).mkString(", ")}]")
                    val binded1 = binded.copy(password = "")
                    val lf = formBinded
                      .fill(binded1)
                      .withGlobalError("error.unknown.email_pw")
                    emailSubmitError(lf, r)
                  } { mnode =>
                    val personId = mnode.id.get
                    LOGGER.info(s"$logPrefix Password match ok, user#$personId")
                    val mpersonOptFut = mNodes
                      .getByIdCache(personId)
                      .withNodeType(MNodeTypes.Person)

                    val rdrFut = RdrBackOrFut(r) { emailSubmitOkCall(personId) }
                    var addToSession: List[(String, String)] = List(
                      MSessionKeys.PersonId.value -> personId
                    )
                    // Реализация длинной сессии при наличии флага rememberMe.
                    addToSession = binded.ttl.addToSessionAcc(addToSession)
                    val rdrFut2 = rdrFut.map { rdr =>
                      rdr.addingToSession(addToSession : _*)
                    }
                    // Выставить язык, сохраненный ранее в MPerson
                    setLangCookie2(rdrFut2, mpersonOptFut)
                  }
              }
          }
        )
      }
    }
  }


  /** Рендер страницы с возможностью логина по email и паролю. */
  def emailPwLoginForm(r: Option[String]) = csrf.AddToken {
    isAnon().async { implicit request =>
      for (lf <- emailPwLoginFormStubM) yield {
        val resp0 = epwLoginPage(lf, r)
        // Если r не пуст, то добавить в ответ хидер, чтобы js-клиент мог распознать redirected 200 OK
        if (r.isEmpty) {
          resp0
        } else {
          resp0.withHeaders(
            IdentConst.HTTP_HDR_SUDDEN_AUTH_FORM_RESP -> ""
          )
        }
      }
    }
  }

  /** Общий код методов emailPwLoginForm() и emailSubmitError(). */
  protected def epwLoginPage(lf: EmailPwLoginForm_t, r: Option[String])
                            (implicit request: IReq[_]): Result = {
    Ok( loginTpl(lf, r) )
  }

  def emailSubmitError(lf: EmailPwLoginForm_t, r: Option[String])
                      (implicit request: IReq[_]): Future[Result] = {
    epwLoginPage(lf, r)
  }

}
