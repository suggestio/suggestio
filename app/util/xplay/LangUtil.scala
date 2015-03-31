package util.xplay

import models.usr.MPerson
import play.api.i18n.Lang
import play.api.mvc.Result
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import play.api.Play.current
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.03.15 18:39
 * Description: Утиль для поддержки языков.
 */
object LangUtil {

  /** Выставить lang.cookie. */
  def setLangCookie1(resFut: Future[Result], personId: String): Future[Result] = {
    setLangCookie2(resFut, MPerson.getById(personId))
  }

  def setLangCookie2(resFut: Future[Result], mpersonOptFut: Future[Option[MPerson]]): Future[Result] = {
    mpersonOptFut.flatMap { mpersonOpt =>
      setLangCookie3(resFut, mpersonOpt)
    }
  }

  def setLangCookie3(resFut: Future[Result], mpersonOpt: Option[MPerson]): Future[Result] = {
    val langOpt = mpersonOpt.map(_.lang)
    setLangCookie4(resFut, langOpt)
  }

  def setLangCookie4(resFut: Future[Result], langCodeOpt: Option[String]): Future[Result] = {
    val langOpt = langCodeOpt.flatMap(Lang.get)
    setLangCookie5(resFut, langOpt)
  }

  def setLangCookie5(resFut: Future[Result], langOpt: Option[Lang]): Future[Result] = {
    langOpt match {
      case Some(lang) =>
        resFut.map { _.withLang(lang) }
      case None =>
        resFut
    }
  }

}
