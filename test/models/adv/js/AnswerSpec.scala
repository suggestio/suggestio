package models.adv.js

import functional.OneAppPerSuiteNoGlobalStart
import org.scalatestplus.play._
import play.api.libs.json.Json

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.09.15 9:52
 * Description: Тесты для модели MAnswer. Интересует в первую очередь десериализация.
 */
class AnswerSpec extends PlaySpec with OneAppPerSuiteNoGlobalStart {

  /** Пример ответа от js-части. */
  private def ANSWER_FB1 = {
    """{"c":"6216602119439899191",
       "d":{"a":"a","f":{"name":"fb","appId":"1588523678029765"},
         "e":["facebook.com"],"d":"success",
         "h":{"p":["user_photos","manage_pages","publish_pages","publish_actions","public_profile"]},
         "i":[{"id":"AU-wyWvYPyNAHOX_buBC","url":"https://facebook.com/me","href":"http://www.suggest.io/?m.id=-vr-hrgNRd6noyQ3_teu_A","name":"fb me",
         "custom":{"i":"1376642379315624","t":"user"}}]}}"""
  }


  "MAnswer.unapply()" must {

    "Parse raw real-word answer using play.json.Reads" in {
      val jsResult = Json.parse(ANSWER_FB1).validate[Answer]
      assert(jsResult.isSuccess, jsResult)
    }

  }

}
