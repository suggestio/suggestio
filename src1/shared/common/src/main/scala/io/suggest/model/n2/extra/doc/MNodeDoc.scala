package io.suggest.model.n2.extra.doc

import io.suggest.jd.tags.JsonDocument
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 15:55
  * Description: Кросс-платформенная модель данных по рекламной карточки второй поколения.
  *
  * Эта модель подразумевает использование JSON-документа и эджи вместо списка текстовых полей
  * с текстом, графикой и всем остальным.
  *
  * Явно непустая-модель, подразумевается использование Option[MNodeAd2] для пустой модели.
  *
  * Модель используется в первую очередь на сервере внутри mnode.extras.ad.
  */
object MNodeDoc {

  /** Модель названий полей модели [[MNodeDoc]]. */
  object Fields {

    val TEMPLATE_FN = "d"

  }


  /** Поддержка play-json. */
  implicit val MNODE_AD2_FORMAT: OFormat[MNodeDoc] = {
    (__ \ Fields.TEMPLATE_FN).format[JsonDocument]
      .inmap[MNodeDoc](apply, _.template)
  }

}


/** Класс модели данных по узлу, который является рекламной карточкой.
  *
  * @param template JSON-Document, т.е. шаблон, описывающий рендер какого-то внешнего контента.
  *                 Сам контент является внешним по отношению к шаблону.
  *                 В узле контент представлен эджами, которые слинкованы с документом по предикатам и/или edge-uid'ам.
  */
case class MNodeDoc(
                     template: JsonDocument
                   )
