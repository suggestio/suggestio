package util.acl

import java.net.URL

import models.MAdnNode
import play.api.data._, Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.adv._
import play.api.mvc.{Results, Result, Request, ActionBuilder}
import util.FormUtil.{urlM, esIdUuidM}
import util.PlayMacroLogsDyn
import util.acl.PersonWrapper.PwOpt_t
import util.SiowebEsUtil.client

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.01.15 16:47
 * Description: Файл содержит ACL ActionBuilder для сабмита экземпляров данных в [[models.adv.MExtTarget]].
 * Это первый ActionBuilder в проекте, залезающий в тело реквеста с помощью маппинга формы.
 * В теле может содержаться id экшена, и нужно проверять права доступа.
 */

object CanSubmitExtTargetForNode {

  /**
   * Маппинг формы для ввода ссылки на цель.
   * @param adnId id узла, в рамках которого происходит действо.
   * @return Экземпляр формы.
   */
  def targetFormM(adnId: String): Form[MExtTarget] = {
    val tgUrlM = urlM
      .transform[(URL, Option[MExtService])] (
        {url =>
          //val url1 = srv.normalizeTargetUrl(url)
          url -> MExtServices.findForHost(url.getHost)
        },
        { _._1 }
      )
      .verifying("error.service.unknown", _._2.isDefined)
      .transform[(String, MExtService)] (
        {case (url, srvOpt) =>
          val srv = srvOpt.get
          val url1 = srv.normalizeTargetUrl(url)
          (url1, srv) },
        {case (url, srv) =>
          (new URL(url), Some(srv)) }
      )
    val m = mapping(
      "url" -> tgUrlM,
      "id"  -> optional(esIdUuidM)
    )
    {case ((url, srv), idOpt) =>
      MExtTarget(url = url, service = srv, adnId = adnId, id = idOpt)
    }
    {tg =>
      val res = ((tg.url, tg.service), tg.id)
      Some(res)
    }
    Form(m)
  }

}


/** Экземпляр реквеста на сабмит цели. */
case class ExtTargetSubmitRequest[A](
  pwOpt       : PwOpt_t,
  adnNode     : MAdnNode,
  newTgForm   : Form[MExtTarget],
  tgExisting  : Option[MExtTarget],
  sioReqMd    : SioReqMd,
  request     : Request[A]
) extends AbstractRequestForAdnNode(request) {
  override def isMyNode = true
}


/** Заготовка ActionBuilder'а для проверки доступа на запись (create, edit) для target'а,
  * id которого возможно задан в теле POST'а. */
trait CanSubmitExtTargetForNodeBase extends ActionBuilder[ExtTargetSubmitRequest] with PlayMacroLogsDyn {

  /** id узла, заявленного клиентом. */
  def adnId: String

  override def invokeBlock[A](request: Request[A], block: (ExtTargetSubmitRequest[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    val isAdnNodeAdmFut = IsAdnNodeAdmin.isAdnNodeAdmin(adnId, pwOpt)
    val formBinded = CanSubmitExtTargetForNode.targetFormM(adnId).bindFromRequest()(request)
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
    val res = Results.Forbidden(s"Target ${tg.id.get} doesn't belongs to node[$adnId].")
    Future successful res
  }

}

case class CanSubmitExtTargetForNode(adnId: String)
  extends CanSubmitExtTargetForNodeBase
  with ExpireSession[ExtTargetSubmitRequest]
