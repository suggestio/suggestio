package io.suggest.n2.edge

import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.primo.id.IId
import io.suggest.text.StringUtil
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.11.2019 12:07
  * Description: Контейнер "флага" с возможными доп.данными.
  * По сути - описывается эдж внутри эджа.
  */
object MEdgeFlagData
  extends IEsMappingProps
{

  object Fields {
    def FLAG_FN = "f"
  }

  implicit def edgeFlagDataJson: OFormat[MEdgeFlagData] = {
    val F = Fields
    (__ \ F.FLAG_FN).format[MEdgeFlag]
      .inmap(apply, _.flag)
  }

  @inline implicit def univEq: UnivEq[MEdgeFlagData] = UnivEq.derive

  def flag = GenLens[MEdgeFlagData](_.flag)

  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields

    Json.obj(
      F.FLAG_FN -> FKeyWord.indexedJs,
    )
  }

}


/** Контейнер данных одного эдж-флага.
  *
  * @param flag Сам флаг. Единственное обязательное поле.
  */
case class MEdgeFlagData(
                          flag      : MEdgeFlag,
                        )
  extends IId[MEdgeFlag]
{

  override def id = flag

  override def toString = flag.toString

}
