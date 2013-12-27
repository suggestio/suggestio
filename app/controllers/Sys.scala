package controllers

import play.api.data._
import play.api.data.Forms._
import util.acl.IsSuperuser
import util.FormUtil._
import views.html.sys1._
import play.api.libs.concurrent.Execution.Implicits._
import models._
import util.{SiobixClient, Logs, DomainManager}
import scala.concurrent.Future
import SiobixClient.askTimeout
import io.suggest.proto.bixo._
import io.suggest.model.{MVirtualIndexVin, MDVIActive}
import io.suggest.util.VirtualIndexUtil
import util.SiowebEsUtil.client
import util.urls_supply.SeedUrlsSupplier

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.13 18:14
 * Description: Административный доступ к системной панели suggest.io. Не для обычных юзеров, а только внутреннее использование.
 * В прошлой версии sioweb был /db/ контроллер для этого. Для экшенов этого контроллера всегда используется isSuperuser.
 */

object Sys extends SioController with Logs {

  import LOGGER._

  /** Маппинг формы добавления сайта. */
  val addSiteFormM = Form("domain" -> domain2dkeyMapper)


  /** index.html для системной панели. */
  def index = IsSuperuser { implicit request =>
    Ok(indexTpl())
  }


  /** Список всех доменов (всех ключей доменов). */
  def dkeysListAll = IsSuperuser.async { implicit request =>
    MDomain.getAll.map { list =>
      Ok(domain.dkeysListAllTpl(list))
    }
  }


  /** Страница "обзор домена" со общей информацией. */
  def dkeyShow(dkey: String) = IsSuperuser.async { implicit request =>
    MDomain.getForDkey(dkey) flatMap {
      case Some(mdomain) =>
        mdomain.allPersonAuthz map { authzList =>
          Ok(domain.dkeyShowTpl(mdomain, authzList))
        }

      case None => NotFound("No such domain: " + dkey)
    }
  }


  /**
   * Отрендерить страницу поиска по домену.
   * @param dkey Ключ домена.
   * @return Форма для имитации живого поиска на сайте.
   */
  def dkeySearch(dkey: String) = IsSuperuser.async { implicit request =>
    MDomain.getForDkey(dkey: String) map {
      case Some(mdomain) => Ok(domain.dkeySearchTpl(dkey))
      case None          => NotFound("No such domain: " + dkey)
    }
  }


  /** Рендер формы добавления домена. */
  def addSiteForm = IsSuperuser { implicit request =>
    Ok(addSite.addSiteFormTpl(addSiteFormM))
  }

  /** Сабмит формы, отрендеренной в addSiteForm(). */
  def addSiteFormSubmit = IsSuperuser.async { implicit request =>
    lazy val logPrefix = "addSiteFormSubmit(): "
    addSiteFormM.bindFromRequest.fold(
      {formWithErrors =>
        trace(logPrefix + "form parse failed: " + formWithErrors.errors)
        BadRequest(formWithErrors.errorsAsJson)
      }
      ,
      {dkey =>
        trace(logPrefix + "POST parsed. dkey found = " + dkey)
        val addedBy = request.pwOpt.get.id + " (без проверки)"
        DomainManager.installDkey(dkey=dkey, addedBy=addedBy) map { result =>
          val msg = result match {
            case Some(crawlerRef) => "Domain already in crawler: " + crawlerRef
            case None             => "Crawler successfully notified about new domain."
          }
          info(logPrefix + msg)
          Ok(addSite.addSiteSuccessTpl(dkey, result.map(_.toString())))
        }
      }
    )
  }


  /** Удаление домена из системы. */
  def dkeyDelete(dkey: String) = IsSuperuser { implicit request =>
    ???
  }


  /** Запрос major rebuild. Он направляется в main-кравлер. */
  def majorRebuild = IsSuperuser.async { implicit request =>
    SiobixClient.majorRebuildRequest map {
      case Left(reason) => BadRequest(reason)
      case Right(msg)   => Ok(msg)
    }
  }


  /** Отрендерить страницу индекса в разделе siobix. */
  def siobixIndex = IsSuperuser { implicit request =>
    Ok(siobix.indexTpl())
  }

  /** Отрендерить страницу с управлением ребилда. */
  def siobixRebuild = IsSuperuser { implicit request =>
    Ok(siobix.rebuildTpl())
  }


  /** Гуляем по дереву акторов (TalkitiveActor) в siobix. */
  def siobixActor(path: String) = IsSuperuser.async { implicit request =>
    val logPrefix = s"siobixActor($path): "
    val actorPath = if (path startsWith "/") path else "/" + path
    val sel = SiobixClient.remoteAskSelection("/user" + actorPath)
    trace(logPrefix + s"actorPath -> $actorPath ;; asking selection -> ${sel.actorSel}")
    val childFut = (sel ? TA_GetDirectChildren).asInstanceOf[Future[List[String]]]
    for {
      statusJson <- (sel ? TA_GetStatusReport).asInstanceOf[Future[String]]
      children   <- childFut
    } yield {
      trace(logPrefix + "statusJson -> " + statusJson)
      trace(logPrefix + "children -> " + children)
      val slashlessPath = if (path startsWith "/") path.tail else path
      Ok(siobix.showActorTpl(slashlessPath, statusJson, children))
    }
  }


  // ============================================================
  // Текущие индексы (просмотр конфигурации виртуальных индексов)

  def indicesIndex = IsSuperuser { implicit request =>
    Ok(indices.indexTpl())
  }

  /** Листинг всех MDVIActive. */
  def indicesListAllActive = IsSuperuser.async { implicit request =>
    MDVIActive.getAll.map { l =>
      trace("All MDVIActive found: " + l.size)
      Ok(indices.listAllMdviActiveTpl(l))
    }
  }

  /** Выдать данные по вирт.индексу в домене.
    * @param dkey Ключ домена
    * @param vin виртуальный индекс.
    */
  def indicesShowActiveFor(dkey:String, vin:String) = IsSuperuser.async { implicit request =>
    MDVIActive.getForDkeyVin(dkey=dkey, vin=vin) map {
      case Some(mdviActive) => Ok(indices.showMdviActiveTpl(mdviActive))
      case None             => NotFound(s"No virtual index '$vin' for dkey '$dkey'")
    }
  }

  /** Выдать данные по vin'у. */
  def showVin(vin: String) = IsSuperuser.async { implicit request =>
    val mvi = new MVirtualIndexVin(vin)
    mvi.getUsers.map { l =>
      Ok(indices.showMviTpl(mvi, l))
    }
  }

  /** Запрос на downgrade всех индексов. */
  def indicesDowngradeAll = IsSuperuser.async { implicit request =>
    // TODO Нужно отправлять запрос в main-кравлер, чтобы тот делал всё это если можно. На время начальной разработки, этот функционал тут, хоть и конфликтует с majorRebuild().
    VirtualIndexUtil.downgradeAll.map { _ =>
      Redirect(routes.Sys.indicesListAllActive())
    }
  }


  // ==============================================================
  // referrers


  val pushRefFormM = Form(tuple(
    "refUrl"    -> nonEmptyText,
    "isHiPrio"  -> boolean
  ))


  /** Отрендерить клиенту форму отправки произвольного реферрера в MainCrawler. */
  def pushReferrerForm = IsSuperuser { implicit request =>
    Ok(siobix.pushReferrerFormTpl(pushRefFormM))
  }

  /** Сабмит вышеотрендеренной формы. */
  def pushReferrerFormSubmit = IsSuperuser { implicit request =>
    pushRefFormM.bindFromRequest.fold(
      {formWithErrors =>
        NotAcceptable(siobix.pushReferrerFormTpl(formWithErrors, withResult=Some(false)))
      },
      {case (refUrl, isHiPrio) =>
        if (isHiPrio) {
          SeedUrlsSupplier.sendReferrerNow(refUrl)
        } else {
          SeedUrlsSupplier.sendReferrer(refUrl)
        }
        Ok(siobix.pushReferrerFormTpl(pushRefFormM, withResult=Some(true)))
      }
    )
  }

}
