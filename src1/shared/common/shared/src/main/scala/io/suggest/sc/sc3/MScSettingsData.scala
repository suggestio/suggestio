package io.suggest.sc.sc3

import japgolly.univeq._
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.12.2020 13:37
  * Description: Контейнер хранения настроек выдачи.
  *
  * Используется максимально гибкий JSON-формат через Map[String, JSON], для удобства поддержки.
  */
object MScSettingsData {

  def empty = apply()

  implicit def scSettingsJson: OFormat[MScSettingsData] = {
    (__ \ "d")
      .formatNullable[JsObject]
      .inmap[MScSettingsData](
        m => apply( m getOrElse JsObject.empty ),
        jsObj => Option.when( jsObj.data.value.nonEmpty )( jsObj.data ),
      )
  }

  @inline implicit def univEq: UnivEq[MScSettingsData] = UnivEq.derive


  def data = GenLens[MScSettingsData]( _.data )

}


/** Контейнер модели сеттингов.
  *
  * @param data Ассоциативный массив данных.
  */
final case class MScSettingsData(
                                  data        : JsObject        = JsObject.empty,
                                )
