package util.xplay

import io.suggest.es.model.EsModelDi
import io.suggest.model.n2.node.{IMNodes, MNode, MNodeTypes}
import models.mproj.IMCommonDi
import play.api.i18n.{I18nSupport, Lang}
import play.api.mvc.Result

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.03.15 18:39
 * Description: Утиль для поддержки языков.
 */
trait SetLangCookieUtil
  extends I18nSupport
  with IMCommonDi
  with EsModelDi
  with IMNodes
{

  import mCommonDi._
  import esModel.api._

  /** Выставить lang.cookie. */
  def setLangCookie1(resFut: Future[Result], personId: String): Future[Result] = {
    setLangCookie2(
      resFut,
      mNodes
        .getByIdCache(personId)
        .withNodeType(MNodeTypes.Person)
    )
  }

  def setLangCookie2(resFut: Future[Result], mpersonOptFut: Future[Option[MNode]]): Future[Result] = {
    mpersonOptFut.flatMap { mpersonOpt =>
      setLangCookie3(resFut, mpersonOpt)
    }
  }

  def setLangCookie3(resFut: Future[Result], mpersonOpt: Option[MNode]): Future[Result] = {
    val langOpt = mpersonOpt.map(_.meta.basic.lang)
    setLangCookie4(resFut, langOpt)
  }

  def setLangCookie4(resFut: Future[Result], langCodeOpt: Option[String]): Future[Result] = {
    val langOpt = langCodeOpt.flatMap(Lang.get)
    setLangCookie5(resFut, langOpt)
  }

  def setLangCookie5(resFut: Future[Result], langOpt: Option[Lang]): Future[Result] = {
    langOpt match {
      case Some(lang) =>
        resFut.map { _.withLang(lang)(mCommonDi.messagesApi) }
      case None =>
        resFut
    }
  }

}
