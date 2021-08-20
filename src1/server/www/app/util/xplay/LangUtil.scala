package util.xplay

import com.google.inject.Inject
import io.suggest.n2.node.MNode
import play.api.i18n.Lang
import play.api.mvc.Result
import util.acl.SioControllerApi

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.03.15 18:39
 * Description: Утиль для поддержки языков.
 */
final class LangUtil @Inject()(
                                sioControllerApi: SioControllerApi,
                              ) {

  import sioControllerApi._

  def setLangCookie2(resFut: Future[Result], mpersonOptFut: Future[Option[MNode]]): Future[Result] = {
    mpersonOptFut.flatMap { mpersonOpt =>
      getLangFrom( mpersonOpt ).fold(resFut) { lang =>
        for (res0 <- resFut) yield
          res0.withLang(lang)(messagesApi)
      }
    }
  }
  def setLangCookie(res: Result, langOpt: Option[Lang]): Result = {
    langOpt.fold(res) { lang =>
      res.withLang(lang)(messagesApi)
    }
  }

  def getLangFrom(mpersonOpt: Option[MNode]): Option[Lang] = {
    val langCodeOpt = mpersonOpt.map(_.meta.basic.lang)
    langCodeOpt.flatMap(Lang.get)
  }

}
