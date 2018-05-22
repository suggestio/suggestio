package io.suggest.dev

import io.suggest.common.geom.d2.ISize2di
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.text.parse.ParserUtil.Implicits._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 17:19
 * Description: Модель данных по экрану устройства.
 */

object MScreen {

  def maybeFromString(s: String): Either[String, MScreen] = {
    val p = new DevScreenParsersImpl
    p.parse( p.devScreenP, s )
      .toEither
  }

  implicit def mScreenFormat: Format[MScreen] = {
    implicitly[Format[String]]
      .inmap[MScreen](
        maybeFromString(_).right.get,
        _.toQsValue
      )
  }

  def roundPxRatio(pxRatioRaw: Double): Double = {
    // Коэффициент недоскругления, точность до 0.1 - достаточная.
    val r = 10
    Math.round(pxRatioRaw * r).toDouble / r
  }

  implicit def univEq: UnivEq[MScreen] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

  def default = MScreen(
    width   = 1024,
    height  = 768,
    pxRatio = MPxRatios.default
  )

}


/**
 * Данные по экрану.
 * @param width Ширина в css-пикселях.
 * @param height Высота в css-пикселях.
 * @param pxRatio Плотность пикселей.
 */
case class MScreen(
                  // TODO Сделать поле wh, убрать top-level поля w/h
                    override val width    : Int,
                    override val height   : Int,
                    pxRatio               : MPxRatio
                  )
  extends ISize2di
{

  /** Сериализовать для передачи на сервер. */
  final def toQsValue: String = {
    // Округлять pxRatio до первого знака после запятой:
    width.toString +
      PicSzParsers.WH_DELIM + height.toString +
      PicSzParsers.IMG_RES_DPR_DELIM + pxRatio.pixelRatio
    // TODO Надо использовать pxRatio.value
  }

  override final def toString: String = toQsValue

  def withPxRatio(pxRatio: MPxRatio) = copy(pxRatio = pxRatio)

}

