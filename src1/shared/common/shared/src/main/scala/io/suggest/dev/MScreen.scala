package io.suggest.dev

import diode.FastEq
import io.suggest.common.geom.d2.MSize2di
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.text.parse.ParserUtil.Implicits._
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 17:19
 * Description: Модель данных по экрану устройства.
 */

object MScreen {

  implicit object MScreenFastEq extends FastEq[MScreen] {
    override def eqv(a: MScreen, b: MScreen): Boolean = {
      (a.wh ===* b.wh) &&
      (a.pxRatio ===* b.pxRatio)
    }
  }

  def maybeFromString(s: String): Either[String, MScreen] = {
    val p = new DevScreenParsersImpl
    p.parse( p.devScreenP, s )
      .toEither
  }

  implicit def mScreenFormat: Format[MScreen] = {
    implicitly[Format[String]]
      .inmap[MScreen](
        maybeFromString(_).getOrElse(throw new NoSuchElementException("no screen defined")),
        _.toQsValue
      )
  }

  def roundPxRatio(pxRatioRaw: Double): Double = {
    // Коэффициент недоскругления, точность до 0.1 - достаточная.
    val r = 10
    Math.round(pxRatioRaw * r).toDouble / r
  }

  @inline implicit def univEq: UnivEq[MScreen] = UnivEq.derive

  def default = MScreen(
    wh = MSize2di(
      width   = 1024,
      height  = 768,
    ),
    pxRatio = MPxRatios.default
  )

  def wh        = GenLens[MScreen](_.wh)
  def pxRatio   = GenLens[MScreen](_.pxRatio)

}


/**
 * Данные по экрану.
 * @param wh Ширина и высота в css-пикселях.
 * @param pxRatio Плотность пикселей.
 */
case class MScreen(
                    wh                    : MSize2di,
                    pxRatio               : MPxRatio
                  ) {

  /** Сериализовать для передачи на сервер. */
  final def toQsValue: String = {
    // Округлять pxRatio до первого знака после запятой:
    wh.width.toString +
      PicSzParsers.WH_DELIM + wh.height.toString +
      PicSzParsers.IMG_RES_DPR_DELIM + pxRatio.pixelRatio
    // TODO Надо использовать pxRatio.value
  }

  override final def toString: String = toQsValue

  def isHeightEnought: Boolean =
    wh.height >= 300  // TODO допилить попап, авто-теги и тд.
    //false

}

