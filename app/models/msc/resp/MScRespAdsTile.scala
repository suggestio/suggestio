package models.msc.resp

import io.suggest.sc.ScConstants.Resp._
import models.msc.{MFoundAd, MGridParams}
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.05.15 16:02
 * Description: Модель для представления ответа на запрос findAds от выдачи.
 * Модель появилась в API v2+, до этого была пачка голого js, который рендерился сервером и
 * неспеша сам распихивал информацию модели на клиенте.
 */
object MScRespAdsTile {

  /** Сериализация в JSON. */
  implicit val WRITES: OWrites[MScRespAdsTile] = (
    (__ \ MADS_FN).write[Seq[MFoundAd]] and
    (__ \ CSS_FN).writeNullable[String] and
    (__ \ PARAMS_FN).writeNullable[MGridParams]
  )(unlift(unapply))

}


/**
 * Экземпляр модели ответов на findAds v2 запросы.
 * @param mads Отрендеренные карточки для .
 * @param css Отрендеренные css-стили для отрендеренных карточек, если есть.
 * @param params Параметры отображения, если необходимо перевыставить.
 */
case class MScRespAdsTile(
  mads    : Seq[MFoundAd],
  css     : Option[String],
  params  : Option[MGridParams]
)
