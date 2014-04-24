package controllers

import util.{Context, PlayMacroLogsImpl}
import util.FormUtil._
import play.api.data._, Forms._
import util.acl._
import models._
import com.typesafe.plugin.{use, MailerPlugin}
import views.html.market.lk.adn._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import play.api.Play.current
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.04.14 18:31
 * Description: Контроллер для взаимодействия с рекламными карточками подчинённых узлов.
 * Это всё относится к узлам-супервизорам.
 */
object MarketLkAdnSlaveAd extends SioController with PlayMacroLogsImpl {

  import LOGGER._


  object HideShopAdActions extends Enumeration {
    type HideShopAdAction = Value
    // Тут пока только один вариант отключения карточки. Когда был ещё и DELETE.
    // Потом можно будет спилить варианты отключения вообще, если не понадобятся.
    val HIDE = Value

    def maybeWithName(n: String): Option[HideShopAdAction] = {
      try {
        Some(withName(n))
      } catch {
        case e: Exception => None
      }
    }
  }

  import HideShopAdActions.HideShopAdAction

  /** Форма сокрытия рекламы подчинённого магазина. */
  val shopAdHideFormM = Form(tuple(
    "action" -> nonEmptyText(maxLength = 10)
      .transform(
        strTrimF andThen { _.toUpperCase } andThen HideShopAdActions.maybeWithName,
        {aOpt: Option[HideShopAdAction] => (aOpt getOrElse "").toString }
      )
      .verifying("hide.action.invalid", { _.isDefined })
      .transform(
        _.get,
        { hsaa: HideShopAdAction => Some(hsaa) }
      ),
    "reason" -> hideEntityReasonM
  ))


  /** Рендер формы сокрытия какой-то рекламы. */
  def showSlaveAd(adId: String) = CanViewSlaveAd(adId).apply { implicit request =>
    import request.mad
    Ok(_showSlaveAdTpl(request.slaveNode, mad, request.supNode, shopAdHideFormM))
  }

  /** Сабмит формы сокрытия/удаления формы. */
  def slaveAdHideFormSubmit(adId: String) = CanSuperviseSlaveAd(adId).async { implicit request =>
    import request.mad
    val rdr = Redirect(routes.MarketLkAdn.showSlave(request.mad.producerId))
    shopAdHideFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"shopAdHideFormSubmit($adId): Form bind failed: ${formatFormErrors(formWithErrors)}")
        rdr.flashing("error" -> "Необходимо указать причину")
      },
      {case (HideShopAdActions.HIDE, reason) =>
        mad.receivers = Map.empty
        mad.saveReceivers map { _ =>
          // Отправить письмо магазину-владельцу рекламы
          notyfyAdDisabled(reason)
          rdr.flashing("success" -> "Объявление выключено")
        }
      }
    )
  }

  /**
   * Сообщить владельцу магазина, что его рекламу отключили.
   * @param reason Указанная причина отключения.
   */
  private def notyfyAdDisabled(reason: String)(implicit request: RequestForSlaveAd[_]) {
    import request.{slaveNode, mad, supNode}
    slaveNode.mainPersonId.foreach { personId =>
      MPersonIdent.findAllEmails(personId) onSuccess { case emails =>
        if (emails.isEmpty) {
          warn(s"notifyAdDisabled(${mad.id.get}): No notify emails found for shop ${slaveNode.id.get}")
        } else {
          val mail = use[MailerPlugin].email
          mail.setSubject("Suggest.io | Отключена ваша рекламная карточка")
          mail.setFrom("no-reply@suggest.io")
          mail.setRecipient(emails : _*)
          val ctx = implicitly[Context]   // Нано-оптимизация: один контекст для обоих рендеров.
          mail.send(
            // TODO Вынести письма за пределы shop.
            bodyHtml = views.html.market.lk.shop.ad.emailAdDisabledByMartTpl(supNode, slaveNode, mad, reason)(ctx),
            bodyText = views.txt.market.lk.shop.ad.emailAdDisabledByMartTpl(supNode, slaveNode, mad, reason)(ctx)
          )
        }
      }
    }
  }


  /**
   * Отобразить обрубок с подчинёнными рекламными карточками.
   * @param adnId id подчинённого узла.
   */
  def _showSlaveAds(adnId: String) = CanViewSlave(adnId).async { implicit request =>
    MAd.findForProducer(adnId) map { mads =>
      Ok(_node._slaveNodeAdsTpl(
        msup = request.supNode,
        mslave = request.slaveNode,
        mads = mads
      ))
    }
  }



  // Фунционал для публикации рекламной карточки на иных узлах: окошко, сабмит, поиск.
  import publish._

  /** Рендер диалога со списком узлов, пригодных для публикации рекламной карточки. */
  def adPublishDialog(adId: String) = CanSuperviseSlaveAd(adId).async { implicit request =>
    import request.{supNode, mad}
    val supId = supNode.id.get
    MAdnNode.findBySupId(supId) map { slaves =>
      val slaves1 = if (supNode.adn.isReceiver) {
        supNode :: slaves.toList
      } else {
        slaves
      }
      Ok(_adPublishDialogTpl(mad, slaves1))
    }
  }

  /** Самбмит обновлённого списка узлов, на которых надо опубликовать рекламную карточку. */
  def adPublishDialogSubmit(adId: String) = CanSuperviseSlaveAd(adId).async(parse.urlFormEncoded) { implicit request =>
    import request.mad
    // Процессим POST без маппингов - так проще в данном случае.
    val sls = Set(AdShowLevels.LVL_START_PAGE)
    mad.receivers = request.body("node")
      .foldLeft [List[(String, AdReceiverInfo)]] (Nil) { (acc, rcvrId) =>
        val ari0 = mad.receivers.get(rcvrId)
          .map { rcvr =>
            AdReceiverInfo(rcvrId,  slsWant = rcvr.slsWant ++ sls,  slsPub = rcvr.slsPub ++ sls)
          }
          .getOrElse {
            AdReceiverInfo(rcvrId, sls, sls)
          }
        rcvrId -> ari0 :: acc
      }
      .toMap
    mad.saveReceivers.map { _ =>
      val reply = JsObject(Seq(
        "status" -> JsString("ok")
      ))
      Ok(reply)
    }
  }

}
