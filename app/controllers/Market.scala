package controllers

import play.api.i18n.MessagesApi
import util.acl._
import views.html.market._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.14 11:37
 * Description: sio-market controller. Сюда попадают всякие экшены, которые относятся к маркету, но пока
 * не доросли до отдельных контроллеров.
 */

class Market(val messagesApi: MessagesApi) extends SioController {

  /** Статическая страничка, описывающая суть sio market для владельцев WiFi. */
  def aboutMarket = MaybeAuth { implicit request =>
    // 2014.oct.22: Удаление старого about'а, который уже постарел совсем и потерял актуальность.
    //              Буклет о sio-маркете лежит по новой ссылке.
    MovedPermanently( routes.Market.marketBooklet().url )
  }

  /** Статическая страничка, описывающая суть sio market для рекламодателей. */
  def aboutForAdMakers = MaybeAuth { implicit request =>
    cacheControlShort {
      Redirect(routes.Market.marketBooklet().url)
    }
  }

  /** Выдать страницу с вертикальной страницой-презенташкой sio-маркета. */
  def marketBooklet = MaybeAuth { implicit request =>
    cacheControlShort {
      Ok(marketBookletTpl())
    }
  }

}

