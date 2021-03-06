package io.suggest.color

import io.suggest.common.empty.{EmptyProduct, IEmpty}
import io.suggest.err.ErrorConstants
import io.suggest.scalaz.{ScalazUtil, StringValidationNel}
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.functional.syntax._
import play.api.libs.json._
import scalaz.syntax.apply._
import scalaz.{Validation, ValidationNel}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.05.17 15:15
  * Description: Кросс-платформенная модель описания цветовой триады узла.
  *
  * Она имеет вид мапы, но в виде класса, для удобства доступа по ключам.
  * Есть мнение, что это неправильно, и класс нужно заменить на коллекцию.
  */
object MColors extends IEmpty {

  object Fields {
    object Bg {
      def BG_CODE_FN = MColorTypes.Bg.COLOR_CODE_FN
    }
    object Fg {
      def FG_CODE_FN = MColorTypes.Fg.COLOR_CODE_FN
    }
  }


  override type T = MColors

  override val empty = apply()

  /** Поддержка JSON. */
  implicit val MCOLORS_FORMAT: OFormat[MColors] = (
    (__ \ MColorTypes.Bg.value).formatNullable[MColorData] and
    (__ \ MColorTypes.Fg.value).formatNullable[MColorData]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MColors] = UnivEq.derive

  def bgF = { cs: MColors => cs.bg }
  def fgF = { cs: MColors => cs.fg }

  /** Проверка валидности hex-цветов. */
  def validateHexOpt(color: MColors): ValidationNel[String, MColors] = (
    ScalazUtil.liftNelOpt(color.bg)( MColorData.validateHexCodeOnly ) |@|
    ScalazUtil.liftNelOpt(color.fg)( MColorData.validateHexCodeOnly )
  )(apply _)

  /** Проверка, что все цвета выставлены. */
  def validateHexSome(color: MColors): StringValidationNel[MColors] = (
    ScalazUtil.liftNelSome(color.bg, "bg." + ErrorConstants.Words.EXPECTED)( MColorData.validateHexCodeOnly ) |@|
    ScalazUtil.liftNelSome(color.fg, "fg." + ErrorConstants.Words.EXPECTED)( MColorData.validateHexCodeOnly )
  )(apply _)

  /** Костыль: проверка заданных цветов, но если цвета не заданы, то возвращать дефолтовые.
    * Появился тут, т.к. ранее через SysMarket создавались adn-узлы без цветов, но в новой lk-adn-edit форме цвета обязательны.
    * Проблема решается на уровне валидации, подстановкой всегда одинаковых дефолтовых данных на стадии валидации и сохранения.
    */
  def validateOrAdnSome(colors: MColors): StringValidationNel[MColors] = {
    validateHexSome(colors).orElse {
      Validation.success( MColorTypes.scDefaultColors )
    }
  }

  def bg = GenLens[MColors]( _.bg )
  def fg = GenLens[MColors]( _.fg )

  def ofType( mct: MColorType ) = {
    mct match {
      case MColorTypes.Bg => bg
      case MColorTypes.Fg => fg
    }
  }

}


// TODO Вероятно, надо заменить эту модель (или её поля) чем-то типа Map[MColorType, MColorData].

final case class MColors(
                          bg        : Option[MColorData]    = None,
                          fg        : Option[MColorData]    = None
                        )
  extends EmptyProduct
{

  /** Вернуть все перечисленные цвета. */
  def allColorsIter: Iterator[MColorData] = {
    productIterator.collect {
      case Some(mcd: MColorData) => mcd
    }
  }

  def adnColors = bg :: fg :: Nil

}
