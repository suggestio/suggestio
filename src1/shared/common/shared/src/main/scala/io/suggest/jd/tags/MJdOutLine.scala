package io.suggest.jd.tags

import io.suggest.color.MColorData
import io.suggest.scalaz.ScalazUtil
import japgolly.univeq._
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.ValidationNel

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.12.2020 15:20
  * Description: Модель описания обводки. Хранится как опциональное поле внутри jd props1.
  * Модель содержит опциональные поля, но является явно-пустой (живущей внутри Option[]-поля).
  * Пустота всех полей [[MJdOutLine]] обозначает визуальное отсутствие обводки (обводка без цвета и без всего остального).
  */
object MJdOutLine {

  implicit def outlineJson: OFormat[MJdOutLine] = {
    (__ \ "c")
      .formatNullable[MColorData]
      .inmap[MJdOutLine]( apply, _.color )
  }

  @inline implicit def univEq: UnivEq[MJdOutLine] = UnivEq.derive

  def validate( jdOl: MJdOutLine ): ValidationNel[String, MJdOutLine] = {
    ScalazUtil.liftNelOpt( jdOl.color )( MColorData.validateHexCodeOnly )
      .map( apply )
  }

  def color = GenLens[MJdOutLine]( _.color )

}


/** Модель, описывающая параметры обводки.
  *
  * @param color Цвет обводки.
  */
final case class MJdOutLine(
                             color          : Option[MColorData]          = None,
                           )
