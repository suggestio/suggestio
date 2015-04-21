package models.msc

import controllers.routes
import io.suggest.model.EnumMaybeWithName
import models.blk.OneAdQsArgs
import play.api.mvc.{PathBindable, QueryStringBindable, Call}
import scala.language.implicitConversions

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.04.15 17:30
 * Description: controller-модель с вариантами рендера одиночной карточки.
 */
object OneAdRenderVariants extends Enumeration with EnumMaybeWithName {

  /**
   * Экземпляр модели.
   * @param strId Ключ экземпляра модели.
   */
  abstract protected class Val(val strId: String) extends super.Val(strId) {

    /** routes-вызов для рендера. */
    def routesCall(qsArgs: OneAdQsArgs): Call

    /** Рендерим в картинку? */
    def isToImage: Boolean

    /** Описавающий код i18n. */
    def nameI18n: String
  }

  override type T = Val


  /** Планируется рендер в HTML. */
  val ToHtml: T = new Val("h") {
    override def routesCall(qsArgs: OneAdQsArgs): Call = {
      routes.MarketShowcase.onlyOneAd(qsArgs)
    }

    override def isToImage = false
    override def nameI18n = "HTML"
  }

  /** Планируется рендер в картинку. */
  val ToImage: T = new Val("i") {
    override def routesCall(qsArgs: OneAdQsArgs): Call = {
      routes.MarketShowcase.onlyOneAdAsImage(qsArgs)
    }

    override def isToImage = true
    override def nameI18n = "Image"
  }


  /** Общий код qsb- и pb- мапперов. Вызывается через Either.right.flatMap(). */
  private def binder(varStr: String): Either[String, T] = {
    maybeWithName(varStr) match {
      case Some(v) => Right(v)
      case None    => Left("Unknown rendering variant: " + varStr)
    }
  }

  /** routes qsb для модели. */
  implicit def qsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[T] = {
    new QueryStringBindable[T] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, T]] = {
        strB.bind(key, params).map { maybeVarianStr =>
          maybeVarianStr
            .right
            .flatMap { binder }
        }
      }

      override def unbind(key: String, value: T): String = {
        strB.unbind(key, value.strId)
      }
    }
  }

  /** path binder для маппинга кусков пути URL. */
  implicit def pb(implicit strB: PathBindable[String]): PathBindable[T] = {
    new PathBindable[T] {
      override def bind(key: String, value: String): Either[String, T] = {
        strB.bind(key, value)
          .right
          .flatMap { binder }
      }

      override def unbind(key: String, value: T): String = {
        strB.unbind(key, value.strId)
      }
    }
  }

}
