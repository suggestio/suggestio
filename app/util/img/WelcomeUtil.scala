package util.img

import scala.concurrent.Future
import models._
import play.api.data.Forms._
import ImgFormUtil._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.event.SiowebNotifier.Implicts.sn

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.07.14 10:54
 * Description: Утиль для картинки/карточки приветствия.
 */
object WelcomeUtil {

  /** Ключ для картинки, используемой в качестве приветствия. */
  val WELCOME_IMG_KEY = "wlcm"

  val welcomeImgIdKM = "welcomeImgId" -> optional(ImgFormUtil.imgIdJpegM)

  /** Асинхронно получить welcome-ad-карточку. */
  def getWelcomeAdOpt(welcomeAdId: Option[String]): Future[Option[MWelcomeAd]] = {
    welcomeAdId
      .fold [Future[Option[MWelcomeAd]]] (Future successful None) (MWelcomeAd.getById)
  }
  def getWelcomeAdOpt(node: MAdnNode): Future[Option[MWelcomeAd]] = {
    getWelcomeAdOpt( node.meta.welcomeAdId )
  }

  /** Обновление картинки и карточки приветствия. Картинка хранится в полу-рекламной карточке, поэтому надо ещё
    * обновить карточку и пересохранить её. */
  def updateWelcodeAdFut(adnNode: MAdnNode, newWelcomeImgOpt: Option[ImgIdKey]): Future[Option[String]] = {
    getWelcomeAdOpt(adnNode) flatMap { currWelcomeAdOpt =>
      ImgFormUtil.updateOrigImg(
        needImgs = newWelcomeImgOpt.map(ImgInfo4Save(_, withThumb = false)).toSeq,
        oldImgs = currWelcomeAdOpt.flatMap(_.imgs.headOption).map(_._2).toIterable
      ) flatMap {
        // Новой картинки нет. Надо удалить старую карточку (если была), и очистить соотв. welcome-поле.
        case None =>
          adnNode.meta
            .welcomeAdId
            .fold [Future[Option[String]]]
              { Future successful None }
              { waId => MAd.deleteById(waId).map { _ => None } }

        // Новая картинка есть. Пора обновить текущую карточук, или новую создать.
        case newImgInfoOpt @ Some(newImgInfo) =>
          val newImgs = Map(WELCOME_IMG_KEY -> newImgInfo)
          val newWelcomeAd = currWelcomeAdOpt.fold
            { MWelcomeAd(producerId = adnNode.id.get, imgs = newImgs) }
            { _.copy(imgs = newImgs) }
          newWelcomeAd.save
            .map { Some.apply }
      }
    }
  }


  def welcomeAd2iik(waOpt: Option[MWelcomeAd]) = {
    waOpt
      .flatMap { _.imgs.headOption }
      .map[OrigImgIdKey] { img => img._2 }
  }

}
