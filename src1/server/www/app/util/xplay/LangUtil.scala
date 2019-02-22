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

  def setLangCookie2(resFut: Future[Result], mpersonOptFut: Future[Option[MNode]]): Future[Result] = {
    mpersonOptFut.flatMap { mpersonOpt =>
      getLangFrom( mpersonOpt ).fold(resFut) { lang =>
        for (res0 <- resFut) yield
          res0.withLang(lang)(mCommonDi.messagesApi)
      }
    }
  }
  def setLangCookie(res: Result, langOpt: Option[Lang]): Result = {
    langOpt.fold(res) { lang =>
      res.withLang(lang)(mCommonDi.messagesApi)
    }
  }

  def getLangFrom(mpersonOpt: Option[MNode]): Option[Lang] = {
    val langCodeOpt = mpersonOpt.map(_.meta.basic.lang)
    langCodeOpt.flatMap(Lang.get)
  }

}
