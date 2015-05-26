package models.msc

import io.suggest.sc.ScConstants.Resp.HTML_FN

import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.05.15 16:15
 * Description: В v2 API было решено, что нужно в findAds нужно возвращать не массив из html-рендеров, а
 * массив json'ов с полем html. Это предоставит возможность добавлять поля и менять формат ответа.
 */
object MFoundAd {

  implicit def writes: Writes[MFoundAd] = {
    new Writes[MFoundAd] {
      override def writes(o: MFoundAd): JsValue = {
        JsObject(Seq(
          HTML_FN -> JsString(o.html)
        ))
      }
    }
  }

}


case class MFoundAd(
  html: String
)
