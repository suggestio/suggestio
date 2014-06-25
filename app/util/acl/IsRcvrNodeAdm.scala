package util.acl

import java.sql.Connection

import play.api.mvc.{Results, Result, ActionBuilder, Request}
import util.PlayMacroLogsImpl
import util.acl.PersonWrapper.PwOpt_t
import models._
import scala.concurrent.Future
import play.api.Play.current
import play.api.db.DB
import IsAuth.onUnauth
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.05.14 17:26
 * Description: Есть ли у юзера admin-доступ к узлам, на которые отправлена на публикацию указанная карточка?
 * На этом этапе неизвестно, с какой стороны на узел смотрит текущий юзер.
 */



/** Статические функции логгирования для ACL AdvWndAccess. */
object AdvWndAccess extends PlayMacroLogsImpl {

  def notFoundFut = Future successful Results.NotFound
}

import AdvWndAccess._

/**
 * Запрос окна с информацией о размещении карточки. Такое окно может запрашивать как создатель карточки,
 * так и узел-ресивер, который модерирует или уже отмодерировал карточку. Такое же окно может появлятся у узла,
 * которому делегировали обязанности модераторации запросов рекламных карточек.
 */
trait AdvWndAccessBase extends ActionBuilder[AdvWndRequest] {
  val adId: String
  val povAdnId: Option[String]
  val needMBB: Boolean

  import LOGGER._

  def hasNotExpiredAdvs(rcvrId: String): Boolean = {
    DB.withConnection { implicit c =>
      hasNotExpiredAdvsConn(rcvrId)
    }
  }

  def hasNotExpiredAdvsConn(rcvrId: String)(implicit c: Connection): Boolean = {
    MAdvOk.hasNotExpiredByAdIdAndRcvr(adId, rcvrId)  ||  MAdvReq.hasNotExpiredByAdIdAndRcvr(adId, rcvrId)
  }

  /**
   * Асинхронно вычислить связи между текущей карточкой до pov-узла, рассматривая его как вероятного делегата-модератора.
   * @param pai povAdnId
   * @param pwOpt Данные о текущем юзере.
   * @return Фьючерс с опшином. Если Some, то значит хотя бы одна связь между карточкой и pov-узлом-модератором есть.
   *         Если связей не установлено или узел отсутствует, то None.
   */
  def findRcvrMdr(pai: String, pwOpt: PwOpt_t): Future[Option[PovRcvrInfo]] = {
    val dgNodeOptFut = MAdnNodeCache.getById(pai)
      .map { _.filter {
        IsAdnNodeAdmin.isAdnNodeAdminCheck(_, pwOpt)
      }}
    dgNodeOptFut flatMap {
      // Если узел-модератор не существует, то дальше делать уже ничего не надо
      case None =>
        warn(s"advDelegate node[$pai] not found or user[${pwOpt.map(_.personId)}] has NO admin rights on this node.")
        Future successful Option.empty[PovRcvrInfo]
      // Узел, указанный в pai существует.
      case Some(dgNode) =>
        // Фоновый запуск поиска id узлов, которые делегировали своё adv-право указанному узлу (povAdnId).
        val rcvrIdsDgFut = MAdnNode.findIdsAdvDelegatedTo(pai)
        // Для установки связи между карточкой, ресивером и делегатом-модератором ресивера надо выкачать список всех текущих размещений карточки.
        val syncResult = DB.withConnection { implicit c =>
          val advsOk = MAdvOk.findNotExpiredByAdId(adId)
          val advsReq = MAdvReq.findNotExpiredByAdId(adId)
          (advsOk, advsReq)
        }
        val (advsOk, advsReq) = syncResult
        val advs = advsOk ++ advsReq
        // Определяем, есть ли ресивер карточки среди делегатов-ресиверов текущего pov-узла.
        val advsRcvrsSet = advs.map(_.rcvrAdnId).toSet
        rcvrIdsDgFut map { rcvrIdsDg =>
          // Множество ресиверов, для которых pov-узел занимается модераторством.
          val resultRcvrIds = rcvrIdsDg.toSet intersect advsRcvrsSet
          if (resultRcvrIds.isEmpty) {
            warn(s"User[${pwOpt.map(_.personId)}] have rights on pov node[$pai], but there are no adv-connection to ad[$adId].")
            None
          } else {
            Some(PovRcvrInfo(dgNode, resultRcvrIds, isMdr = true, isNodeAdm = true))
          }
        }
    }
  }

  /**
   * Асинхронно фильтрануть возможный узел ресивера по правам на админство юзера.
   * @param rcvrOptFut Результат MAdnNode.getById().
   * @param pwOpt Данные по текущему юзеру.
   * @return Фьючерс с опшином. Если Some() значит узел существует, и юзер является админом этого узла.
   *         Иначе None.
   */
  def maybeRcvrAdmin(rcvrOptFut: Future[Option[MAdnNode]], pwOpt: PwOpt_t): Future[Option[PovRcvrInfo]] = {
    rcvrOptFut map {
      _.filter { rcvrId  =>
        val result = IsAdnNodeAdmin.isAdnNodeAdminCheck(rcvrId, pwOpt)
        // Логгируем результат фильтрации
        lazy val logPrefix = {
          val personId = pwOpt.fold("?anonymous?")(_.personId)
          s"maybeRcvrAdmin(): User[$personId] "
        }
        if (result) {
          trace(s"${logPrefix}is admin of rcvr[$rcvrId] of ad[$adId]. So pov node is user's rcvr.")
        } else {
          warn(s"${logPrefix}refused to access to ad[$adId] from/as rcvr node[$rcvrId].")
        }
        // Возвращаем результат фильтрации.
        result
      }
      .map { rcvr  =>  PovRcvrInfo(rcvr, rcvr.id.toSet, isNodeAdm = true, isMdr = false) }
    }
  }

  /**
   * Асинхронно сконвертить результат поиска ресивера в опциональную инфу типа [[PovRcvrInfo]]
   * @param rcvrOptFut Результат MAdnNode.getById().
   * @param pwOpt Инфа по юзеру.
   * @return Фьючерс с опшеном. Если Some(), значит узел ресивера существует. Иначе None.
   */
  def maybeRcvr2arInfo(rcvrOptFut: Future[Option[MAdnNode]], pwOpt: PwOpt_t): Future[Option[PovRcvrInfo]] = {
    rcvrOptFut map {
      _.map { rcvr =>
        val rcvrAdmin = IsAdnNodeAdmin.isAdnNodeAdminCheck(rcvr, pwOpt)
        PovRcvrInfo(rcvr, rcvr.id.toSet, isNodeAdm = rcvrAdmin, isMdr = false)
      }
    }
  }

  /** Запуск суровой функции проверки прав доступа, которая определеляет текущую обстановку и затем проверяет те или
    * иные права доступа. Почти всё это происходит асихронно. */
  override def invokeBlock[A](request: Request[A], block: (AdvWndRequest[A]) => Future[Result]): Future[Result] = {
    PersonWrapper.getFromRequest(request) match {
      case pwOpt @ Some(pw) =>
        MAd.getById(adId).flatMap {
          case Some(mad) =>
            val producerOptFut = MAdnNodeCache.getById(mad.producerId)
            // Вычислить id ресивера исходя того, что передано в fromAdnId. Если во fromAdnId узел-модератор, то
            val rcvrIdOpt = povAdnId filter {
              rcvrId  =>  rcvrId != mad.producerId  &&  hasNotExpiredAdvs(rcvrId)
            }
            val rcvrOptFut = rcvrIdOpt
              .fold [Future[Option[MAdnNode]]] { Future successful None } { MAdnNodeCache.getById }
            producerOptFut flatMap {
              case Some(producer) =>
                // Strict-проверка прав на продьюсер, чтобы не терять логику работы в случае ресивера при суперюзера.
                val isProducerAdmin = if (povAdnId exists { _ != mad.producerId }) {
                  // В povAdnId задан не producer, поэтому подразумеваем, что юзер не является админом продьюсером, даже если он админ.
                  false
                } else {
                  IsAdnNodeAdmin.isAdnNodeAdminCheckStrict(producer, pwOpt)
                }
                val rcvrInfoFut: Future[Option[PovRcvrInfo]] = if (isProducerAdmin) {
                  // Юзер выступает в роли автора рекламной карточки.
                  trace(s"User[${pw.personId}] is admin of producer node[${mad.producerId}].")
                  maybeRcvr2arInfo(rcvrOptFut, pwOpt)
                } else if (povAdnId.isDefined && rcvrIdOpt.isEmpty) {
                  // Возможно, pov-узел - это узел-модератор узла-ресивера.
                  findRcvrMdr(povAdnId.get, pwOpt)
                } else if (rcvrIdOpt.isDefined) {
                  // Есть id ресивера, и в фоне уже идёт запрос данных по ресиверу. Цепляемся за него.
                  maybeRcvrAdmin(rcvrOptFut, pwOpt)
                } else {
                  // Больше идей нет.
                  debug(s"I have NO ideas about connection between ad[$adId] and user[${pw.personId}] with povAdnId = $povAdnId ;; Giving up.")
                  Future successful None
                }
                rcvrInfoFut flatMap {
                  // Юзер шел к успеху, но не фартануло с проверкой прав доступа. Причина отказа уже в логах.
                  case None if !isProducerAdmin =>
                    Future successful Results.Forbidden("Access denied.")
                  case povInfoOpt =>
                    trace(s"ad[$adId] povAdnId=$povAdnId :: OK :: isProdAdm -> $isProducerAdmin rcvrId -> $rcvrIdOpt povInfoOpt -> $povInfoOpt")
                    val srmFut: Future[SioReqMd] = Some(mad.producerId)
                      .filter { _ => isProducerAdmin }
                      .orElse { povInfoOpt.filter(_.isNodeAdm).flatMap(_.node.id) }
                      .filter { _ => needMBB }    // TODO Opt Надо бы needMBB учитывать до начала вычисления узла текущего кошелька.
                      .fold [Future[SioReqMd]] { SioReqMd.fromPwOpt(pwOpt) } { SioReqMd.fromPwOptAdn(pwOpt, _) }
                    srmFut flatMap { srm =>
                      val rcvrInfoOpt = povInfoOpt.filter(!_.isMdr)
                      val req1 = AdvWndRequest(mad, producer,
                        rcvrOpt = rcvrInfoOpt.map(_.node),
                        isProducerAdmin = isProducerAdmin,
                        isRcvrAdmin = rcvrInfoOpt.fold(false)(_.isNodeAdm),
                        mdrOpt = povInfoOpt.filter(_.isMdr).map(_.node),
                        rcvrIds = povInfoOpt.fold (Set.empty[String]) (_.rcvrIds),
                        pwOpt, request, srm
                      )
                      block(req1)
                    }
                }

              // should never occur
              case None =>
                val msg = "Ad producer node not exist, but it should."
                error(s"ISE: adId=$adId producerId=${mad.producerId} :: $msg")
                Future successful Results.InternalServerError(msg)
            }

          case None =>
            debug(s"ad not found: adId = " + adId)
            notFoundFut
        }

      case None =>
        trace(s"User is NOT logged in: " + request.remoteAddress)
        IsAuth.onUnauth(request)
    }
  }
}
/**
 * Реализация [[AdvWndAccessBase]] с поддержкой таймаута сессии.
 * @param adId id рекламной карточки.
 * @param povAdnId Опциональная точка зрения на карточку.
 */
case class AdvWndAccess(adId: String, povAdnId: Option[String], needMBB: Boolean)
  extends AdvWndAccessBase
  with ExpireSession[AdvWndRequest]


case class AdvWndRequest[A](
  mad: MAd,
  producer: MAdnNode,
  rcvrOpt: Option[MAdnNode],
  isProducerAdmin: Boolean,
  isRcvrAdmin: Boolean,
  mdrOpt: Option[MAdnNode],
  rcvrIds: Set[String],
  pwOpt: PwOpt_t,
  request: Request[A],
  sioReqMd: SioReqMd
) extends AbstractRequestWithPwOpt(request) {
  def isRcvrAccess = rcvrOpt.isDefined || mdrOpt.isDefined
}


/** Контейнер для данных результата работы метода [[AdvWndAccess]].findRcvrMdr(). */
sealed case class PovRcvrInfo(
  node: MAdnNode,
  rcvrIds: Set[String],
  isNodeAdm: Boolean,
  isMdr: Boolean
) {
  override def toString: String = {
    s"PovRcvrInfo(MAdnNode(${node.id.get}), rcvrs=[${rcvrIds.mkString(",")}], isMdr=$isMdr, isNodeAdm=$isNodeAdm)"
  }
}
