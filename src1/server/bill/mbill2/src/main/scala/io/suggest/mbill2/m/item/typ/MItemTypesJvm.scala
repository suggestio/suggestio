package io.suggest.mbill2.m.item.typ

import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.12.15 12:50
  * Description: JVM поддержки статической модели типов item'ов заказа.
  * Сама модель уехала в common в одноимённый класс (без суффикса Jvm).
  */
object MItemTypesJvm {

  /** Поддержка маппинга для play router. */
  implicit def mitQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[MItemType] = {
    new QueryStringBindableImpl[MItemType] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MItemType]] = {
        for {
          maybeStrId <- strB.bind(key, params)
        } yield {
          maybeStrId.right.flatMap { strId =>
            MItemTypes.withNameOption(strId)
              .toRight("error.invalid")
          }
        }
      }
      override def unbind(key: String, value: MItemType): String = {
        strB.unbind(key, value.strId)
      }
    }
  }

}



