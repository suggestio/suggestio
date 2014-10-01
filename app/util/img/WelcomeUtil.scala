package util.img

import controllers.MarketShowcase

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

  val welcomeImgIdKM = "welcomeImgId" -> optional(ImgFormUtil.imgIdM)

  /** Асинхронно получить welcome-ad-карточку. */
  def getWelcomeAdOpt(welcomeAdId: Option[String]): Future[Option[MWelcomeAd]] = {
    welcomeAdId
      .fold [Future[Option[MWelcomeAd]]] (Future successful None) (MWelcomeAd.getById)
  }
  def getWelcomeAdOpt(node: MAdnNode): Future[Option[MWelcomeAd]] = {
    getWelcomeAdOpt( node.meta.welcomeAdId )
  }

  def updateWaImg(waOpt: Option[MWelcomeAd], newWaImgOpt: Option[ImgIdKey]) = {
    ImgFormUtil.updateOrigImg(
      needImgs = newWaImgOpt.map(ImgInfo4Save(_, withThumb = false)).toSeq,
      oldImgs = waOpt.flatMap(_.imgs.headOption).map(_._2).toIterable
    )
  }

  /** Обновление картинки и карточки приветствия. Картинка хранится в полу-рекламной карточке, поэтому надо ещё
    * обновить карточку и пересохранить её. */
  def updateWelcodeAdFut(adnNode: MAdnNode, newWelcomeImgOpt: Option[ImgIdKey]): Future[Option[String]] = {
    getWelcomeAdOpt(adnNode) flatMap { currWelcomeAdOpt =>
      updateWaImg(currWelcomeAdOpt, newWelcomeImgOpt) flatMap {
        // Новой картинки нет. Надо удалить старую карточку (если была), и очистить соотв. welcome-поле.
        case None =>
          adnNode.meta
            .welcomeAdId
            .fold [Future[Option[String]]]
              { Future successful None }
              { waId => MWelcomeAd.deleteById(waId).map { _ => None } }

        // Новая картинка есть. Пора обновить текущую карточук, или новую создать.
        case newImgInfoOpt @ Some(newImgInfo) =>
          val newWelcomeAd = updateWaOptWith(currWelcomeAdOpt, newImgInfo, adnNode.id.get)
          newWelcomeAd.save
            .map { Some.apply }
      }
    }
  }


  /**
   * Обновление указанной рекламной карточки без сайд-эффектов. Если картинки нет, то и карточки на выходе не будет.
   * @param waOpt Исходная рекламная карточка, если есть.
   * @param newImgOpt Новая картинка, если есть.
   * @param newProducerId producerId если понадобится его выставить.
   * @return Опциональный результат в виде экземпляра MWA (новой или на основе исходной waOpt).
   */
  def updateWaOptAdHoc(waOpt: Option[MWelcomeAd], newImgOpt: Option[MImgInfoT], newProducerId: String): Option[MWelcomeAd] = {
    newImgOpt map { newImg =>
      updateWaOptWith(waOpt, newImg, newProducerId)
    }
  }

  def updateWaOptWith(waOpt: Option[MWelcomeAd], newImg: MImgInfoT, newProducerId: String): MWelcomeAd = {
    val newImgs = Map(WELCOME_IMG_KEY -> newImg)
    waOpt.fold
      { MWelcomeAd(producerId = newProducerId, imgs = newImgs) }
      { _.copy(imgs = newImgs) }
  }


  def welcomeAd2iik(waOpt: Option[MWelcomeAd]) = {
    waOpt
      .flatMap { _.imgs.headOption }
      .map[OrigImgIdKey] { img => img._2 }
  }


  /**
   * Асинхронно собрать аргументы для рендера карточки приветствия.
   * @param adnNode Узел, для которого нужно подготовить настройки рендера приветствия.
   * @return Фьючерс с опциональными настройками. Если None, то приветствие рендерить не надо.
   */
  def getWelcomeRenderArgs(adnNode: MAdnNode): Future[Option[WelcomeRenderArgsT]] = {
    val welcomeAdOptFut = adnNode.meta
      .welcomeAdId
      .fold (Future successful Option.empty[MWelcomeAd]) (MWelcomeAd.getById)
    // Получить параметры (метаданные) фоновой картинки из хранилища картирок.
    val bgFut = adnNode.gallery
      .headOption
      .fold[Future[Either[String, MImgInfoT]]] {
        Future successful colorBg(adnNode)
      } { bgImgFilename =>
        val oiik = OrigImgIdKey.apply(bgImgFilename)
        oiik.getImageWH map {
          case metaSome if metaSome.nonEmpty =>
            Right(oiik.copy(meta = metaSome))
          case _ => colorBg(adnNode)
        }
      }
    for {
      welcomeAdOpt <- welcomeAdOptFut
      bg1          <- bgFut
    } yield {
      val wra = new WelcomeRenderArgsT {
        override def bg = bg1
        override def fgImage = welcomeAdOpt.flatMap(_.imgs.get(WELCOME_IMG_KEY))
        override def fgText = Some(adnNode.meta.name)
      }
      Some(wra)
    }
  }

  /** внутренний метод для генерации ответа по фону приветствия в режиме цвета. */
  private def colorBg(adnNode: MAdnNode) = Left(adnNode.meta.color.getOrElse(MarketShowcase.SITE_BGCOLOR_DFLT))

}
