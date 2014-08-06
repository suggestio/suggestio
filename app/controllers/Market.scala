package controllers

import play.twirl.api.HtmlFormat
import util.billing.StatBillingQueueActor
import util._
import util.acl._
import views.html.market._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import SiowebEsUtil.client
import io.suggest.ym.model.stat.{MAdStat, AdStatActions}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.14 11:37
 * Description: sio-market controller. Сюда попадают всякие экшены, которые относятся к маркету, но пока
 * не доросли до отдельных контроллеров.
 */

object Market extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  // статистка

  /** Кем-то просмотрена одна рекламная карточка. */
  def adStats(martId: String, adId: String, actionRaw: String) = MaybeAuth.apply { implicit request =>
    val action = AdStatActions.withName(actionRaw)
    MAd.getById(adId).map { madOpt =>
      madOpt.filter { mad =>
        mad.receivers.valuesIterator.exists(_.receiverId == martId)
      } foreach { mad =>
        StatBillingQueueActor.sendNewStats(rcvrId = martId, mad = mad, action = action)
        val adStat = MAdStat(
          clientAddr = request.remoteAddress,
          action = action,
          ua = request.headers.get(USER_AGENT),
          adId = adId,
          adOwnerId = mad.producerId,
          personId = request.pwOpt.map(_.personId)
        )
        adStat.save
      }
    }
    NoContent
  }


  /**
   * Раздача страниц с текстами договоров sio-market.
   * @param clang Язык договора.
   * @return 200 Ok
   *         404 если текст договора не доступен на указанном языке.
   */
  def contractOfferText(clang: String) = MaybeAuth { implicit request =>
    import views.html.market.contract._
    val clangNorm = clang.toLowerCase.trim
    val ctx = implicitly[Context]
    val textRenderOpt: Option[(HtmlFormat.Appendable, String)] = if (clangNorm startsWith "ru") {
      val render = textRuTpl()(ctx)
      Some(render -> "ru")
    } else {
      None
    }
    textRenderOpt match {
      case Some((render, clang2)) =>
        val fullRender = contractBase(clang2)(render)(ctx)
        Ok(fullRender)
      case None =>
        http404ctx(ctx)
    }
  }


  /** Статическая страничка, описывающая суть sio market для владельцев WiFi. */
  def aboutMarket = MaybeAuth { implicit request =>
    Ok(aboutTpl())
  }

  /** Статическая страничка, описывающая суть sio market для рекламодателей. */
  def aboutForAdMakers = MaybeAuth { implicit request =>
    Ok(aboutForAdMakersTpl())
  }

  /** Выдать страницу с вертикальной страницой-презенташкой sio-маркета. */
  def marketBooklet = MaybeAuth { implicit request =>
    Ok(marketBookletTpl())
  }

}

