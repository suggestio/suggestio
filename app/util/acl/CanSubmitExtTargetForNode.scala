package util.acl

import controllers.SioController
import io.suggest.di.IEsClient
import models.MAdnNode
import models.req.SioReqMd
import play.api.data._
import models.adv._
import play.api.mvc.{Result, Request, ActionBuilder}
import util.PlayMacroLogsDyn
import util.acl.PersonWrapper.PwOpt_t
import util.adv.ExtUtil

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.01.15 16:47
 * Description: Файл содержит ACL ActionBuilder для сабмита экземпляров данных в [[models.adv.MExtTarget]].
 * Это первый ActionBuilder в проекте, залезающий в тело реквеста с помощью маппинга формы.
 * В теле может содержаться id экшена, и нужно проверять права доступа.
 */


/** Экземпляр реквеста на сабмит цели. */
case class ExtTargetSubmitRequest[A](
  pwOpt       : PwOpt_t,
  adnNode     : MAdnNode,
  newTgForm   : Form[(MExtTarget, Option[MExtReturn])],
  tgExisting  : Option[MExtTarget],
  sioReqMd    : SioReqMd,
  request     : Request[A]
)
  extends AbstractRequestForAdnNode(request)
{
  override def isMyNode = true
}


/** Аддон для контроллера для добавления поддержки */
trait CanSubmitExtTargetForNode extends SioController with IEsClient {

  /** Заготовка ActionBuilder'а для проверки доступа на запись (create, edit) для target'а,
    * id которого возможно задан в теле POST'а. */
  trait CanSubmitExtTargetForNodeBase extends ActionBuilder[ExtTargetSubmitRequest] with PlayMacroLogsDyn {

    /** id узла, заявленного клиентом. */
    def adnId: String

    override def invokeBlock[A](request: Request[A], block: (ExtTargetSubmitRequest[A]) => Future[Result]): Future[Result] = {
      val pwOpt = PersonWrapper.getFromRequest(request)
      val isAdnNodeAdmFut = IsAdnNodeAdmin.isAdnNodeAdmin(adnId, pwOpt)
      val formBinded = ExtUtil.oneTargetFullFormM(adnId).bindFromRequest()(request)
      val srmFut = SioReqMd.fromPwOptAdn(pwOpt, adnId)
      // Запускаем сразу в фоне поиск уже сохранённой цели.
      val tgIdOpt = formBinded.apply("id").value
      val tgOptFut = tgIdOpt match {
        case Some(tgId) =>
          MExtTarget.getById(tgId)
        case None =>
          Future successful Option.empty[MExtTarget]
      }
      isAdnNodeAdmFut flatMap {
        // Юзер является админом текущего узла. Нужно проверить права на цель.
        case Some(mnode) =>
          // Всё ок может быть в разных случаях, Общий код вынесен сюда.
          def allOk(tgOpt: Option[MExtTarget] = None): Future[Result] = {
            srmFut flatMap { srm =>
              val req1 = ExtTargetSubmitRequest(
                pwOpt       = pwOpt,
                adnNode     = mnode,
                newTgForm   = formBinded,
                tgExisting  = tgOpt,
                sioReqMd    = srm,
                request     = request
              )
              block(req1)
            }
          }
          tgOptFut flatMap {
            // Цель не существует...
            case None =>
              if (tgIdOpt.isDefined)
                // но если id цели передан, то это ненормально
                LOGGER.debug(s"User[$pwOpt] submitted tg_id[${tgIdOpt.get}] for inexisting ext target. Tolerating...")
              allOk()

            // Есть такая цель в хранилищах.
            case someTg @ Some(tg) =>
              if (tg.adnId == adnId) {
                // Эта цель принадлежит узлу, которым владеет текущий юзер.
                allOk(someTg)
              } else {
                // [xakep] Ксакеп отакует: попытка перезаписать чужую цель.
                breakInAttempted(pwOpt, request, tg)
              }
          }

        // Нет прав на узел.
        case None =>
          IsAdnNodeAdmin.onUnauth(request, pwOpt)
      }
    }

    def breakInAttempted(pwOpt: PwOpt_t, request: Request[_], tg: MExtTarget): Future[Result] = {
      LOGGER.warn(s"FORBIDDEN: User[$pwOpt] @${request.remoteAddress} tried to rewrite foreign target[${tg.id.get}] via node[$adnId]. AdnNode expected = ${tg.adnId}.")
      val res = Forbidden(s"Target ${tg.id.get} doesn't belongs to node[$adnId].")
      Future successful res
    }

  }

  case class CanSubmitExtTargetForNodePost(override val adnId: String)
    extends CanSubmitExtTargetForNodeBase
    with ExpireSession[ExtTargetSubmitRequest]
    with CsrfPost[ExtTargetSubmitRequest]

}
