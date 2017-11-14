package io.suggest.sc.sc3

import io.suggest.media.IMediaInfo
import io.suggest.model.n2.node.meta.colors.MColors
import io.suggest.sc.index.MWelcomeInfo
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 18:31
  * Description: Кросс-платформенная модель данных по отображаемому узлу для нужд выдачи.
  * Под узлом тут в первую очередь подразумевается узел выдачи, а не конкретная узел-карточка.
  */
object MSc3IndexResp {

  /** Поддержка play-json сериализации. */
  implicit def MSC_NODE_INFO_FORMAT: OFormat[MSc3IndexResp] = (
    (__ \ "a").formatNullable[String] and
    (__ \ "n").formatNullable[String] and
    (__ \ "c").format[MColors] and
    (__ \ "l").formatNullable[IMediaInfo] and
    (__ \ "w").formatNullable[MWelcomeInfo]
  )(apply, unlift(unapply))

}


/** Контейнер данных по узлу в интересах выдачи.
  * На всякий случай, модель максимально толерантна к данными и целиком необязательна.
  *
  * @param nodeId id узла в s.io.
  * @param name Название узла (или текущего метоположения), если есть.
  * @param colors Цвета, если есть.
  * @param logoOpt Данные по логотипу-иллюстрации.
  * @param welcome Данные для рендера экрана приветствия.
  */
case class MSc3IndexResp(
                         nodeId  : Option[String],
                         name    : Option[String],
                         colors  : MColors,
                         logoOpt : Option[IMediaInfo],
                         welcome : Option[MWelcomeInfo]
                       )

