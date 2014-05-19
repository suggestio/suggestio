package util.blocks

import io.suggest.ym.model.common.BlockMeta
import play.api.data.{Mapping, FormError}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.05.14 22:04
 * Description: Утиль для сборки блоков с heightBf-полем.
 * Жесткий велосипед с height необходим из-за серьезных проблем с расширенияеми маппингов через трейты.
 * Бывают родственные блоки, которые могут иметь или не иметь поля высоты. В таких случаях поле
 * Высоты не рендерится в одном из блоков, но всегда доступно для маппинга.
 */

object Height {
  val BF_HEIGHT_NAME_DFLT = "height"
  val BF_HEIGHT_DFLT = BfHeight(BF_HEIGHT_NAME_DFLT)


  def mergeBindAccWithHeight(maybeAcc: Either[Seq[FormError], BindAcc],
                               offerN: Int,
                               maybeHeight: Either[Seq[FormError], Int]):  Either[Seq[FormError], BindAcc] = {
    (maybeAcc, maybeHeight) match {
      case (Right(acc0), Right(height)) =>
        acc0.height = height
        maybeAcc

      case (Left(accFE), Right(descr)) =>
        maybeAcc

      case (Right(_), Left(colorFE)) =>
        Left(colorFE)   // Избыточна пересборка left either из-за right-типа. Можно также вернуть через .asInstanceOf, но это плохо.

      case (Left(accFE), Left(colorFE)) =>
        Left(accFE ++ colorFE)
    }
  }

}


import Height._


/** Базовый интерфейс для поля heightBf. */
trait HeightI {
  def heightBf: BfHeight
}


/** Если нужно добавление поля в blockFields (т.е. в форму редактора), то нужен этот трейт. */
trait Height extends ValT with HeightI {
  abstract override def blockFieldsRev: List[BlockFieldT] = heightBf :: super.blockFieldsRev

  override def heightBf = Height.BF_HEIGHT_DFLT

  // Mapping
  private def m = heightBf.getStrictMapping.withPrefix(heightBf.name).withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeHeight = m.bind(data)
    mergeBindAccWithHeight(maybeAcc0, offerN = 0, maybeHeight)
  }

  abstract override def unbind(value: BlockMapperResult): Map[String, String] = {
    val v = m.unbind( value.bd.blockMeta.height )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BlockMapperResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = value.bd.blockMeta.height
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }
}


/** Бывает, что блок имеет фиксированную высоту. Тут хелпер, немного экономящий время.
  * Высоту можно переопределить. */
trait HeightFixed extends ValT {
  val HEIGHT = 300
  def getBlockMeta: BlockMeta = getBlockMeta(HEIGHT)
}

