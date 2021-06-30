package models.msc

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumJvmUtil
import io.suggest.xplay.qsb.CrossQsBindable
import japgolly.univeq.UnivEq
import play.api.mvc.{PathBindable, QueryStringBindable}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.04.15 17:30
 * Description: controller-модель с вариантами рендера одиночной карточки.
 */
object OneAdRenderVariants extends StringEnum[OneAdRenderVariant] {

  /** Планируется рендер в HTML. */
  case object ToHtml extends OneAdRenderVariant("h") {
    override def isToImage = false
    override def nameI18n = "HTML"
  }

  /** Планируется рендер в картинку. */
  case object ToImage extends OneAdRenderVariant("i") {
    override def isToImage = true
    override def nameI18n = "Image"
  }

  override def values = findValues

}


/** Экземпляр модели.
  * @param value Ключ экземпляра модели.
  */
sealed abstract class OneAdRenderVariant(override val value: String) extends StringEnumEntry {

  /** Рендерим в картинку? */
  def isToImage: Boolean

  /** Описавающий код i18n. */
  def nameI18n: String

}

object OneAdRenderVariant {

  @inline implicit def univEq: UnivEq[OneAdRenderVariant] = UnivEq.derive

  implicit def oarvQsb: CrossQsBindable[OneAdRenderVariant] =
    EnumeratumJvmUtil.valueEnumQsb( OneAdRenderVariants )

  implicit def oarvPb: PathBindable[OneAdRenderVariant] =
    EnumeratumJvmUtil.valueEnumPb( OneAdRenderVariants )

}
